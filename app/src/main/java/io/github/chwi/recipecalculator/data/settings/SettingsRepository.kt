package io.github.chwi.recipecalculator.data.settings

import io.github.chwi.recipecalculator.core.rational.FractionStyle
import io.github.chwi.recipecalculator.core.theme.AccentTheme
import io.github.chwi.recipecalculator.core.theme.ThemeMode
import io.github.chwi.recipecalculator.core.units.UnitSystem
import kotlinx.coroutines.flow.Flow

/** Read/write access to the user's display preferences. Backed by DataStore. */
interface SettingsRepository {

    /** Emits the current settings and re-emits on every change. */
    val settings: Flow<AppSettings>

    suspend fun setFractionStyle(style: FractionStyle)

    suspend fun setUnitSystem(system: UnitSystem)

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setAccent(accent: AccentTheme)
}
