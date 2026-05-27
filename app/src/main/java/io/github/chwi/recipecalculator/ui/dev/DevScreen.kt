package io.github.chwi.recipecalculator.ui.dev

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chwi.recipecalculator.core.rational.FractionStyle
import io.github.chwi.recipecalculator.core.rational.Rational
import io.github.chwi.recipecalculator.core.rational.format
import io.github.chwi.recipecalculator.data.model.IngredientEntity
import io.github.chwi.recipecalculator.data.model.RecipeWithIngredients
import io.github.chwi.recipecalculator.ui.theme.RecipeTheme

/**
 * Developer harness (Phase 00 verification). Inserts the sample recipe into Room and renders
 * whatever is read back, formatting each quantity through [Rational] to prove the num/den columns
 * round-trip and the fraction display works end to end.
 */
@Composable
fun DevScreen(
    modifier: Modifier = Modifier,
    viewModel: DevViewModel = hiltViewModel(),
) {
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(RecipeTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.lg),
    ) {
        Text(
            text = "DEVELOPER TOOLS",
            style = RecipeTheme.typography.kicker,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Room round-trip",
            style = RecipeTheme.typography.detailH1,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Button(
            onClick = viewModel::insertSampleRecipe,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Insert sample recipe")
        }
        Text(
            text = "${recipes.size} recipe(s) in the database",
            style = RecipeTheme.typography.body,
            color = RecipeTheme.colors.muted,
        )
        HorizontalDivider(color = RecipeTheme.colors.rule)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.xl)) {
            items(recipes, key = { it.recipe.id }) { recipe ->
                RecipeReadback(recipe)
            }
        }
    }
}

@Composable
private fun RecipeReadback(recipe: RecipeWithIngredients) {
    Column(verticalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.xxs)) {
        Text(
            text = recipe.recipe.title,
            style = RecipeTheme.typography.sectionH2,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "${recipe.recipe.servings} ${recipe.recipe.yieldUnit} · ${recipe.recipe.timeMinutes} min · ${recipe.recipe.difficulty}",
            style = RecipeTheme.typography.caption,
            color = RecipeTheme.colors.muted,
        )
        recipe.orderedIngredients.forEach { ingredient ->
            Text(
                text = ingredientLine(ingredient),
                style = RecipeTheme.typography.body,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Render an ingredient as "qty unit name", quantity formatted through [Rational]. */
private fun ingredientLine(ingredient: IngredientEntity): String {
    val qty = Rational.of(ingredient.qtyNum, ingredient.qtyDen)
    val qtyText = qty.format(FractionStyle.INLINE)
    val unit = if (ingredient.unit == "ea") "" else ingredient.unit
    val modifier = ingredient.modifier?.let { " ($it)" } ?: ""
    return listOf(qtyText, unit, ingredient.name).filter { it.isNotBlank() }
        .joinToString(" ") + modifier
}
