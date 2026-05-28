package io.github.chwi.recipecalculator.core.density

/**
 * Ingredient-name → `gramsPerCup` lookup used by the editor when the user picks a volume unit
 * (cup/tbsp/tsp). The value feeds `IngredientEntity.gramsPerCup`, which drives metric conversion.
 *
 * Unknown names return null and conversion in [io.github.chwi.recipecalculator.core.units.convert]
 * falls back gracefully (the cup quantity is left as-is). v1 ships a curated starter table; entries
 * can be added without API change.
 */
fun lookupGramsPerCup(name: String): Int? =
    KNOWN_DENSITIES[normalize(name)]

/**
 * Normalize an ingredient name for lookup. Trim, lowercase, collapse internal whitespace,
 * and strip a single trailing 's' so basic English plurals collide with their singulars
 * ("eggs" → "egg"). Lookup keys in [KNOWN_DENSITIES] are stored already-normalized.
 */
internal fun normalize(name: String): String {
    val collapsed = name.trim().lowercase().replace(WHITESPACE, " ")
    return if (collapsed.length > 1 && collapsed.endsWith('s')) {
        collapsed.dropLast(1)
    } else {
        collapsed
    }
}

private val WHITESPACE = Regex("\\s+")

/**
 * Curated densities in grams per US cup. Sourced from common baking references (King Arthur,
 * USDA). Where convention varies (e.g. brown sugar packed vs unpacked), the packed value wins
 * because that's what most recipes mean. Keys are pre-normalized: lowercase, no trailing 's'.
 */
internal val KNOWN_DENSITIES: Map<String, Int> = mapOf(
    // Flours
    "flour" to 120,
    "all-purpose flour" to 120,
    "ap flour" to 120,
    "bread flour" to 127,
    "whole wheat flour" to 113,
    "cake flour" to 114,
    "almond flour" to 96,
    "rye flour" to 102,

    // Sugars
    "sugar" to 200,
    "granulated sugar" to 200,
    "white sugar" to 200,
    "brown sugar" to 213,
    "light brown sugar" to 213,
    "dark brown sugar" to 213,
    "powdered sugar" to 120,
    "confectioners sugar" to 120,

    // Fats
    "butter" to 227,
    "olive oil" to 216,
    "vegetable oil" to 218,
    "coconut oil" to 218,
    "shortening" to 205,

    // Dry pantry
    "rolled oat" to 90,
    "steel-cut oat" to 165,
    "cocoa powder" to 85,
    "baking soda" to 220,
    "baking powder" to 230,
    "kosher salt" to 240,
    "salt" to 273,
    "table salt" to 273,
    "cornstarch" to 128,
    "breadcrumb" to 108,
    "panko" to 50,
    "white rice" to 200,
    "rice" to 200,

    // Liquids
    "water" to 237,
    "milk" to 244,
    "buttermilk" to 242,
    "heavy cream" to 238,
    "yogurt" to 245,
    "sour cream" to 230,
    "honey" to 340,
    "maple syrup" to 322,

    // Mix-ins
    "chocolate chip" to 170,
    "peanut butter" to 258,
    "walnut" to 117,
    "pecan" to 109,
    "almond" to 143,
    "shredded coconut" to 75,
    "raisin" to 146,
    "cream cheese" to 232,
)
