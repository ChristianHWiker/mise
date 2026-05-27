package io.github.chwi.recipecalculator.core.rational

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * An exact rational number `num / den`, stored as two integers.
 *
 * This is the load-bearing data model for recipe quantities: storing `3/4` and `1/8`
 * as exact fractions means `3/4 + 1/8` is exactly `7/8`, never `0.8749999…`. Quantities
 * are persisted as the integer columns `num` and `den` (see the Room layer).
 *
 * Instances are always kept in canonical form: reduced by their greatest common divisor,
 * with the sign carried on the numerator and the denominator strictly positive. Construct
 * via [of] (or the [Int].over helpers) rather than the raw constructor to guarantee this.
 *
 * Arithmetic is performed with [Long] intermediates and reduced back to [Int], so ordinary
 * recipe-scale values never overflow.
 */
@ConsistentCopyVisibility
data class Rational private constructor(val num: Int, val den: Int) : Comparable<Rational> {

    operator fun plus(other: Rational): Rational =
        of(num.toLong() * other.den + other.num.toLong() * den, den.toLong() * other.den)

    operator fun minus(other: Rational): Rational =
        of(num.toLong() * other.den - other.num.toLong() * den, den.toLong() * other.den)

    operator fun times(other: Rational): Rational =
        of(num.toLong() * other.num, den.toLong() * other.den)

    operator fun div(other: Rational): Rational {
        require(other.num != 0) { "Division by zero" }
        return of(num.toLong() * other.den, den.toLong() * other.num)
    }

    operator fun times(factor: Int): Rational = of(num.toLong() * factor, den.toLong())

    /**
     * Scale by an arbitrary real [factor] (e.g. a recipe scaling factor of `1.5` or `0.6667`).
     * The factor is captured at [SCALE_PRECISION] resolution and the result reduced, mirroring
     * the reference `scaleQty` in the design handoff.
     */
    fun scale(factor: Double): Rational {
        val scaledNum = (num.toLong() * factor * SCALE_PRECISION).roundToLong()
        return of(scaledNum, den.toLong() * SCALE_PRECISION)
    }

    fun toDouble(): Double = num.toDouble() / den.toDouble()

    val isWhole: Boolean get() = num % den == 0

    /** Round to a whole count `>= 1` — used for countable ingredients like eggs. */
    fun roundToCount(): Rational = of(maxOf(1L, toDouble().roundToLong()), 1L)

    override fun compareTo(other: Rational): Int =
        (num.toLong() * other.den).compareTo(other.num.toLong() * den)

    /** Canonical machine-readable form, e.g. `"9/4"`. For UI use [format]. */
    override fun toString(): String = "$num/$den"

    companion object {
        /** Denominator resolution used when capturing a floating-point scale factor. */
        const val SCALE_PRECISION = 1_000_000L

        val ZERO = of(0, 1)
        val ONE = of(1, 1)

        /** Build a reduced, sign-normalized [Rational] from a numerator and denominator. */
        fun of(num: Long, den: Long): Rational {
            require(den != 0L) { "Denominator must be non-zero" }
            val sign = if (den < 0) -1 else 1
            var n = num * sign
            var d = abs(den)
            val g = gcd(abs(n), d)
            n /= g
            d /= g
            require(n in Int.MIN_VALUE..Int.MAX_VALUE && d in 1..Int.MAX_VALUE.toLong()) {
                "Rational $num/$den does not fit in Int after reduction"
            }
            return Rational(n.toInt(), d.toInt())
        }

        fun of(num: Int, den: Int): Rational = of(num.toLong(), den.toLong())

        fun of(whole: Int): Rational = of(whole.toLong(), 1L)

        private fun gcd(a: Long, b: Long): Long {
            var x = a
            var y = b
            while (y != 0L) {
                val t = y
                y = x % y
                x = t
            }
            return if (x == 0L) 1L else x
        }

        private val MIXED = Regex("""^(-?\d+)\s+(\d+)\s*/\s*(\d+)$""")
        private val FRACTION = Regex("""^(-?\d+)\s*/\s*(\d+)$""")

        /**
         * Parse a user-entered quantity. Accepts (with optional surrounding whitespace and
         * spaces around the slash):
         *  - whole numbers: `"2"`
         *  - simple fractions: `"3/4"`
         *  - mixed numbers: `"1 1/2"`
         *  - decimals: `"0.75"`, `"1.5"`
         *
         * Returns `null` if the input cannot be parsed.
         */
        fun parseOrNull(input: String): Rational? {
            val s = input.trim()
            if (s.isEmpty()) return null

            MIXED.matchEntire(s)?.let { m ->
                val (whole, num, den) = m.destructured
                if (den.toInt() == 0) return null
                val sign = if (whole.startsWith("-")) -1 else 1
                return of(whole.toInt()) + of(sign * num.toInt(), den.toInt())
            }
            FRACTION.matchEntire(s)?.let { m ->
                val (num, den) = m.destructured
                if (den.toInt() == 0) return null
                return of(num.toInt(), den.toInt())
            }
            if ('.' in s) return s.toDoubleOrNull()?.let(::fromDouble)
            return s.toIntOrNull()?.let { of(it) }
        }

        /** Approximate a decimal as a rational at [SCALE_PRECISION] resolution, then reduce. */
        fun fromDouble(value: Double): Rational =
            of((value * SCALE_PRECISION).roundToLong(), SCALE_PRECISION)
    }
}

/** How a [Rational] is rendered for display. */
enum class FractionStyle { INLINE, STACKED, DECIMAL }

/** Cooking fractions a remainder is snapped to for display. */
private val COOKING_GRID = listOf(
    1 to 8, 1 to 4, 1 to 3, 3 to 8, 1 to 2, 5 to 8, 2 to 3, 3 to 4, 7 to 8,
)

private val UNICODE_FRACTIONS = mapOf(
    "1/2" to "½", "1/4" to "¼", "3/4" to "¾", "1/8" to "⅛", "3/8" to "⅜",
    "5/8" to "⅝", "7/8" to "⅞", "1/3" to "⅓", "2/3" to "⅔",
)

/** Below this remainder we treat the value as a whole number; likewise within this of the next. */
private const val SNAP_THRESHOLD = 1.0 / 32.0

/**
 * Render this quantity for display, snapping fractional remainders to the common cooking grid
 * ({1/8, 1/4, 1/3, 3/8, 1/2, 5/8, 2/3, 3/4, 7/8}). Mirrors `formatFraction` from the handoff.
 *
 *  - [FractionStyle.INLINE]  → `"1 1/2"`, `"3/4"`, `"2"`
 *  - [FractionStyle.STACKED] → `"1½"`, `"¾"`, `"2"`
 *  - [FractionStyle.DECIMAL] → `"1.5"`, `"0.75"`, `"2"`
 */
fun Rational.format(style: FractionStyle): String {
    val value = toDouble()
    if (style == FractionStyle.DECIMAL) {
        val rounded = (value * 100).roundToLong() / 100.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
    }

    val negative = value < 0
    val magnitude = abs(value)
    val whole = floor(magnitude).toLong()
    val remainder = magnitude - whole

    val text = when {
        remainder < SNAP_THRESHOLD -> whole.toString()
        remainder > 1 - SNAP_THRESHOLD -> (whole + 1).toString()
        else -> {
            val (n, d) = COOKING_GRID.minBy { (n, d) -> abs(remainder - n.toDouble() / d) }
            when (style) {
                FractionStyle.STACKED -> {
                    val glyph = UNICODE_FRACTIONS["$n/$d"] ?: "$n/$d"
                    if (whole > 0) "$whole$glyph" else glyph
                }
                else -> { // INLINE
                    val inline = "$n/$d"
                    if (whole > 0) "$whole $inline" else inline
                }
            }
        }
    }
    return if (negative && text != "0") "-$text" else text
}
