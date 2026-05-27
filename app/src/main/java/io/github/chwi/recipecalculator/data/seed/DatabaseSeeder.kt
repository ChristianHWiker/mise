package io.github.chwi.recipecalculator.data.seed

import io.github.chwi.recipecalculator.data.repository.RecipeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the starter recipe into an empty database. Invoked once from [RecipeApp][
 * io.github.chwi.recipecalculator.RecipeApp] on launch; the empty check makes it idempotent, so it
 * never duplicates rows on subsequent starts or after the user adds their own recipes.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val repository: RecipeRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun seedIfEmpty() {
        scope.launch {
            if (repository.observeRecipeCount().first() == 0) {
                repository.addRecipe(SampleData.recipe, SampleData.ingredients)
            }
        }
    }
}
