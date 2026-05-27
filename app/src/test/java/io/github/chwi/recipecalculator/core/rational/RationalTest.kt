package io.github.chwi.recipecalculator.core.rational

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RationalTest {

    // ── Construction & normalization ────────────────────────────────
    @Test
    fun `reduces to lowest terms`() {
        assertEquals(Rational.of(3, 4), Rational.of(6, 8))
        assertEquals(Rational.of(1, 2), Rational.of(50, 100))
        assertEquals(Rational.of(2, 1), Rational.of(4, 2))
    }

    @Test
    fun `normalizes sign onto numerator`() {
        val r = Rational.of(1, -2)
        assertEquals(-1, r.num)
        assertEquals(2, r.den)
        assertEquals(Rational.of(-1, 2), Rational.of(1, -2))
    }

    @Test
    fun `zero is canonical regardless of denominator`() {
        assertEquals(Rational.ZERO, Rational.of(0, 5))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero denominator is rejected`() {
        Rational.of(1, 0)
    }

    // ── Arithmetic ──────────────────────────────────────────────────
    @Test
    fun `addition is exact across denominators`() {
        // The headline case: 3/4 + 1/8 = 7/8, no floating-point drift.
        assertEquals(Rational.of(7, 8), Rational.of(3, 4) + Rational.of(1, 8))
    }

    @Test
    fun `subtraction multiplication division`() {
        assertEquals(Rational.of(1, 4), Rational.of(1, 2) - Rational.of(1, 4))
        assertEquals(Rational.of(3, 8), Rational.of(3, 4) * Rational.of(1, 2))
        assertEquals(Rational.of(2, 1), Rational.of(1, 1) / Rational.of(1, 2))
    }

    @Test
    fun `multiply by integer`() {
        assertEquals(Rational.of(9, 4), Rational.of(3, 4) * 3)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `dividing by zero rational is rejected`() {
        Rational.of(1, 2) / Rational.ZERO
    }

    // ── Scaling ─────────────────────────────────────────────────────
    @Test
    fun `scale by whole factor`() {
        assertEquals(Rational.of(9, 2), Rational.of(9, 4).scale(2.0))
    }

    @Test
    fun `scale by fractional factor stays exact enough to display`() {
        // 9/4 cup flour scaled to 1.5x = 27/8 = 3.375
        assertEquals(3.375, Rational.of(9, 4).scale(1.5).toDouble(), 1e-9)
    }

    // ── Comparison ──────────────────────────────────────────────────
    @Test
    fun `comparable orders by value`() {
        assertTrue(Rational.of(1, 3) < Rational.of(1, 2))
        assertTrue(Rational.of(3, 4) > Rational.of(2, 3))
        assertEquals(0, Rational.of(2, 4).compareTo(Rational.of(1, 2)))
        val sorted = listOf(Rational.of(3, 4), Rational.of(1, 8), Rational.of(1, 2)).sorted()
        assertEquals(listOf(Rational.of(1, 8), Rational.of(1, 2), Rational.of(3, 4)), sorted)
    }

    // ── Whole / count helpers ───────────────────────────────────────
    @Test
    fun `isWhole detects integers`() {
        assertTrue(Rational.of(4, 2).isWhole)
        assertFalse(Rational.of(3, 4).isWhole)
    }

    @Test
    fun `roundToCount rounds and floors at one`() {
        assertEquals(Rational.of(2), Rational.of(7, 4).roundToCount())   // 1.75 -> 2
        assertEquals(Rational.of(1), Rational.of(1, 8).roundToCount())   // 0.125 -> 1 (min)
        assertEquals(Rational.of(3), Rational.of(5, 2).roundToCount())   // 2.5 -> 3
    }

    // ── Parsing ─────────────────────────────────────────────────────
    @Test
    fun `parse whole number`() {
        assertEquals(Rational.of(2), Rational.parseOrNull("2"))
    }

    @Test
    fun `parse simple fraction`() {
        assertEquals(Rational.of(3, 4), Rational.parseOrNull("3/4"))
        assertEquals(Rational.of(3, 4), Rational.parseOrNull(" 6 / 8 "))
    }

    @Test
    fun `parse mixed number`() {
        assertEquals(Rational.of(3, 2), Rational.parseOrNull("1 1/2"))
        assertEquals(Rational.of(9, 4), Rational.parseOrNull("2 1/4"))
    }

    @Test
    fun `parse decimal`() {
        assertEquals(0.75, Rational.parseOrNull("0.75")!!.toDouble(), 1e-9)
        assertEquals(Rational.of(3, 2), Rational.parseOrNull("1.5"))
    }

    @Test
    fun `parse rejects garbage`() {
        assertNull(Rational.parseOrNull(""))
        assertNull(Rational.parseOrNull("abc"))
        assertNull(Rational.parseOrNull("1/0"))
        assertNull(Rational.parseOrNull("1 2 3"))
    }

    // ── Display formatting ──────────────────────────────────────────
    @Test
    fun `inline format renders mixed numbers`() {
        assertEquals("2", Rational.of(2).format(FractionStyle.INLINE))
        assertEquals("3/4", Rational.of(3, 4).format(FractionStyle.INLINE))
        assertEquals("1 1/2", Rational.of(3, 2).format(FractionStyle.INLINE))
    }

    @Test
    fun `stacked format uses unicode glyphs`() {
        assertEquals("¾", Rational.of(3, 4).format(FractionStyle.STACKED))
        assertEquals("1½", Rational.of(3, 2).format(FractionStyle.STACKED))
    }

    @Test
    fun `decimal format trims whole values`() {
        assertEquals("2", Rational.of(2).format(FractionStyle.DECIMAL))
        assertEquals("0.75", Rational.of(3, 4).format(FractionStyle.DECIMAL))
        assertEquals("1.5", Rational.of(3, 2).format(FractionStyle.DECIMAL))
    }

    @Test
    fun `display snaps near-whole remainders`() {
        // 2.001 (scaled) should display as "2", and 2.99 as "3", per the 1/32 threshold.
        assertEquals("2", Rational.of(2001, 1000).format(FractionStyle.INLINE))
        assertEquals("3", Rational.of(299, 100).format(FractionStyle.INLINE))
    }

    @Test
    fun `display snaps odd remainder to nearest cooking fraction`() {
        // 0.3 is closest to 1/3 on the cooking grid.
        assertEquals("1/3", Rational.of(3, 10).format(FractionStyle.INLINE))
        // 0.37 snaps to 3/8 (closer than 1/3).
        assertEquals("3/8", Rational.of(37, 100).format(FractionStyle.INLINE))
    }
}
