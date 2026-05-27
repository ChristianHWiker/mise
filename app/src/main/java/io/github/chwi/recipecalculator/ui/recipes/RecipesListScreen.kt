package io.github.chwi.recipecalculator.ui.recipes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.chwi.recipecalculator.ui.common.PlaceholderScreen
import io.github.chwi.recipecalculator.ui.theme.RecipeTheme

/** Home / library. Placeholder until the editorial Home screen is built. */
@Composable
fun RecipesListScreen(
    onRecipeClick: (Long) -> Unit,
    onAddRecipe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        PlaceholderScreen(
            kicker = "Cookbook",
            title = "My cookbook",
            note = "Recipe library lands here. Tap + to add a recipe.",
        )
        ExtendedFloatingActionButton(
            onClick = onAddRecipe,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add recipe") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(RecipeTheme.spacing.xxl),
        )
    }
}
