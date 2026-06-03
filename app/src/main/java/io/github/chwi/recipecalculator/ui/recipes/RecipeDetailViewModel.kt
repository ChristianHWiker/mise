package io.github.chwi.recipecalculator.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chwi.recipecalculator.core.rational.FractionStyle
import io.github.chwi.recipecalculator.core.rational.Rational
import io.github.chwi.recipecalculator.core.units.UnitSystem
import io.github.chwi.recipecalculator.core.units.displayQty
import io.github.chwi.recipecalculator.data.model.RecipeWithIngredients
import io.github.chwi.recipecalculator.data.repository.RecipeRepository
import io.github.chwi.recipecalculator.data.settings.AppSettings
import io.github.chwi.recipecalculator.data.settings.SettingsRepository
import io.github.chwi.recipecalculator.navigation.RecipeDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt
import javax.inject.Inject

/** A display-ready ingredient line on the detail screen. */
data class IngredientRowUi(
    val id: Long,
    val name: String,
    val modifier: String?,
    val qtyText: String,
    val unitText: String,
)

/** The ingredient a reverse-scale sheet is open for, with everything the sheet needs to do its math. */
data class SheetTarget(
    val ingredientId: Long,
    val name: String,
    val baseQty: Rational,
    val unit: String,
    val gramsPerCup: Int?,
)

data class DetailUiState(
    val loading: Boolean = true,
    val recipeId: Long = 0,
    val number: String = "",
    val category: String = "",
    val title: String = "",
    val metaLine: String = "",
    val pinned: Boolean = false,
    val photoUri: String? = null,
    val yieldUnit: String = "",
    val baseServings: Int = 1,
    val servings: Int = 1,
    val factor: Double = 1.0,
    val fractionStyle: FractionStyle = FractionStyle.INLINE,
    val unitSystem: UnitSystem = UnitSystem.US,
    val ingredients: List<IngredientRowUi> = emptyList(),
    val steps: List<String> = emptyList(),
    val sheet: SheetTarget? = null,
)

/**
 * Backs the recipe detail / scaling screen. The recipe id arrives type-safely from the navigation
 * route via [SavedStateHandle]. Live recipe data and user settings are combined with two pieces of
 * screen state — the current scale [factor] and which ingredient's reverse-scale [sheetIngredientId]
 * is open — into one [DetailUiState]. Quantities are scaled and converted at read time; the stored
 * recipe is never mutated by scaling.
 */
@HiltViewModel
class RecipeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RecipeRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val recipeId: Long = savedStateHandle.toRoute<RecipeDetail>().recipeId

    private val factor = MutableStateFlow(1.0)
    private val sheetIngredientId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeRecipe(recipeId),
        settingsRepository.settings,
        factor,
        sheetIngredientId,
    ) { recipe, settings, factor, sheetId ->
        recipe?.let { build(it, settings, factor, sheetId) } ?: DetailUiState(loading = true, recipeId = recipeId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetailUiState(recipeId = recipeId),
    )

    /** Nudge the yield up or down. Step scales with the recipe size (min 1), per the handoff. */
    fun changeServings(increase: Boolean) {
        val state = uiState.value
        val step = max(1, (state.baseServings / 10.0).roundToInt())
        val target = state.servings + if (increase) step else -step
        factor.value = max(1, target).toDouble() / state.baseServings
    }

    fun openSheet(ingredientId: Long) { sheetIngredientId.value = ingredientId }

    fun closeSheet() { sheetIngredientId.value = null }

    /** Apply a reverse-scale result: rescale the whole recipe and dismiss the sheet. */
    fun applyScale(newFactor: Double) {
        if (newFactor > 0) factor.value = newFactor
        sheetIngredientId.value = null
    }

    fun togglePin() {
        val pinned = uiState.value.pinned
        viewModelScope.launch { repository.setPinned(recipeId, !pinned) }
    }

    private fun build(
        recipe: RecipeWithIngredients,
        settings: AppSettings,
        factor: Double,
        sheetId: Long?,
    ): DetailUiState {
        val header = recipe.recipe
        val ingredients = recipe.orderedIngredients
        val servings = max(1, (header.servings * factor).roundToInt())

        val rows = ingredients.map { ing ->
            val d = displayQty(
                baseQty = Rational.of(ing.qtyNum, ing.qtyDen),
                unit = ing.unit,
                gramsPerCup = ing.gramsPerCup,
                factor = factor,
                fractionStyle = settings.fractionStyle,
                system = settings.unitSystem,
            )
            IngredientRowUi(
                id = ing.id,
                name = ing.name,
                modifier = ing.modifier,
                qtyText = d.qtyText,
                unitText = d.unitText,
            )
        }

        val sheet = sheetId?.let { id ->
            ingredients.firstOrNull { it.id == id }?.let { ing ->
                SheetTarget(
                    ingredientId = ing.id,
                    name = ing.name,
                    baseQty = Rational.of(ing.qtyNum, ing.qtyDen),
                    unit = ing.unit,
                    gramsPerCup = ing.gramsPerCup,
                )
            }
        }

        return DetailUiState(
            loading = false,
            recipeId = header.id,
            number = "№ %03d".format(header.id),
            category = header.category,
            title = header.title,
            metaLine = "${header.timeMinutes} min · ${header.difficulty} · ${ingredients.size} ingredients",
            pinned = header.pinned,
            photoUri = header.photoUri,
            yieldUnit = header.yieldUnit,
            baseServings = header.servings,
            servings = servings,
            factor = factor,
            fractionStyle = settings.fractionStyle,
            unitSystem = settings.unitSystem,
            ingredients = rows,
            steps = header.steps,
            sheet = sheet,
        )
    }
}

