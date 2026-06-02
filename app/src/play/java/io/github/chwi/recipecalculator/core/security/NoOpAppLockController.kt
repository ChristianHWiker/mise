package io.github.chwi.recipecalculator.core.security

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `play` flavor: biometric lock is compiled out. [isAvailable] returns false so Settings hides
 * the toggle and the launch gate short-circuits; the remaining surface exists only so call
 * sites in `main/` can depend on the interface without a `BuildConfig.FLAVOR` branch.
 */
@Singleton
class NoOpAppLockController @Inject constructor() : AppLockController {

    private val _isLockEnabled = MutableStateFlow(false)
    override val isLockEnabled: StateFlow<Boolean> = _isLockEnabled.asStateFlow()

    override fun isAvailable(): Boolean = false

    override suspend fun setLockEnabled(enabled: Boolean) {
        // No persistence in play — toggle UI is hidden, so this is unreachable in practice.
    }

    override suspend fun requestUnlock(activity: FragmentActivity): UnlockResult = UnlockResult.Success
}
