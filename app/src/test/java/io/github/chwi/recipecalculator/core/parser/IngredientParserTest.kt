package io.github.chwi.recipecalculator.core.parser

import io.github.chwi.recipecalculator.core.rational.Rational
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientParserTest {

    @Test
    fun parses_simple_cup_with_modifier() {
        val r = parseIngredientLine("1 1/2 cups all-purpose flour, sifted")
        assertEquals(Rational.of(3, 2), r.qty)
        assertEquals("cup", r.unit)
        assertEquals("all-purpose flour", r.name)
        assertEquals("sifted", r.modifier)
        assertEquals(1.0f, r.confidence)
    }

    @Test
    fun parses_simple_fraction_no_whole() {
        val r = parseIngredientLine("3/4 cup sugar")
        assertEquals(Rational.of(3, 4), r.qty)
        assertEquals("cup", r.unit)
        assertEquals("sugar", r.name)
        assertNull(r.modifier)
    }

    @Test
    fun parses_unicode_vulgar_fraction() {
        val r = parseIngredientLine("1½ cups water")
        assertEquals(Rational.of(3, 2), r.qty)
        assertEquals("cup", r.unit)
        assertEquals("water", r.name)
    }

    @Test
    fun parses_decimal_quantity() {
        val r = parseIngredientLine("1.5 tsp salt")
        assertNotNull(r.qty)
        assertEquals(1.5, r.qty!!.toDouble(), 0.0001)
        assertEquals("tsp", r.unit)
        assertEquals("salt", r.name)
    }

    @Test
    fun parses_whole_number_grams() {
        val r = parseIngredientLine("250 g butter")
        assertEquals(Rational.of(250, 1), r.qty)
        assertEquals("g", r.unit)
        assertEquals("butter", r.name)
    }

    @Test
    fun strips_leading_bullet_and_em_dash() {
        val r = parseIngredientLine("• 2 eggs")
        assertEquals(Rational.of(2, 1), r.qty)
        // "eggs" has no unit alias → unit is null, but qty + name are present.
        assertEquals("eggs", r.name)
    }

    @Test
    fun strips_leading_enumeration() {
        val r = parseIngredientLine("1. 2 cups flour")
        assertEquals(Rational.of(2, 1), r.qty)
        assertEquals("cup", r.unit)
        assertEquals("flour", r.name)
    }

    @Test
    fun no_unit_no_modifier_eggs() {
        val r = parseIngredientLine("3 eggs")
        assertEquals(Rational.of(3, 1), r.qty)
        assertNull(r.unit)
        assertEquals("eggs", r.name)
        // qty + name, no unit → 0.7 confidence (unit absent).
        assertEquals(0.7f, r.confidence)
    }

    @Test
    fun salt_to_taste_no_quantity() {
        val r = parseIngredientLine("Salt to taste")
        assertNull(r.qty)
        assertNull(r.unit)
        assertEquals("Salt to taste", r.name)
        assertEquals(0.5f, r.confidence)
    }

    @Test
    fun trailing_parenthetical_becomes_modifier() {
        val r = parseIngredientLine("1 cup brown sugar (packed)")
        assertEquals(Rational.of(1, 1), r.qty)
        assertEquals("cup", r.unit)
        assertEquals("brown sugar", r.name)
        assertEquals("packed", r.modifier)
    }

    @Test
    fun parenthetical_combines_with_comma_modifier() {
        val r = parseIngredientLine("1 cup walnuts, chopped (lightly toasted)")
        assertEquals("walnuts", r.name)
        assertNotNull(r.modifier)
        assertTrue(r.modifier!!.contains("chopped"))
        assertTrue(r.modifier!!.contains("lightly toasted"))
    }

    @Test
    fun em_dash_separates_name_from_modifier() {
        val r = parseIngredientLine("200 g butter — softened")
        assertEquals(Rational.of(200, 1), r.qty)
        assertEquals("g", r.unit)
        assertEquals("butter", r.name)
        assertEquals("softened", r.modifier)
    }

    @Test
    fun tablespoon_aliases_collapse_to_tbsp() {
        listOf("1 tbsp", "1 tbs", "1 tablespoon", "1 Tablespoons").forEach { prefix ->
            val r = parseIngredientLine("$prefix olive oil")
            assertEquals("for input '$prefix'", "tbsp", r.unit)
            assertEquals("olive oil", r.name)
        }
    }

    @Test
    fun unrecognized_unit_falls_back_to_no_unit() {
        // "stalks" isn't in the vocabulary → unit null, but qty + name still parse cleanly.
        val r = parseIngredientLine("2 stalks celery, diced")
        assertEquals(Rational.of(2, 1), r.qty)
        assertNull(r.unit)
        // The unrecognized token gets folded into the name — better than dropping it.
        assertTrue("name was '${r.name}'", r.name.contains("celery"))
        assertEquals("diced", r.modifier)
    }

    @Test
    fun lb_collapses_to_kg_for_editor_compatibility() {
        // Editor's UNIT_CHOICES has no lb/oz; we collapse to the nearest supported unit so the
        // dropdown isn't broken on save. The user can fix on the confirmation screen.
        val r = parseIngredientLine("1 lb ground beef")
        assertEquals("kg", r.unit)
        assertEquals("ground beef", r.name)
    }

    @Test
    fun completely_unrecognizable_line_keeps_raw_text() {
        val r = parseIngredientLine("xxxxx")
        assertNull(r.qty)
        assertNull(r.unit)
        assertEquals("xxxxx", r.name)
        assertEquals("xxxxx", r.rawText)
        assertTrue(r.confidence <= 0.5f)
    }

    @Test
    fun parseIngredientBlock_skips_blank_lines() {
        val rows = parseIngredientBlock(
            """
            1 cup flour

            2 eggs

            """.trimIndent()
        )
        assertEquals(2, rows.size)
        assertEquals("flour", rows[0].name)
        assertEquals("eggs", rows[1].name)
    }

    @Test
    fun raw_text_is_preserved_for_review() {
        val raw = "  •  1 1/2 cups all-purpose flour, sifted  "
        val r = parseIngredientLine(raw)
        assertEquals(raw.trim(), r.rawText)
    }

    // ── refineForIngredients ──────────────────────────────────────────────────

    @Test
    fun refine_drops_noise_keeps_qty_bearing_rows() {
        // Snapshot of the user's first real OCR run — a web recipe page.
        val ocr = listOf(
            "roberto.com/real -carbonara-recipe/?",
            "RECIPESRESTAURANTS",
            "Equipment",
            "• pan",
            "Ingredients",
            "•",
            "• 150 gr guanciale",
            "LOCAL SPECIALTIES",
            "• 400 gr pasta Best choice: spaghetti. I use spaghetti alla chitarra in this",
            "recipe.",
            "Instructions",
            "INGREOIENTS",
            "6 egg yolks Depending on egg size, about 1.5 each person.",
            "warm.",
            "q.s. black pepper according to taste, on top",
            "• 120 gr grated pecorino cheese pecorino from Rome is the best choice, the",
            "1 pinch salt in the water to cook the pasta",
            "ABOUT",
            "CONTACT ME",
            "PRIVACY",
            "SUBSCRIBE",
        )
        val refined = refineForIngredients(parseIngredientBlock(ocr.joinToString("\n")))
        // Expect the 5 numeric ingredients + the q.s. row = 6 rows total.
        // (The "4.91 from 11 votes" rating false positive isn't present in this snapshot.)
        assertEquals(
            "expected 6 refined rows, got: ${refined.map { it.name }}",
            6, refined.size,
        )
    }

    @Test
    fun refine_keeps_q_s_to_taste_rows() {
        val rows = parseIngredientBlock("q.s. black pepper according to taste, on top")
        val refined = refineForIngredients(rows)
        assertEquals(1, refined.size)
        assertTrue(refined[0].name.contains("black pepper"))
    }

    @Test
    fun refine_truncates_name_at_capital_after_lowercase() {
        // "egg yolks Depending on egg size" → name should stop at "egg yolks".
        val rows = parseIngredientBlock("6 egg yolks Depending on egg size, about 1.5 each person.")
        val refined = refineForIngredients(rows).single()
        assertEquals("egg yolks", refined.name)
        assertNotNull(refined.modifier)
        assertTrue(refined.modifier!!.contains("Depending"))
    }

    @Test
    fun refine_truncates_name_at_clause_stop_word() {
        // "pinch salt in the water…" → "pinch salt" (stop word "in").
        val rows = parseIngredientBlock("1 pinch salt in the water to cook the pasta")
        val refined = refineForIngredients(rows).single()
        assertEquals("pinch salt", refined.name)
        assertTrue(refined.modifier!!.contains("water"))
    }

    @Test
    fun refine_truncates_long_descriptive_tail_on_pasta() {
        val rows = parseIngredientBlock(
            "• 400 gr pasta Best choice: spaghetti. I use spaghetti alla chitarra in this",
        )
        val refined = refineForIngredients(rows).single()
        assertEquals("pasta", refined.name)
        assertNotNull(refined.modifier)
    }

    @Test
    fun refine_leaves_short_multi_word_names_alone() {
        val rows = parseIngredientBlock("1 1/2 cups all-purpose flour, sifted")
        val refined = refineForIngredients(rows).single()
        assertEquals("all-purpose flour", refined.name)
        assertEquals("sifted", refined.modifier)
    }

    @Test
    fun refine_drops_empty_and_pure_noise() {
        val rows = parseIngredientBlock(
            """
            •
            PRIVACY
            SUBSCRIBE
            ywwruunyy cncryu wy
            """.trimIndent()
        )
        val refined = refineForIngredients(rows)
        assertEquals(0, refined.size)
    }
}
