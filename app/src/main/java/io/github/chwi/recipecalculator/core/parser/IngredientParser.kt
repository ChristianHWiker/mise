package io.github.chwi.recipecalculator.core.parser

import io.github.chwi.recipecalculator.core.rational.Rational

/**
 * Result of parsing a single OCR'd ingredient line. Fields are best-effort: missing pieces
 * (e.g. no recognizable quantity) leave the corresponding fields null/blank rather than
 * dropping the row, so the confirmation screen can show the raw text alongside whatever
 * we extracted.
 *
 * [confidence] is a coarse 0..1 signal driving the "needs review" badge in the UI:
 *  - 1.0 → qty + unit + name all parsed cleanly
 *  - 0.7 → qty + name parsed, unit absent or unrecognized
 *  - 0.5 → only a name (no quantity) — likely "salt to taste"
 *  - 0.2 → nothing structural recognized; raw text only
 */
data class ParsedLine(
    val qty: Rational? = null,
    val unit: String? = null,
    val name: String = "",
    val modifier: String? = null,
    val confidence: Float = 0f,
    val rawText: String = "",
)

/** Units the parser recognizes; mirrors and extends `UNIT_CHOICES` from the editor. */
private val UNIT_ALIASES: Map<String, String> = buildMap {
    listOf("cup", "cups", "c").forEach { put(it, "cup") }
    listOf("tablespoon", "tablespoons", "tbsp", "tbs", "tbl", "tb", "T").forEach { put(it, "tbsp") }
    listOf("teaspoon", "teaspoons", "tsp", "ts", "t").forEach { put(it, "tsp") }
    listOf("gram", "grams", "g", "gr").forEach { put(it, "g") }
    listOf("kilogram", "kilograms", "kg").forEach { put(it, "kg") }
    listOf("milliliter", "milliliters", "millilitre", "millilitres", "ml", "mL").forEach { put(it, "ml") }
    listOf("liter", "liters", "litre", "litres", "l").forEach { put(it, "ml") } // collapse to ml; user can edit
    listOf("ounce", "ounces", "oz").forEach { put(it, "g") } // collapse to g; user can edit
    listOf("pound", "pounds", "lb", "lbs").forEach { put(it, "kg") }
}

/** Map of unicode vulgar fractions to ASCII equivalents, applied before tokenization. */
private val UNICODE_FRACTIONS = mapOf(
    "½" to " 1/2", "¼" to " 1/4", "¾" to " 3/4",
    "⅛" to " 1/8", "⅜" to " 3/8", "⅝" to " 5/8", "⅞" to " 7/8",
    "⅓" to " 1/3", "⅔" to " 2/3",
    "⅕" to " 1/5", "⅖" to " 2/5", "⅗" to " 3/5", "⅘" to " 4/5",
    "⅙" to " 1/6", "⅚" to " 5/6",
)

/** Leading list-marker noise that printed recipes (and OCR) often have. */
private val LEADING_NOISE = Regex("""^[\s•\-–—*·●▪■□◦◯○]+""")

/** A leading enumeration like "1." or "1)" only if followed by something else — keep bare "1" as a qty. */
private val LEADING_ENUMERATION = Regex("""^\d+\s*[.)]\s+""")

/** Mixed number or simple fraction at line start, e.g. "1 1/2 ", "3/4 ", "2 ". */
private val QTY_REGEX = Regex("""^(\d+\s+\d+\s*/\s*\d+|\d+\s*/\s*\d+|\d+(?:\.\d+)?)\s+""")

/** A trailing parenthetical, e.g. "(packed)" — folded into the modifier. */
private val TRAILING_PAREN = Regex("""\s*\(([^)]+)\)\s*$""")

/**
 * Parse a single OCR'd ingredient line into a best-effort structured row.
 *
 * Pipeline:
 *  1. Strip leading list noise and trailing punctuation; normalize unicode fractions to ASCII.
 *  2. Strip a leading enumeration ("1. ", "2) ") that's clearly an item number, not a quantity.
 *  3. Match a leading mixed/fraction/decimal quantity and feed it to [Rational.parseOrNull].
 *  4. Match the next token against the unit alias table.
 *  5. Split the remainder on the first comma (or " — "): left = name, right = modifier.
 *  6. Also fold a trailing parenthetical into the modifier.
 *
 * Anything that doesn't match falls through with a low confidence; [rawText] is preserved
 * so the user can edit from the original on the confirmation screen.
 */
fun parseIngredientLine(rawInput: String): ParsedLine {
    val rawText = rawInput.trim()
    if (rawText.isEmpty()) return ParsedLine(rawText = rawText)

    var working = rawText
        .replace(LEADING_NOISE, "")
        .let { stripped ->
            var s = stripped
            UNICODE_FRACTIONS.forEach { (glyph, ascii) -> s = s.replace(glyph, ascii) }
            s.trim().replace(Regex("""\s+"""), " ")
        }

    // "1. flour" → "flour" (enumeration), but leave bare "1" alone — it's a qty.
    working = working.replace(LEADING_ENUMERATION, "")

    // Trailing parenthetical → modifier hint, removed from name.
    var parenModifier: String? = null
    TRAILING_PAREN.find(working)?.let { m ->
        parenModifier = m.groupValues[1].trim()
        working = working.removeRange(m.range).trim()
    }
    // Drop trailing punctuation like "." or ";"
    working = working.trimEnd('.', ';', ',')

    val qtyMatch = QTY_REGEX.find(working)
    val qty = qtyMatch?.groupValues?.get(1)?.let { Rational.parseOrNull(it) }
    if (qtyMatch != null) working = working.removeRange(qtyMatch.range).trim()

    // First whitespace-delimited token may be a unit.
    val firstSpace = working.indexOf(' ')
    val maybeUnitToken = if (firstSpace == -1) working else working.substring(0, firstSpace)
    val canonicalUnit = UNIT_ALIASES[maybeUnitToken.trimEnd('.').lowercase()]
        ?: UNIT_ALIASES[maybeUnitToken.trimEnd('.')] // case-sensitive aliases ("T" vs "t")
    if (canonicalUnit != null) {
        working = if (firstSpace == -1) "" else working.substring(firstSpace + 1).trim()
    }

    // "flour, sifted" or "flour — sifted" → name + modifier.
    val (name, commaModifier) = splitOnceOnComma(working)

    val combinedModifier = listOfNotNull(commaModifier?.takeIf { it.isNotBlank() }, parenModifier)
        .joinToString(", ")
        .takeIf { it.isNotEmpty() }

    val confidence = when {
        qty != null && canonicalUnit != null && name.isNotBlank() -> 1.0f
        qty != null && name.isNotBlank() -> 0.7f
        qty == null && name.isNotBlank() -> 0.5f
        else -> 0.2f
    }

    return ParsedLine(
        qty = qty,
        unit = canonicalUnit,
        name = name.trim(),
        modifier = combinedModifier,
        confidence = confidence,
        rawText = rawText,
    )
}

/** Split on the first ',' or ' — '/' - ' separator; everything after is the modifier. */
private fun splitOnceOnComma(input: String): Pair<String, String?> {
    val commaIdx = input.indexOf(',')
    val dashIdx = listOf(" — ", " – ", " - ").map { input.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: -1
    val splitIdx = when {
        commaIdx == -1 -> dashIdx
        dashIdx == -1 -> commaIdx
        else -> minOf(commaIdx, dashIdx)
    }
    return if (splitIdx < 0) input to null
    else input.substring(0, splitIdx).trim() to input.substring(splitIdx + 1).trim().trimStart('—', '–', '-', ' ')
}

/** Parse a multi-line OCR blob into one [ParsedLine] per non-blank line. */
fun parseIngredientBlock(text: String): List<ParsedLine> =
    text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map(::parseIngredientLine)
        .toList()

/**
 * Whittle a raw parser pass over an OCR'd page down to lines that actually look like ingredients.
 *
 * Real-world recipe photos — especially web screenshots — contain a lot of non-ingredient text:
 * page chrome, sidebars, equipment lists, instruction steps, ratings, footers. The parser will
 * happily produce a [ParsedLine] for each of those (with low confidence), but presenting 30+ such
 * rows on the confirmation screen drowns the actual ingredients. We filter to rows that show
 * positive ingredient signal:
 *  - they have a recognized quantity, **or**
 *  - they explicitly opt in via "to taste" / "q.s." / "a pinch"-style phrasing.
 *
 * For surviving rows we also trim the name at the first sentence-boundary — web recipes routinely
 * append a descriptive paragraph after each ingredient on the same OCR line ("400 gr pasta. Best
 * choice: spaghetti…"), and the parser's first pass can't tell where the ingredient stops.
 */
fun refineForIngredients(rows: List<ParsedLine>): List<ParsedLine> =
    rows
        .filter { looksLikeIngredient(it) }
        .map(::trimNameAtSentenceBoundary)

private val UNITLESS_KEYWORDS = listOf("to taste", "q.s.", "qb", "a pinch", "a dash", "a few")

private fun looksLikeIngredient(row: ParsedLine): Boolean {
    if (row.qty != null) return true
    val haystack = (row.name + " " + (row.modifier ?: "")).lowercase()
    return UNITLESS_KEYWORDS.any { it in haystack }
}

/** Words that almost certainly start a descriptive clause, not part of an ingredient's name. */
private val CLAUSE_STOP_WORDS = setOf(
    "in", "for", "from", "to", "with", "without", "best", "depending",
    "according", "this", "that", "preferably", "or", "plus", "and",
)

/**
 * A period that ends a real word (3+ letters) followed by whitespace — i.e. a sentence boundary,
 * not an abbreviation like `q.s.` or `tbsp.` where the period sits next to a single letter.
 */
private val PERIOD_SENTENCE = Regex("""[A-Za-z]{3,}\.\s+""")

private fun trimNameAtSentenceBoundary(row: ParsedLine): ParsedLine {
    val name = row.name
    if (name.isEmpty()) return row

    val cutIdx = findCutIndex(name)
    if (cutIdx == null || cutIdx >= name.length - 1) return row

    val keptName = name.substring(0, cutIdx).trimEnd(',', ' ', '.', ';', ':')
    val tail = name.substring(cutIdx).trim().trimEnd(',', '.', ';', ':')
    if (keptName.isEmpty()) return row // refuse to amputate to nothing

    val mergedModifier = listOf(row.modifier?.trim(), tail.takeIf { it.isNotBlank() })
        .filterNotNull()
        .filter { it.isNotBlank() }
        .joinToString(", ")
        .takeIf { it.isNotEmpty() }

    return row.copy(name = keptName, modifier = mergedModifier)
}

/** Return the offset of the earliest sentence-boundary cut in [name], or null if none applies. */
private fun findCutIndex(name: String): Int? {
    val candidates = mutableListOf<Int>()

    // (a) A real sentence-ending period — the period follows a 3+ letter word.
    PERIOD_SENTENCE.find(name)?.let { match ->
        val periodIdx = name.indexOf('.', match.range.first)
        if (periodIdx > 0) candidates += periodIdx
    }

    // (b) Colon, em-dash, en-dash.
    val colon = name.indexOf(": ")
    if (colon > 0) candidates += colon
    listOf(" — ", " – ").forEach { sep ->
        val idx = name.indexOf(sep)
        if (idx > 0) candidates += idx
    }

    // (c) A capital letter immediately after a lowercase-letter word, e.g. "yolks Depending".
    Regex("""[a-z]\s+[A-Z]""").find(name)?.let { match ->
        candidates += match.range.first + 1
    }

    // (d) A clause stop-word in lowercase position (i.e. not the very first token).
    Regex("""\s+""").findAll(name).map { it.range.first }.forEach { spaceIdx ->
        val word = name.substring(spaceIdx + 1)
            .substringBefore(' ')
            .lowercase()
            .trimEnd(',', '.', ';', ':')
        if (word in CLAUSE_STOP_WORDS) candidates += spaceIdx
    }

    return candidates.filter { it > 0 }.minOrNull()
}
