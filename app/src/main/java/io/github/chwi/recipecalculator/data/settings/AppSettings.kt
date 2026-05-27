package io.github.chwi.recipecalculator.data.settings

import io.github.chwi.recipecalculator.core.rational.FractionStyle
import io.github.chwi.recipecalculator.core.units.UnitSystem

/**
 * User-tunable display preferences, persisted via DataStore. Dark mode is intentionally absent —
 * per the design handoff it follows the system setting rather than an in-app toggle.
 */
data class AppSettings(
    val fractionStyle: FractionStyle = FractionStyle.INLINE,
    val unitSystem: UnitSystem = UnitSystem.US,
)
