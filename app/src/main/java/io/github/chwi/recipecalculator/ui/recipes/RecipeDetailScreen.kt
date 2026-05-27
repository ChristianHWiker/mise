package io.github.chwi.recipecalculator.ui.recipes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.chwi.recipecalculator.ui.common.PlaceholderScreen

/** Recipe detail / scaling screen. Placeholder until the editorial Detail screen is built. */
@Composable
fun RecipeDetailScreen(
    recipeId: Long,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        kicker = "Recipe",
        title = "Recipe #$recipeId",
        note = "Forward + reverse scaling lives here.",
        modifier = modifier,
    )
}
