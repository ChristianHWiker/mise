package io.github.chwi.recipecalculator.core.units

/**
 * Which measurement system quantities are displayed in.
 *
 *  - [US] keeps recipe units as authored (cups, tsp, tbsp).
 *  - [METRIC] converts volume units to grams using each ingredient's density
 *    (`gramsPerCup`), leaving spoon measures and countable items alone.
 *
 * This is a display-time choice, persisted in app settings — the stored quantity is always the
 * exact authored rational, never the converted value.
 */
enum class UnitSystem { US, METRIC }
