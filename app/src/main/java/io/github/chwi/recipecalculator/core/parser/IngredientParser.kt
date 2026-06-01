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

/**
 * A dual "metric/imperial" quantity printed glued together at line start, as recipe sites do:
 * "100g/3.5 oz", "175g/6 oz", "400g/14 oz". We keep the metric measure (`$1 $2`) and discard
 * the imperial alternative. The imperial unit token is matched loosely (`[a-zA-Z0-9]{1,3}`)
 * because OCR routinely mangles "oz" into "0z"/"0Z". The metric number tolerates an O/o in
 * place of a zero ("10Og" → "100g") — another stock OCR confusion — cleaned up in the replacement.
 */
private val DUAL_METRIC_IMPERIAL =
    Regex("""^([\dOo]+(?:[.,]\d+)?)\s*([a-zA-Z]+)\s*/\s*\d+(?:[.,]\d+)?\s*[a-zA-Z0-9]{1,3}\b""")

/** A leading number glued straight onto a unit or word — "100g", "1garlic", "1/2cup". */
private val NUMBER_GLUED_TO_WORD = Regex("""^(\d+\s*/\s*\d+|\d+(?:[.,]\d+)?)([a-zA-Z])""")

/** Any parenthetical, e.g. "(packed)" or a mid-name "(pancetta or block bacon)" — folded into the modifier. */
private val PARENTHETICAL = Regex("""\(([^)]*)\)""")

/**
 * Parse a single OCR'd ingredient line into a best-effort structured row.
 *
 * Pipeline:
 *  1. Strip leading list noise and trailing punctuation; normalize unicode fractions to ASCII.
 *  2. Strip a leading enumeration ("1. ", "2) ") that's clearly an item number, not a quantity.
 *  3. Match a leading mixed/fraction/decimal quantity and feed it to [Rational.parseOrNull].
 *  4. Match the next token against the unit alias table.
 *  5. Split the remainder on the first comma (or " — "): left = name, right = modifier.
 *  6. Also fold any parenthetical (trailing or mid-name) into the modifier.
 *
 * Anything that doesn't match falls through with a low confidence; [rawText] is preserved
 * so the user can edit from the original on the confirmation screen.
 */
fun parseIngredientLine(rawInput: String): ParsedLine {
    val rawText = rawInput.trim()
    if (rawText.isEmpty()) return ParsedLine(rawText = rawText)

    var working = rawText.replace(LEADING_NOISE, "")
    UNICODE_FRACTIONS.forEach { (glyph, ascii) -> working = working.replace(glyph, ascii) }
    working = working
        // Collapse a glued metric/imperial pair to its metric half (fixing an O-for-0 in the
        // number), then split any number that sits flush against the following letter — both
        // happen before whitespace normalization.
        .replace(DUAL_METRIC_IMPERIAL) { m ->
            "${m.groupValues[1].replace('O', '0').replace('o', '0')} ${m.groupValues[2]} "
        }
        .replace(NUMBER_GLUED_TO_WORD, "$1 $2")
        .replace(Regex("""\s+"""), " ")
        .trim()

    // "1. flour" → "flour" (enumeration), but leave bare "1" alone — it's a qty.
    working = working.replace(LEADING_ENUMERATION, "")

    // Parentheticals anywhere → modifier hints, removed from the name. Recipe sites park
    // clarifications mid-name ("guanciale (pancetta or block bacon)"), not only at the end.
    val parenModifiers = mutableListOf<String>()
    working = PARENTHETICAL.replace(working) { m ->
        m.groupValues[1].trim().takeIf { it.isNotEmpty() }?.let { parenModifiers += it }
        " "
    }.replace(Regex("""\s+"""), " ").trim()
    val parenModifier = parenModifiers.joinToString(", ").takeIf { it.isNotEmpty() }
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

/** Parse a multi-line OCR blob into one [ParsedLine] per logical ingredient line. */
fun parseIngredientBlock(text: String): List<ParsedLine> =
    mergeWrappedLines(
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList(),
    ).map(::parseIngredientLine)

/**
 * Reassemble ingredient lines that ML Kit split because they wrapped on the page.
 *
 * A long ingredient (e.g. "100g/3.5 oz parmigiano reggiano … (or pecorino romano, sub /
 * parmesan, Note 3)") is emitted as two physical lines, and the trailing fragment
 * ("parmesan, Note 3)") has no quantity of its own — so it would be parsed as a junk row and
 * the real ingredient truncated. We fold a line into its predecessor only when it reads as a
 * wrapped parenthetical tail — it opens with a parenthesis, or it carries a closing ')' with no
 * matching '(' of its own. We deliberately do *not* merge on a bare lowercase start, since
 * unitless ingredients legitimately begin that way ("q.s. black pepper", "a pinch of salt").
 */
fun mergeWrappedLines(lines: List<String>): List<String> {
    val merged = mutableListOf<String>()
    for (raw in lines) {
        val line = raw.trim()
        if (line.isEmpty()) continue
        if (merged.isNotEmpty() && isContinuation(line)) {
            merged[merged.lastIndex] = "${merged.last()} $line"
        } else {
            merged += line
        }
    }
    return merged
}

private fun isContinuation(line: String): Boolean {
    val first = line.first()
    if (first == '(' || first == ')') return true
    // A ')' appearing before any '(' is a dangling close — the tail of a parenthetical that
    // opened on the previous line ("… sub" / "parmesan, Note 3)").
    val open = line.indexOf('(')
    val close = line.indexOf(')')
    return close >= 0 && (open < 0 || close < open)
}

/**
 * Whittle a raw parser pass over an OCR'd page down to lines that actually look like ingredients.
 *
 * Real-world recipe photos — especially web screenshots — contain a lot of non-ingredient text:
 * page chrome, sidebars, equipment lists, instruction steps, ratings, footers. The parser will
 * happily produce a [ParsedLine] for each of those (with low confidence), but presenting 30+ such
 * rows on the confirmation screen drowns the actual ingredients. We filter to rows that show
 * positive ingredient signal:
 *  - they have a recognized quantity, **or**
 *  - they explicitly opt in via "to taste" / "q.s." / "a pinch"-style phrasing,
 *
 * while explicitly rejecting numbered instruction steps, whose step number would otherwise read
 * as a quantity (see [looksLikeInstructionStep]).
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

/**
 * A numbered instruction step masquerading as a quantified ingredient: a leading step number
 * ("1", "(2", "3.") immediately followed by a Capitalized lead-in word, e.g. "1 Guanciale Cut
 * into…", "(2 Carbonara sauce Place…", "3 Cook pasta Bring…". The step number parses as a
 * quantity, so without this guard these survive the qty filter. Ingredient lines instead read
 * "2 large eggs" / "100 g spaghetti" — lowercase (or a unit) after the number. The number may be
 * glued straight onto the word ("1Guanciale", "3Cook"), so no whitespace is required between them.
 */
private val NUMBERED_STEP_LEAD = Regex("""^\(?\d{1,2}[.):]?\s*[A-Z][a-z]""")

/** A "lowercase-word Capitalized-word" break — the signature of a sentence, not an ingredient name. */
private val SENTENCE_CASE_BREAK = Regex("""[a-z]\s+[A-Z]""")

private fun looksLikeInstructionStep(raw: String): Boolean =
    NUMBERED_STEP_LEAD.containsMatchIn(raw) &&
        SENTENCE_CASE_BREAK.containsMatchIn(raw) &&
        raw.split(Regex("""\s+""")).size > 5

private fun looksLikeIngredient(row: ParsedLine): Boolean {
    if (looksLikeInstructionStep(row.rawText)) return false
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
