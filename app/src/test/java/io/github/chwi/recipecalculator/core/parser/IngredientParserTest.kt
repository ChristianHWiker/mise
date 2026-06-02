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

    // ── dual metric/imperial + glued quantities ──────────────────────────────

    @Test
    fun parses_glued_dual_metric_imperial_keeps_metric() {
        // "100g/3.5 oz parmigiano reggiano" — number glued to unit, plus an imperial alternative.
        val r = parseIngredientLine("100g/3.5 oz parmigiano reggiano, finely shredded")
        assertEquals(Rational.of(100, 1), r.qty)
        assertEquals("g", r.unit)
        assertEquals("parmigiano reggiano", r.name)
        assertEquals("finely shredded", r.modifier)
        assertEquals(1.0f, r.confidence)
    }

    @Test
    fun parses_glued_dual_with_ocr_mangled_oz() {
        // OCR routinely reads "oz" as "0z"/"0Z"; the imperial half must still be discarded.
        val r = parseIngredientLine("400g/14 0Z spaghetti")
        assertEquals(Rational.of(400, 1), r.qty)
        assertEquals("g", r.unit)
        assertEquals("spaghetti", r.name)
    }

    @Test
    fun parses_dual_with_o_for_zero_in_number() {
        // OCR read "100g" as "10Og" (capital O for the second zero) — must clean to 100.
        val r = parseIngredientLine("10Og/3.5 0z parmigiano reggiano, finely shredded")
        assertEquals(Rational.of(100, 1), r.qty)
        assertEquals("g", r.unit)
        assertEquals("parmigiano reggiano", r.name)
    }

    @Test
    fun parses_number_glued_to_word() {
        // "1garlic clove" — no space between quantity and the ingredient word.
        val r = parseIngredientLine("1garlic clove, finely minced (optional, Note 4)")
        assertEquals(Rational.of(1, 1), r.qty)
        assertNull(r.unit)
        assertEquals("garlic clove", r.name)
        assertTrue(r.modifier!!.contains("finely minced"))
    }

    // ── mergeWrappedLines ─────────────────────────────────────────────────────

    @Test
    fun merges_wrapped_continuation_fragment() {
        val merged = mergeWrappedLines(
            listOf(
                "100g/3.5 0z parmigiano reggiano, finely shredded (or pecorino romano, sub",
                "parmesan, Note 3)",
            ),
        )
        assertEquals(1, merged.size)
        assertTrue(merged.single().endsWith("Note 3)"))
    }

    @Test
    fun does_not_merge_a_new_quantified_item() {
        val merged = mergeWrappedLines(listOf("100g spaghetti", "400g flour"))
        assertEquals(2, merged.size)
    }

    @Test
    fun mid_name_parenthetical_moves_to_modifier_not_truncated() {
        // Regression: "guanciale (pancetta or block bacon)" must not be cut at the "or" inside
        // the parens. The clarification belongs in the modifier; the name stays clean.
        val rows = parseIngredientBlock(
            "175g/6 oz guanciale (pancetta or block bacon), weight after skin removed (Note 1)",
        )
        val r = refineForIngredients(rows).single()
        assertEquals("guanciale", r.name)
        assertEquals(Rational.of(175, 1), r.qty)
        assertEquals("g", r.unit)
        assertTrue("modifier was '${r.modifier}'", r.modifier!!.contains("pancetta or block bacon"))
    }

    // ── numbered instruction steps are not ingredients ────────────────────────

    @Test
    fun refine_drops_numbered_instruction_steps() {
        val rows = parseIngredientBlock(
            """
            1 Guanciale Cut into 0.5cm/1/5 thick slices then into batons.
            3 Cook pasta Bring 4 litres of water to the boil with the salt Add
            """.trimIndent(),
        )
        assertEquals(0, refineForIngredients(rows).size)
    }

    @Test
    fun refine_drops_step_number_glued_to_word() {
        // OCR frequently glues the step number to the lead-in word: "1Guanciale", "6Cook".
        val rows = parseIngredientBlock(
            """
            1Guanciale Cut into 0.5cm thick slices then into batons.
            3Carbonara sauce Place eggs and yolks in a large bowl Whisk to combine
            6Cook guanciale While the pasta is cooking place guanciale in a pan
            """.trimIndent(),
        )
        assertEquals(0, refineForIngredients(rows).size)
    }

    @Test
    fun refine_keeps_short_capitalized_ingredient() {
        // Guard against over-eager step filtering: a short, capitalized ingredient survives.
        val rows = parseIngredientBlock("2 Bay leaves")
        val refined = refineForIngredients(rows).single()
        assertTrue(refined.name.contains("Bay"))
    }

    @Test
    fun carbonara_recovers_all_real_ingredients() {
        // Verbatim ML Kit dump (block lines flattened) from a real Pixel capture of
        // recipetineats.com/carbonara — the four "Ng/X oz" ingredients used to vanish.
        val ocr = listOf(
            "2 large eggs (Note 2)",
            "2 egg yolks (Note 2)",
            "175g/6 oz guanciale (pancetta or block bacon), weight after skin removed",
            "(Note 1)",
            "025 tsp black pepper",
            "100g/3.5 0z parmigiano reggiano, finely shredded (or pecorino romano, sub",
            "parmesan, Note 3)",
            "400g/14 0Z spaghetti",
            "1 tbsp cooking/kosher salt (for cooking pasta)",
            "125 ml pasta cooking water",
            "1garlic clove, finely minced (optional, Note 4)",
        )
        val refined = refineForIngredients(parseIngredientBlock(ocr.joinToString("\n")))
        val names = refined.joinToString(" | ") { it.name.lowercase() }
        assertTrue("guanciale missing in: $names", names.contains("guanciale"))
        assertTrue("parmigiano missing in: $names", names.contains("parmigiano"))
        assertTrue("spaghetti missing in: $names", names.contains("spaghetti"))
        assertTrue("garlic missing in: $names", names.contains("garlic"))
    }

    // ── OCR'd checkbox glyph + dropped fraction numerator (recipetineats carbonara) ──────────

    @Test
    fun strips_leading_ocr_checkbox_letter_before_qty() {
        // ML Kit reads the print ☐ checkbox as a stray "D " prefix.
        val r = parseIngredientLine("D 2 egg yolks (Note 2)")
        assertEquals(Rational.of(2, 1), r.qty)
        assertEquals("egg yolks", r.name)
        assertEquals("Note 2", r.modifier)
    }

    @Test
    fun strips_leading_ocr_checkbox_before_qty_with_unit() {
        val r = parseIngredientLine("D 1 tbsp cooking/kosher salt (for cooking pasta)")
        assertEquals(Rational.of(1, 1), r.qty)
        assertEquals("tbsp", r.unit)
        assertTrue("name was '${r.name}'", r.name.contains("salt"))
    }

    @Test
    fun does_not_strip_leading_capital_when_no_qty_follows() {
        // Guard against LEADING_OCR_CHECKBOX over-firing: a capital-first phrase with no digit
        // immediately after must keep its first word.
        assertEquals("Salt to taste", parseIngredientLine("Salt to taste").name)
        assertEquals(
            "Parsley, finely chopped",
            parseIngredientLine("Parsley, finely chopped").let { "${it.name}, ${it.modifier}" },
        )
    }

    @Test
    fun parses_leading_bare_fraction_as_implicit_one() {
        // OCR drops the leading "1" of "1/2cup", leaving "/2cup". Without the implicit-1
        // restore the line has no qty signal and gets filtered out as junk.
        val r = parseIngredientLine("/2cup pasta cooking water")
        assertEquals(Rational.of(1, 2), r.qty)
        assertEquals("cup", r.unit)
        assertEquals("pasta cooking water", r.name)
    }

    @Test
    fun strips_leading_ocr_checkbox_before_bare_fraction() {
        // Checkbox-as-letter sits in front of an OCR-dropped-numerator fraction: "D /2cup …".
        // Both fixes must chain: strip the "D ", then restore "/2" → "1/2".
        val r = parseIngredientLine("D /2cup pasta cooking water")
        assertEquals(Rational.of(1, 2), r.qty)
        assertEquals("cup", r.unit)
        assertEquals("pasta cooking water", r.name)
    }

    @Test
    fun strips_leading_ocr_checkbox_glued_to_qty() {
        // OCR sometimes glues the checkbox-as-letter straight onto the qty: "U1tbsp …".
        val r = parseIngredientLine("U1tbsp cooking/kosher salt (for cooking pasta)")
        assertEquals(Rational.of(1, 1), r.qty)
        assertEquals("tbsp", r.unit)
        assertTrue("name was '${r.name}'", r.name.contains("salt"))
    }

    @Test
    fun recovers_dual_metric_imperial_when_g_misread_as_9() {
        // OCR read "175g/6 oz" as "1759/6 0z" — without recovery, "1759/6" parses as a fraction.
        val r = parseIngredientLine("1759/6 0z guanciale (pancetta or block bacon)")
        assertEquals(Rational.of(175, 1), r.qty)
        assertEquals("g", r.unit)
        assertEquals("guanciale", r.name)
        assertTrue("modifier was '${r.modifier}'", r.modifier!!.contains("pancetta or block bacon"))
    }

    @Test
    fun does_not_mangle_real_fractions_with_non_imperial_unit() {
        // Guard against DUAL_METRIC_IMPERIAL with-optional-unit firing on "1/4 tsp pepper".
        val r = parseIngredientLine("1/4 tsp black pepper")
        assertEquals(Rational.of(1, 4), r.qty)
        assertEquals("tsp", r.unit)
        assertEquals("black pepper", r.name)
    }

    @Test
    fun recipetineats_capture_recovers_all_nine_ingredients() {
        // Verbatim raw OCR (2026-06-02 Pixel capture, recipetineats.com/carbonara, capture #3
        // after zoom+crop). All nine real ingredients must survive refine; the GARNISH section
        // and standalone garnish names must not.
        val ocr = listOf(
            "175g/6 oz guanciale (pancetta or block bacon), weight after skin removed (Note",
            "1)",
            "2 large eggs (Note 2)",
            "2 egg yolks (Note 2)",
            "100g/3.5 0Z parmigiano reggiano, finely shredded (or pecorino romano, sub",
            "pamesan, Note 3)",
            "/4 tsp black pepper",
            "D 400g/14 0Z spaghetti",
            "1tbsp cooking/kosher salt (for cooking pasta)",
            "D /2cup pasta cooking water",
            "1garlic clove, finely minced (optional, Note 4)",
            "GARNISH (OPTIONAL):",
            "Parsley, finely chopped",
            "Parmigiano reggiano",
        )
        val refined = refineForIngredients(parseIngredientBlock(ocr.joinToString("\n")))
        val names = refined.joinToString(" | ") { it.name.lowercase() }
        listOf("guanciale", "eggs", "yolks", "parmigiano", "pepper", "spaghetti", "salt", "water", "garlic")
            .forEach { needle ->
                assertTrue("'$needle' missing in: $names", names.contains(needle))
            }
        // Guanciale must be 175 g, not the 1759/6 fraction.
        val guanciale = refined.single { it.name.lowercase().contains("guanciale") }
        assertEquals(Rational.of(175, 1), guanciale.qty)
        assertEquals("g", guanciale.unit)
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
