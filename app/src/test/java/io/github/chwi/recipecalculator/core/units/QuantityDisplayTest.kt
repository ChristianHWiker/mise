package io.github.chwi.recipecalculator.core.units

import io.github.chwi.recipecalculator.core.rational.FractionStyle
import io.github.chwi.recipecalculator.core.rational.Rational
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuantityDisplayTest {

    // ── convert: countable ───────────────────────────────────────────────
    @Test
    fun `ea rounds to whole count and ignores system`() {
        val us = convert(Rational.of(2, 1), "ea", null, UnitSystem.US)
        val metric = convert(Rational.of(2, 1), "ea", null, UnitSystem.METRIC)
        assertEquals("ea", us.unit)
        assertEquals(Rational.of(2, 1), us.qty)
        assertEquals(us, metric)
    }

    @Test
    fun `ea never rounds below one`() {
        val conv = convert(Rational.of(1, 4), "ea", null, UnitSystem.US)
        assertEquals(Rational.of(1, 1), conv.qty)
    }

    // ── convert: US passthrough ──────────────────────────────────────────
    @Test
    fun `US keeps cups unchanged`() {
        val conv = convert(Rational.of(9, 4), "cup", 120, UnitSystem.US)
        assertEquals("cup", conv.unit)
        assertEquals(Rational.of(9, 4), conv.qty)
    }

    // ── convert: metric cup → grams ──────────────────────────────────────
    @Test
    fun `metric converts cups to grams using density`() {
        // 9/4 cup flour * 120 g/cup = 270 g, snaps to nearest 5 → 270
        val conv = convert(Rational.of(9, 4), "cup", 120, UnitSystem.METRIC)
        assertEquals("g", conv.unit)
        assertEquals(270.0, conv.qty.toDouble(), 0.0)
    }

    @Test
    fun `metric snaps large gram values to 5g`() {
        // 3/4 cup sugar * 200 = 150 g → already on 5g grid
        val sugar = convert(Rational.of(3, 4), "cup", 200, UnitSystem.METRIC)
        assertEquals(150.0, sugar.qty.toDouble(), 0.0)
        // 1 cup butter * 225 = 225 g
        val butter = convert(Rational.of(1, 1), "cup", 225, UnitSystem.METRIC)
        assertEquals(225.0, butter.qty.toDouble(), 0.0)
    }

    @Test
    fun `metric keeps small gram values on 1g grid`() {
        // 1/8 cup * 100 = 12.5 g, below 50 → 1g step → 13 (allow rounding either way at .5)
        val conv = convert(Rational.of(1, 8), "cup", 100, UnitSystem.METRIC)
        assertEquals("g", conv.unit)
        assertTrue(conv.qty.toDouble() in 12.0..13.0)
    }

    @Test
    fun `metric leaves spoon measures without density alone`() {
        val conv = convert(Rational.of(2, 1), "tsp", null, UnitSystem.METRIC)
        assertEquals("tsp", conv.unit)
        assertEquals(Rational.of(2, 1), conv.qty)
    }

    @Test
    fun `metric converts tbsp to grams`() {
        // 1 tbsp * 170 g/cup / 16 = 10.625 → 11 g
        val conv = convert(Rational.of(1, 1), "tbsp", 170, UnitSystem.METRIC)
        assertEquals("g", conv.unit)
        assertEquals(11.0, conv.qty.toDouble(), 0.0)
    }

    // ── unitLabel ────────────────────────────────────────────────────────
    @Test
    fun `cup pluralizes only when not exactly one`() {
        assertEquals("cup", unitLabel("cup", Rational.of(1, 1)))
        assertEquals("cups", unitLabel("cup", Rational.of(3, 4)))
        assertEquals("cups", unitLabel("cup", Rational.of(9, 4)))
    }

    @Test
    fun `ea has no label`() {
        assertEquals("", unitLabel("ea", Rational.of(2, 1)))
    }

    // ── displayQty ───────────────────────────────────────────────────────
    @Test
    fun `displayQty renders inline fraction in US`() {
        val d = displayQty(Rational.of(9, 4), "cup", 120, 1.0, FractionStyle.INLINE, UnitSystem.US)
        assertEquals("2 1/4", d.qtyText)
        assertEquals("cups", d.unitText)
        assertEquals(2.25, d.qtyValue, 1e-9)
    }

    @Test
    fun `displayQty forces decimal for grams regardless of fraction style`() {
        val d = displayQty(Rational.of(9, 4), "cup", 120, 1.0, FractionStyle.STACKED, UnitSystem.METRIC)
        assertEquals("g", d.unit)
        assertEquals("270", d.qtyText)
    }

    @Test
    fun `displayQty scales before converting`() {
        // double the flour: 9/4 * 2 = 9/2 = 4 1/2 cups
        val d = displayQty(Rational.of(9, 4), "cup", 120, 2.0, FractionStyle.INLINE, UnitSystem.US)
        assertEquals("4 1/2", d.qtyText)
    }

    @Test
    fun `displayQty rounds eggs to whole count when scaled`() {
        // 2 eggs * 0.75 = 1.5 → rounds to 2
        val d = displayQty(Rational.of(2, 1), "ea", null, 0.75, FractionStyle.INLINE, UnitSystem.US)
        assertEquals("ea", d.unit)
        assertEquals("2", d.qtyText)
    }

    // ── factorFromDisplayValue (reverse scaling) ─────────────────────────
    @Test
    fun `reverse scale doubling the displayed value doubles the factor`() {
        // flour currently 2.25 cups at factor 1.0; user says they have 4.5 cups → factor 2.0
        val f = factorFromDisplayValue(
            baseQty = Rational.of(9, 4), unit = "cup", gramsPerCup = 120,
            currentFactor = 1.0, fractionStyle = FractionStyle.INLINE, system = UnitSystem.US,
            newDisplayValue = 4.5,
        )
        assertEquals(2.0, f, 1e-6)
    }

    @Test
    fun `reverse scale is stable when value is unchanged`() {
        val f = factorFromDisplayValue(
            baseQty = Rational.of(3, 4), unit = "cup", gramsPerCup = 200,
            currentFactor = 1.0, fractionStyle = FractionStyle.INLINE, system = UnitSystem.US,
            newDisplayValue = 0.75,
        )
        assertEquals(1.0, f, 1e-6)
    }

    @Test
    fun `reverse scale guards against zero current value`() {
        val f = factorFromDisplayValue(
            baseQty = Rational.ZERO, unit = "cup", gramsPerCup = 120,
            currentFactor = 1.5, fractionStyle = FractionStyle.INLINE, system = UnitSystem.US,
            newDisplayValue = 3.0,
        )
        assertEquals(1.5, f, 0.0)
    }

    // ── stepper helpers ──────────────────────────────────────────────────
    @Test
    fun `step size depends on unit`() {
        assertEquals(5.0, stepFor("g"), 0.0)
        assertEquals(1.0, stepFor("ea"), 0.0)
        assertEquals(0.25, stepFor("cup"), 0.0)
        assertEquals(0.25, stepFor("tsp"), 0.0)
    }

    @Test
    fun `stepper value formats grams as integer and cups as fraction`() {
        assertEquals("270", formatStepperValue(270.0, "g", FractionStyle.INLINE))
        assertEquals("1 1/2", formatStepperValue(1.5, "cup", FractionStyle.INLINE))
    }
}
