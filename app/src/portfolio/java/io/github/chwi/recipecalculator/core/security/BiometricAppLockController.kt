package io.github.chwi.recipecalculator.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Portfolio-flavor [AppLockController] backed by `androidx.biometric.BiometricPrompt`.
 *
 * - [isAvailable] checks `BiometricManager.canAuthenticate` for `BIOMETRIC_STRONG | DEVICE_CREDENTIAL`.
 *   `DEVICE_CREDENTIAL` lets users without enrolled biometrics fall back to PIN/pattern/password,
 *   so the toggle still works on phones without a fingerprint sensor as long as a screen lock is set.
 * - [isLockEnabled] is sourced from [SecurePreferenceStore] (Keystore-encrypted prefs in this flavor)
 *   and primed lazily on first read from disk; subsequent reads come from the in-memory StateFlow.
 * - [requestUnlock] suspends until the prompt resolves; cancellations and errors become
 *   [UnlockResult.Cancelled] / [UnlockResult.Error] respectively.
 */
@Singleton
class BiometricAppLockController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferences: SecurePreferenceStore,
) : AppLockController {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isLockEnabled = MutableStateFlow(false)
    override val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    init {
        // Hydrate the persisted flag exactly once on construction.
        scope.launch { _isLockEnabled.value = securePreferences.getBoolean(KEY_LOCK_ENABLED, false) }
    }

    override fun isAvailable(): Boolean {
        val status = BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS)
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }

    override suspend fun setLockEnabled(enabled: Boolean) {
        securePreferences.putBoolean(KEY_LOCK_ENABLED, enabled)
        _isLockEnabled.value = enabled
    }

    override suspend fun requestUnlock(activity: FragmentActivity): UnlockResult =
        suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (cont.isActive) cont.resume(UnlockResult.Success)
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (!cont.isActive) return
                        cont.resume(
                            when (errorCode) {
                                BiometricPrompt.ERROR_USER_CANCELED,
                                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                                BiometricPrompt.ERROR_CANCELED -> UnlockResult.Cancelled
                                else -> UnlockResult.Error(errString.toString())
                            },
                        )
                    }
                    // onAuthenticationFailed (single attempt rejected) doesn't terminate the
                    // prompt — the system keeps it up for another try, so no resume here.
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Recipe Calculator")
                .setSubtitle("Use your fingerprint, face, or device PIN")
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build()
            prompt.authenticate(info)
        }

    private companion object {
        const val KEY_LOCK_ENABLED = "app_lock_enabled"
        const val ALLOWED_AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
    }
}
