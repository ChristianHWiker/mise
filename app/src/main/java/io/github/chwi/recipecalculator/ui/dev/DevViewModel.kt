package io.github.chwi.recipecalculator.ui.dev

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chwi.recipecalculator.data.model.RecipeWithIngredients
import io.github.chwi.recipecalculator.data.repository.RecipeRepository
import io.github.chwi.recipecalculator.data.seed.SampleData
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
            repository.addRecipe(SampleData.recipe, SampleData.ingredients)
        }
    }
}
