package io.github.chwi.recipecalculator.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chwi.recipecalculator.core.theme.AccentTheme
import io.github.chwi.recipecalculator.core.theme.ThemeMode
import io.github.chwi.recipecalculator.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Just the slice of settings the theme needs, so the whole tree doesn't recompose on every pref. */
data class ThemeState(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentTheme = AccentTheme.SAGE,
)

/**
 * Surfaces the theme-affecting preferences to the activity, which applies them at the root of the
 * Compose tree. Resolved at the activity scope (a single instance) so the whole app re-themes the
 * instant the user changes either control in Settings.
 */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    repository: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<ThemeState> = repository.settings
        .map { ThemeState(mode = it.themeMode, accent = it.accent) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeState(),
        )
}
