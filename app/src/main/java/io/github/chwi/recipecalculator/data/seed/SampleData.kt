package io.github.chwi.recipecalculator.data.seed

import io.github.chwi.recipecalculator.data.model.IngredientEntity
import io.github.chwi.recipecalculator.data.model.RecipeEntity

/**
 * The brown-butter cookie recipe from the design handoff's `data.js`, expressed as Room entities.
 * Seeded on first launch (see [DatabaseSeeder]) so the app opens onto a real recipe, and reused by
 * the developer harness.
 *
 * Quantities are exact `num`/`den` rationals; `gramsPerCup` is the density used for metric
 * conversion (null for countable or volume-only items like eggs and extracts).
 */
object SampleData {

    val recipe = RecipeEntity(
        title = "Brown butter chocolate chip cookies",
        titleShort = "Brown butter cookies",
        category = "Cookies",
        timeMinutes = 45,
        difficulty = "Easy",
        servings = 12,
        yieldUnit = "cookies",
        steps = listOf(
            "Brown the butter in a pan over medium heat until nutty and amber. Cool 10 min.",
            "Whisk dry ingredients. Beat butter and sugars; add eggs and vanilla.",
            "Fold dry into wet, then chocolate. Rest dough 30 min, or chill overnight.",
            "Bake at 180 °C, 12–14 min, until edges set and centers look underdone.",
        ),
    )

    val ingredients = listOf(
        ingredient(0, "All-purpose flour", 9, 4, "cup", 120),
        ingredient(1, "Brown butter", 1, 1, "cup", 225, "cooled"),
        ingredient(2, "Granulated sugar", 3, 4, "cup", 200),
        ingredient(3, "Dark brown sugar", 3, 4, "cup", 220, "packed"),
        ingredient(4, "Eggs", 2, 1, "ea", null, "large"),
        ingredient(5, "Vanilla extract", 2, 1, "tsp", null),
        ingredient(6, "Baking soda", 1, 1, "tsp", null),
        ingredient(7, "Flaky sea salt", 1, 1, "tsp", null),
        ingredient(8, "Dark chocolate chips", 2, 1, "cup", 170),
    )

    private fun ingredient(
        position: Int,
        name: String,
        num: Int,
        den: Int,
        unit: String,
        gramsPerCup: Int?,
        modifier: String? = null,
    ) = IngredientEntity(
        recipeId = 0, // stamped with the real id inside the insert transaction
        position = position,
        name = name,
        qtyNum = num,
        qtyDen = den,
        unit = unit,
        gramsPerCup = gramsPerCup,
        modifier = modifier,
    )
}
