package io.github.chwi.recipecalculator.core.security

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.StateFlow

/**
 * Flavor seam for the biometric app-lock feature.
 *
 * `main/` only declares the contract — each flavor (`play`, `portfolio`) provides its own impl and
 * Hilt binding under `src/<flavor>/java/.../security/`. UI code injects the interface and never
 * branches on `BuildConfig.FLAVOR`; the [isAvailable] flag is what gates the Settings row and
 * launch-time gate in the play flavor (where it returns false) versus the portfolio flavor (where
 * Phase 4's `BiometricAppLockController` consults `BiometricManager`).
 */
interface AppLockController {
    /** Whether this device + build supports the lock. False in `play` (feature compiled out). */
    fun isAvailable(): Boolean

    /** User's chosen toggle state. Backed by [SecurePreferenceStore] in portfolio, ignored in play. */
    val isLockEnabled: StateFlow<Boolean>

    suspend fun setLockEnabled(enabled: Boolean)

    /** Prompt the user; suspends until the biometric/credential prompt resolves. */
    suspend fun requestUnlock(activity: FragmentActivity): UnlockResult
}

sealed interface UnlockResult {
    data object Success : UnlockResult
    data object Cancelled : UnlockResult
    data class Error(val message: String) : UnlockResult
}
