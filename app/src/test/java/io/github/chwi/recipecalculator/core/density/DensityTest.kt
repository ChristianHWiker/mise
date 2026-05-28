package io.github.chwi.recipecalculator.core.density

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DensityTest {

    @Test fun `lookup is case insensitive`() {
        assertEquals(120, lookupGramsPerCup("Flour"))
        assertEquals(120, lookupGramsPerCup("FLOUR"))
        assertEquals(120, lookupGramsPerCup("flour"))
    }

    @Test fun `lookup trims surrounding whitespace`() {
        assertEquals(120, lookupGramsPerCup("  flour  "))
    }

    @Test fun `lookup collapses internal whitespace`() {
        assertEquals(120, lookupGramsPerCup("all-purpose  flour"))
    }

    @Test fun `lookup handles basic plurals`() {
        // KNOWN_DENSITIES stores "raisin", but recipes usually say "raisins".
        assertEquals(146, lookupGramsPerCup("raisins"))
        assertEquals(146, lookupGramsPerCup("raisin"))
        assertEquals(117, lookupGramsPerCup("walnuts"))
    }

    @Test fun `unknown ingredient returns null`() {
        assertNull(lookupGramsPerCup("unicorn dust"))
        assertNull(lookupGramsPerCup(""))
    }

    @Test fun `every density in the table is positive`() {
        assertTrue(KNOWN_DENSITIES.isNotEmpty())
        KNOWN_DENSITIES.forEach { (name, grams) ->
            assertTrue("$name = $grams must be > 0", grams > 0)
        }
    }

    @Test fun `every key is already normalized`() {
        KNOWN_DENSITIES.keys.forEach { key ->
            assertEquals("key '$key' is not normalized", key, normalize(key))
        }
    }

    @Test fun `common baking ingredients are covered`() {
        // Sanity check: the seed recipe and core baking flow rely on these.
        listOf("flour", "butter", "sugar", "brown sugar", "chocolate chips").forEach {
            assertNotNull("missing density for '$it'", lookupGramsPerCup(it))
        }
    }
}
