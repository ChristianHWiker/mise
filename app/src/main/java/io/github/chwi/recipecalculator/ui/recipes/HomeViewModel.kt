package io.github.chwi.recipecalculator.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chwi.recipecalculator.data.model.RecipeWithIngredients
import io.github.chwi.recipecalculator.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** One row in the cookbook library. */
data class RecipeRowUi(
    val id: Long,
    val title: String,
    val category: String,
    val yieldText: String,
    val timeText: String,
    val pinned: Boolean,
    val toneSeed: Int,
)

/** Everything the Home screen renders, derived from the recipe list + the active search/tag. */
data class HomeUiState(
    val dateKicker: String = "",
    val recipeCount: Int = 0,
    val pinnedCount: Int = 0,
    val lastCookedLabel: String? = null,
    val tags: List<String> = listOf(ALL),
    val activeTag: String = ALL,
    val search: String = "",
    val sectionTitle: String = "All recipes",
    val recipes: List<RecipeRowUi> = emptyList(),
) {
    val sectionCount: Int get() = recipes.size

    companion object {
        const val ALL = "All"
        const val PINNED = "Pinned"
    }
}

/**
 * Backs the editorial Home screen. Recipe data flows in from Room; [search] and [tag] are local UI
 * state. The three are combined into a single [HomeUiState] so the screen only ever observes one
 * thing. Filtering and labels are derived here, keeping the composable declarative.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: RecipeRepository,
) : ViewModel() {

    private val search = MutableStateFlow("")
    private val tag = MutableStateFlow(HomeUiState.ALL)
    private val dateKicker = formatDateKicker(Date())

    val uiState: StateFlow<HomeUiState> =
        combine(repository.observeRecipes(), search, tag) { recipes, query, activeTag ->
            buildState(recipes, query, activeTag)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(dateKicker = dateKicker),
        )

    val searchState: StateFlow<String> = search.asStateFlow()

    fun onSearchChange(value: String) { search.value = value }

    fun onTagSelected(value: String) { tag.value = value }

    private fun buildState(
        recipes: List<RecipeWithIngredients>,
        query: String,
        activeTag: String,
    ): HomeUiState {
        val categories = recipes.map { it.recipe.category }.distinct().sorted()
        val tags = buildList {
            add(HomeUiState.ALL)
            if (recipes.any { it.recipe.pinned }) add(HomeUiState.PINNED)
            addAll(categories)
        }
        // Guard against a stale selection (e.g. the last recipe of a category was deleted).
        val effectiveTag = if (activeTag in tags) activeTag else HomeUiState.ALL

        val filtered = recipes.filter { r ->
            val matchesTag = when (effectiveTag) {
                HomeUiState.ALL -> true
                HomeUiState.PINNED -> r.recipe.pinned
                else -> r.recipe.category == effectiveTag
            }
            val matchesSearch = query.isBlank() ||
                r.recipe.title.contains(query.trim(), ignoreCase = true)
            matchesTag && matchesSearch
        }

        val lastCooked = recipes.mapNotNull { it.recipe.lastCookedAt }.maxOrNull()

        return HomeUiState(
            dateKicker = dateKicker,
            recipeCount = recipes.size,
            pinnedCount = recipes.count { it.recipe.pinned },
            lastCookedLabel = lastCooked?.let { relativeTime(it) },
            tags = tags,
            activeTag = effectiveTag,
            search = query,
            sectionTitle = if (effectiveTag == HomeUiState.ALL) "All recipes" else effectiveTag,
            recipes = filtered.map { it.toRowUi() },
        )
    }
}

private fun RecipeWithIngredients.toRowUi() = RecipeRowUi(
    id = recipe.id,
    title = recipe.title,
    category = recipe.category,
    yieldText = "${recipe.servings} ${recipe.yieldUnit}",
    timeText = "${recipe.timeMinutes} min",
    pinned = recipe.pinned,
    toneSeed = (recipe.id % Int.MAX_VALUE).toInt(),
)

/** "TUESDAY · 27 MAY" — uses the legacy date API to stay correct on API 24/25 (no desugaring). */
private fun formatDateKicker(date: Date): String {
    val dow = SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
    val dayMonth = SimpleDateFormat("d MMM", Locale.getDefault()).format(date)
    return "$dow · $dayMonth".uppercase(Locale.getDefault())
}

private fun relativeTime(epochMillis: Long): String {
    val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - epochMillis)
    return when {
        days <= 0 -> "today"
        days == 1L -> "yesterday"
        days < 7 -> "$days days ago"
        days < 14 -> "last week"
        days < 30 -> "${days / 7} weeks ago"
        days < 60 -> "last month"
        else -> "${days / 30} months ago"
    }
}
