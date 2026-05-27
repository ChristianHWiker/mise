package io.github.chwi.recipecalculator.ui.dev

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chwi.recipecalculator.data.model.IngredientEntity
import io.github.chwi.recipecalculator.data.model.RecipeEntity
import io.github.chwi.recipecalculator.data.model.RecipeWithIngredients
import io.github.chwi.recipecalculator.data.repository.RecipeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the developer harness: inserts a known sample recipe and observes everything read back
 * from Room, so the full persistence path (Hilt → repository → DAO → Room → Flow) can be
 * exercised on-device. This is the Phase 00 "done when" check.
 */
@HiltViewModel
class DevViewModel @Inject constructor(
    private val repository: RecipeRepository,
) : ViewModel() {

    val recipes: StateFlow<List<RecipeWithIngredients>> =
        repository.observeRecipes().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun insertSampleRecipe() {
        viewModelScope.launch {
            repository.addRecipe(SampleRecipe.recipe, SampleRecipe.ingredients)
        }
    }
}

/** The brown-butter cookie recipe from the design handoff's `data.js`, as Room entities. */
private object SampleRecipe {
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

    // qty stored as exact num/den; gramsPerCup is the density for unit conversion (null = countable).
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
