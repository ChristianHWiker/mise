package io.github.chwi.recipecalculator.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chwi.recipecalculator.core.density.lookupGramsPerCup
import io.github.chwi.recipecalculator.core.rational.Rational
import io.github.chwi.recipecalculator.data.model.IngredientEntity
import io.github.chwi.recipecalculator.data.model.RecipeEntity
import io.github.chwi.recipecalculator.data.model.TagEntity
import io.github.chwi.recipecalculator.data.repository.RecipeRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Editor mode: creating a brand-new recipe vs. editing an existing one. */
sealed interface EditorMode {
    data object New : EditorMode
    data class Edit(val recipeId: Long) : EditorMode
}

/** One draft ingredient row. Kept as raw text so intermediate keystrokes don't lose data. */
data class IngredientDraft(
    val key: Long,
    val qtyText: String = "",
    val unit: String = "cup",
    val name: String = "",
    val modifier: String = "",
    val gramsPerCup: Int? = null,
)

data class StepDraft(val key: Long, val text: String = "")

/** Per-field validation flags; populated by [RecipeEditorViewModel.save] when blocking. */
data class ValidationErrors(
    val title: Boolean = false,
    val servings: Boolean = false,
    val timeMinutes: Boolean = false,
    val noValidIngredient: Boolean = false,
    val ingredientKeys: Set<Long> = emptySet(),
) {
    val any: Boolean get() = title || servings || timeMinutes || noValidIngredient || ingredientKeys.isNotEmpty()
}

data class EditorUiState(
    val mode: EditorMode = EditorMode.New,
    val loading: Boolean = false,
    val title: String = "",
    val category: String = "",
    val timeMinutes: String = "",
    val difficulty: String = DIFFICULTIES.first(),
    val servings: String = "",
    val yieldUnit: String = "",
    val pinned: Boolean = false,
    val photoUri: String? = null,
    val ingredients: List<IngredientDraft> = listOf(IngredientDraft(key = 1L)),
    val steps: List<StepDraft> = listOf(StepDraft(key = 1L)),
    val tagsInput: String = "",
    val errors: ValidationErrors = ValidationErrors(),
    val saving: Boolean = false,
) {
    val isEdit: Boolean get() = mode is EditorMode.Edit
}

sealed interface SaveResult {
    data class Created(val id: Long) : SaveResult
    data class Updated(val id: Long) : SaveResult
    data object Deleted : SaveResult
    data class Error(val message: String) : SaveResult
}

val DIFFICULTIES = listOf("Easy", "Medium", "Hard")

/** Units the user can pick on an ingredient row. Order matches the dropdown. */
val UNIT_CHOICES = listOf("cup", "tbsp", "tsp", "g", "kg", "ml", "ea")

private val VOLUME_UNITS_FOR_DENSITY = setOf("cup", "tbsp", "tsp")

/**
 * Backs the create/edit screen. The recipe id arrives type-safely from the [RecipeEditor] route via
 * [SavedStateHandle]; a null id opens an empty draft, a non-null id loads the existing recipe and
 * maps it into editable form. The screen drives the VM through field setters and row operations;
 * `save()` and `delete()` emit one-shot [SaveResult]s for navigation.
 */
@HiltViewModel
class RecipeEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: RecipeRepository,
) : ViewModel() {

    // Navigation Compose populates SavedStateHandle from the type-safe RecipeEditor route under
    // the property name. Reading directly avoids the toRoute<>() serializer path, which needs the
    // navigation runtime registered and would have to be stubbed in unit tests.
    private val recipeId: Long? = savedStateHandle.get<Long>("recipeId")

    private val _state = MutableStateFlow(
        EditorUiState(
            mode = recipeId?.let { EditorMode.Edit(it) } ?: EditorMode.New,
            loading = recipeId != null,
        )
    )
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val _results = Channel<SaveResult>(Channel.BUFFERED)
    val results = _results.receiveAsFlow()

    private var nextKey: Long = 2L

    init {
        if (recipeId != null) viewModelScope.launch { loadExisting(recipeId) }
    }

    private suspend fun loadExisting(id: Long) {
        val recipe = repository.getRecipe(id)
        if (recipe == null) {
            _results.send(SaveResult.Error("Recipe not found"))
            return
        }
        val header = recipe.recipe
        val ingredients = recipe.orderedIngredients.map { ing ->
            IngredientDraft(
                key = freshKey(),
                qtyText = Rational.of(ing.qtyNum, ing.qtyDen).toUserText(),
                unit = ing.unit,
                name = ing.name,
                modifier = ing.modifier.orEmpty(),
                gramsPerCup = ing.gramsPerCup,
            )
        }.ifEmpty { listOf(IngredientDraft(key = freshKey())) }
        val steps = header.steps.map { StepDraft(key = freshKey(), text = it) }
            .ifEmpty { listOf(StepDraft(key = freshKey())) }
        _state.update {
            it.copy(
                loading = false,
                title = header.title,
                category = header.category,
                timeMinutes = if (header.timeMinutes > 0) header.timeMinutes.toString() else "",
                difficulty = header.difficulty.takeIf { d -> d in DIFFICULTIES } ?: DIFFICULTIES.first(),
                servings = header.servings.toString(),
                yieldUnit = header.yieldUnit,
                pinned = header.pinned,
                photoUri = header.photoUri,
                ingredients = ingredients,
                steps = steps,
                tagsInput = recipe.tags.joinToString(", ") { it.name },
            )
        }
    }

    // --- Header field setters ------------------------------------------------

    fun onTitleChange(value: String) = _state.update { it.copy(title = value, errors = it.errors.copy(title = false)) }
    fun onCategoryChange(value: String) = _state.update { it.copy(category = value) }
    fun onTimeChange(value: String) = _state.update { it.copy(timeMinutes = value.filter(Char::isDigit), errors = it.errors.copy(timeMinutes = false)) }
    fun onDifficultyChange(value: String) = _state.update { it.copy(difficulty = value) }
    fun onServingsChange(value: String) = _state.update { it.copy(servings = value.filter(Char::isDigit), errors = it.errors.copy(servings = false)) }
    fun onYieldUnitChange(value: String) = _state.update { it.copy(yieldUnit = value) }
    fun onPinnedChange(value: Boolean) = _state.update { it.copy(pinned = value) }
    fun onPhotoUriChange(value: String?) = _state.update { it.copy(photoUri = value) }
    fun onTagsChange(value: String) = _state.update { it.copy(tagsInput = value) }

    // --- Ingredient row ops --------------------------------------------------

    fun addIngredient() = _state.update {
        it.copy(ingredients = it.ingredients + IngredientDraft(key = freshKey()))
    }

    fun removeIngredient(key: Long) = _state.update {
        val next = it.ingredients.filterNot { row -> row.key == key }
            .ifEmpty { listOf(IngredientDraft(key = freshKey())) }
        it.copy(ingredients = next)
    }

    fun moveIngredient(fromIndex: Int, toIndex: Int) = _state.update {
        if (fromIndex == toIndex) return@update it
        val list = it.ingredients.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return@update it
        list.add(toIndex, list.removeAt(fromIndex))
        it.copy(ingredients = list)
    }

    fun updateIngredient(key: Long, transform: (IngredientDraft) -> IngredientDraft) = _state.update { state ->
        val next = state.ingredients.map { if (it.key == key) transform(it).withDensity() else it }
        state.copy(
            ingredients = next,
            errors = state.errors.copy(
                ingredientKeys = state.errors.ingredientKeys - key,
                noValidIngredient = false,
            ),
        )
    }

    /** Refresh `gramsPerCup` from the lookup table when name or unit changes. */
    private fun IngredientDraft.withDensity(): IngredientDraft {
        if (unit !in VOLUME_UNITS_FOR_DENSITY) return this
        val looked = lookupGramsPerCup(name)
        // Don't clobber an existing user-provided density on a name typo; only fill when null.
        return if (gramsPerCup == null && looked != null) copy(gramsPerCup = looked) else this
    }

    // --- Step row ops --------------------------------------------------------

    fun addStep() = _state.update { it.copy(steps = it.steps + StepDraft(key = freshKey())) }

    fun removeStep(key: Long) = _state.update {
        val next = it.steps.filterNot { row -> row.key == key }
            .ifEmpty { listOf(StepDraft(key = freshKey())) }
        it.copy(steps = next)
    }

    fun moveStep(fromIndex: Int, toIndex: Int) = _state.update {
        if (fromIndex == toIndex) return@update it
        val list = it.steps.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return@update it
        list.add(toIndex, list.removeAt(fromIndex))
        it.copy(steps = list)
    }

    fun updateStep(key: Long, text: String) = _state.update { state ->
        state.copy(steps = state.steps.map { if (it.key == key) it.copy(text = text) else it })
    }

    // --- Save / delete -------------------------------------------------------

    fun save() {
        val current = _state.value
        if (current.saving) return

        val title = current.title.trim()
        val titleBad = title.isEmpty()
        val servings = current.servings.toIntOrNull()
        val servingsBad = servings == null || servings < 1
        val timeMinutes = if (current.timeMinutes.isBlank()) 0 else current.timeMinutes.toIntOrNull() ?: -1
        val timeBad = timeMinutes < 0

        // Drop pure-scratch rows (empty everywhere). Validate the rest.
        val rows = current.ingredients.filterNot {
            it.qtyText.isBlank() && it.name.isBlank() && it.modifier.isBlank()
        }
        val badIngredientKeys = mutableSetOf<Long>()
        val parsed = rows.mapNotNull { row ->
            val qty = Rational.parseOrNull(row.qtyText)
            val name = row.name.trim()
            if (qty == null || qty <= Rational.ZERO || name.isEmpty()) {
                badIngredientKeys += row.key
                null
            } else {
                row to qty
            }
        }
        val noValid = parsed.isEmpty()

        val errors = ValidationErrors(
            title = titleBad,
            servings = servingsBad,
            timeMinutes = timeBad,
            noValidIngredient = noValid,
            ingredientKeys = badIngredientKeys,
        )
        if (errors.any) {
            _state.update { it.copy(errors = errors) }
            return
        }

        val ingredients = parsed.mapIndexed { idx, (row, qty) ->
            IngredientEntity(
                id = 0L,
                recipeId = 0L,
                position = idx,
                name = row.name.trim(),
                qtyNum = qty.num,
                qtyDen = qty.den,
                unit = row.unit,
                gramsPerCup = row.gramsPerCup,
                modifier = row.modifier.trim().ifEmpty { null },
            )
        }
        val steps = current.steps.map { it.text.trim() }.filter { it.isNotEmpty() }
        val tags = current.tagsInput.split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .map { TagEntity(recipeId = 0L, name = it) }

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                when (val mode = current.mode) {
                    EditorMode.New -> {
                        val header = RecipeEntity(
                            title = title,
                            category = current.category.trim(),
                            timeMinutes = timeMinutes,
                            difficulty = current.difficulty,
                            servings = servings!!,
                            yieldUnit = current.yieldUnit.trim(),
                            steps = steps,
                            photoUri = current.photoUri,
                            pinned = current.pinned,
                        )
                        val id = repository.addRecipe(header, ingredients, tags)
                        _results.send(SaveResult.Created(id))
                    }
                    is EditorMode.Edit -> {
                        val existing = repository.getRecipe(mode.recipeId)?.recipe
                        val header = RecipeEntity(
                            id = mode.recipeId,
                            title = title,
                            category = current.category.trim(),
                            timeMinutes = timeMinutes,
                            difficulty = current.difficulty,
                            servings = servings!!,
                            yieldUnit = current.yieldUnit.trim(),
                            steps = steps,
                            photoUri = current.photoUri,
                            pinned = current.pinned,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            lastCookedAt = existing?.lastCookedAt,
                        )
                        repository.updateRecipe(header, ingredients, tags)
                        _results.send(SaveResult.Updated(mode.recipeId))
                    }
                }
            } catch (t: Throwable) {
                _results.send(SaveResult.Error(t.message ?: "Could not save recipe"))
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    fun delete() {
        val mode = _state.value.mode
        if (mode !is EditorMode.Edit || _state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                repository.deleteRecipe(mode.recipeId)
                _results.send(SaveResult.Deleted)
            } catch (t: Throwable) {
                _results.send(SaveResult.Error(t.message ?: "Could not delete recipe"))
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    private fun freshKey(): Long = nextKey++
}

/**
 * Render a stored rational the way the user originally typed it, as close as possible:
 * whole numbers stay bare, anything else uses the "1 1/2" mixed form (parseable round-trip).
 */
private fun Rational.toUserText(): String = when {
    den == 1 -> num.toString()
    num.compareTo(den) > 0 -> {
        val whole = num / den
        val rem = num - whole * den
        if (rem == 0) whole.toString() else "$whole $rem/$den"
    }
    else -> "$num/$den"
}
