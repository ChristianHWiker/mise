package io.github.chwi.recipecalculator.ui.recipes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.chwi.recipecalculator.ui.common.PlaceholderScreen

/** Create/edit a recipe. Placeholder until the editor is built. */
@Composable
fun RecipeEditorScreen(
    recipeId: Long?,
    modifier: Modifier = Modifier,
) {
    PlaceholderScreen(
        kicker = if (recipeId == null) "New" else "Edit",
        title = if (recipeId == null) "New recipe" else "Edit recipe #$recipeId",
        note = "The recipe editor form lands here.",
        modifier = modifier,
    )
}
