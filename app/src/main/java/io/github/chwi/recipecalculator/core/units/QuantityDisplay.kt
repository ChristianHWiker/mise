package io.github.chwi.recipecalculator.core.units

import io.github.chwi.recipecalculator.core.rational.FractionStyle
import io.github.chwi.recipecalculator.core.rational.Rational
import io.github.chwi.recipecalculator.core.rational.format
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Display-time quantity logic: scale a stored rational by a factor, convert it for the chosen
 * [UnitSystem], and render it as text. This is the Kotlin port of the reference `displayQty` /
 * `convert` / `factorFromDisplayValue` helpers from the design handoff's `data.js`, and it drives
 * every ingredient row plus the reverse-scale sheet.
 *
 * The stored quantity is never mutated — scaling and conversion happen here, on the way to the UI.
 */

/** An ingredient quantity converted into a concrete unit, still exact. */
data class ConvertedQuantity(val qty: Rational, val unit: String)

/** A fully display-ready quantity: the rendered text plus the numeric value (for reverse scaling). */
data class DisplayedQuantity(
    val qtyText: String,
    val unitText: String,
    val unit: String,
    val qtyValue: Double,
)

/**
 * Convert [qty] (in [unit]) for the target [system].
 *
 *  - Countable items (`ea`) round to a whole count ≥ 1, regardless of system.
 *  - [UnitSystem.US] passes everything through unchanged.
 *  - [UnitSystem.METRIC] converts `cup`/`tbsp` to grams via [gramsPerCup] (grams ≥ 50 snap to 5 g);
 *    spoon measures without a density (spices, extracts) stay as-is — they read fine in metric kitchens.
 */
fun convert(qty: Rational, unit: String, gramsPerCup: Int?, system: UnitSystem): ConvertedQuantity {
    if (unit == "ea") return ConvertedQuantity(qty.roundToCount(), "ea")
    if (system == UnitSystem.US) return ConvertedQuantity(qty, unit)

    if (unit == "cup" && gramsPerCup != null) {
        val grams = qty.toDouble() * gramsPerCup
        val step = if (grams >= 50) 5 else 1
        val snapped = max(1L, (grams / step).roundToLong() * step)
        return ConvertedQuantity(Rational.of(snapped, 1L), "g")
    }
    if (unit == "tbsp" && gramsPerCup != null) {
        val grams = qty.toDouble() * gramsPerCup / 16.0
        return ConvertedQuantity(Rational.of(max(1L, grams.roundToLong()), 1L), "g")
    }
    return ConvertedQuantity(qty, unit)
}

/** The human label for a [unit], pluralized against [qty] where it matters ("cup" → "cups"). */
fun unitLabel(unit: String, qty: Rational): String {
    val plural = qty.toDouble() != 1.0
    return when (unit) {
        "cup" -> if (plural) "cups" else "cup"
        "tsp" -> "tsp"
        "tbsp" -> "tbsp"
        "g" -> "g"
        "ml" -> "ml"
        "kg" -> "kg"
        "ea" -> ""
        else -> unit
    }
}

/**
 * Scale [baseQty] by [factor], convert for [system], and render with [fractionStyle].
 * Gram/millilitre/count results are always shown as decimals (fractions of a gram are nonsense).
 */
fun displayQty(
    baseQty: Rational,
    unit: String,
    gramsPerCup: Int?,
    factor: Double,
    fractionStyle: FractionStyle,
    system: UnitSystem,
): DisplayedQuantity {
    val scaled = baseQty.scale(factor)
    val conv = convert(scaled, unit, gramsPerCup, system)
    val style = if (conv.unit in DECIMAL_UNITS) FractionStyle.DECIMAL else fractionStyle
    return DisplayedQuantity(
        qtyText = conv.qty.format(style),
        unitText = unitLabel(conv.unit, conv.qty),
        unit = conv.unit,
        qtyValue = conv.qty.toDouble(),
    )
}

/**
 * Reverse scaling: the user says "I actually have [newDisplayValue] of this ingredient" — return the
 * recipe factor that makes the ingredient's displayed amount match, scaling everything else with it.
 */
fun factorFromDisplayValue(
    baseQty: Rational,
    unit: String,
    gramsPerCup: Int?,
    currentFactor: Double,
    fractionStyle: FractionStyle,
    system: UnitSystem,
    newDisplayValue: Double,
): Double {
    val current = displayQty(baseQty, unit, gramsPerCup, currentFactor, fractionStyle, system)
    if (current.qtyValue <= 0.0) return currentFactor
    return currentFactor * (newDisplayValue / current.qtyValue)
}

/** The increment for the reverse-scale stepper, given the displayed [unit]. */
fun stepFor(unit: String): Double = when (unit) {
    "g", "ml" -> 5.0
    "ea" -> 1.0
    else -> 0.25
}

/** Render a free-typed stepper [value] in [unit] the same way an ingredient row would. */
fun formatStepperValue(value: Double, unit: String, fractionStyle: FractionStyle): String =
    if (unit in DECIMAL_UNITS) {
        value.roundToLong().toString()
    } else {
        Rational.of((value * 1000).roundToLong(), 1000L).format(fractionStyle)
    }

private val DECIMAL_UNITS = setOf("g", "ml", "ea")
