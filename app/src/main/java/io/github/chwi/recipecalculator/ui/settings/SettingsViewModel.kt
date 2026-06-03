package io.github.chwi.recipecalculator.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chwi.recipecalculator.core.rational.FractionStyle
import io.github.chwi.recipecalculator.core.security.AppLockController
import io.github.chwi.recipecalculator.core.theme.AccentTheme
import io.github.chwi.recipecalculator.core.theme.ThemeMode
import io.github.chwi.recipecalculator.core.units.UnitSystem
import io.github.chwi.recipecalculator.data.settings.AppSettings
import io.github.chwi.recipecalculator.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Exposes the persisted [AppSettings] and writes changes back through the repository. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val appLock: AppLockController,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings(),
    )

    /** Whether this build + device supports the biometric lock; false in the play flavor. */
    val biometricLockAvailable: Boolean = appLock.isAvailable()

    /** Toggle state, sourced from secure storage. Always false in play. */
    val biometricLockEnabled: StateFlow<Boolean> = appLock.isLockEnabled

    fun setFractionStyle(style: FractionStyle) {
        viewModelScope.launch { repository.setFractionStyle(style) }
    }

    fun setUnitSystem(system: UnitSystem) {
        viewModelScope.launch { repository.setUnitSystem(system) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setAccent(accent: AccentTheme) {
        viewModelScope.launch { repository.setAccent(accent) }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch { appLock.setLockEnabled(enabled) }
    }
}
