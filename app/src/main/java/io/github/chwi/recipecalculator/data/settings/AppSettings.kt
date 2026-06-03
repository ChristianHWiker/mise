package io.github.chwi.recipecalculator.data.settings

import io.github.chwi.recipecalculator.core.rational.FractionStyle
import io.github.chwi.recipecalculator.core.theme.AccentTheme
import io.github.chwi.recipecalculator.core.theme.ThemeMode
import io.github.chwi.recipecalculator.core.units.UnitSystem

/**
 * User-tunable preferences, persisted via DataStore. [themeMode] resolves light/dark (SYSTEM keeps
 * the original behaviour of following the OS); [accent] selects the brand palette.
 */
data class AppSettings(
    val fractionStyle: FractionStyle = FractionStyle.INLINE,
    val unitSystem: UnitSystem = UnitSystem.US,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentTheme = AccentTheme.SAGE,
)
