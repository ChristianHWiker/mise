package io.github.chwi.recipecalculator.core.security

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4 lands here. For the v0.5.0 scaffolding pass this is a stub with the same behavior
 * as the play flavor's no-op — it just proves the flavor seam compiles and Hilt resolves the
 * portfolio binding. Phase 4 will wire `BiometricManager.canAuthenticate` into [isAvailable],
 * pull the toggle state from [SecurePreferenceStore], and drive [requestUnlock] through
 * `androidx.biometric.BiometricPrompt`.
 */
@Singleton
class BiometricAppLockController @Inject constructor() : AppLockController {

    private val _isLockEnabled = MutableStateFlow(false)
    override val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    override fun isAvailable(): Boolean = false // Phase 4: query BiometricManager

    override suspend fun setLockEnabled(enabled: Boolean) {
        // Phase 4: persist via SecurePreferenceStore + update _isLockEnabled
    }

    override suspend fun requestUnlock(activity: FragmentActivity): UnlockResult = UnlockResult.Success
}
