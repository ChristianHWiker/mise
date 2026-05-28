package io.github.chwi.recipecalculator.data.repository

import io.github.chwi.recipecalculator.data.model.IngredientEntity
import io.github.chwi.recipecalculator.data.model.RecipeEntity
import io.github.chwi.recipecalculator.data.model.RecipeWithIngredients
import io.github.chwi.recipecalculator.data.model.TagEntity
import kotlinx.coroutines.flow.Flow

/** Read/write access to recipes. The single point the UI layer talks to for persistence. */
interface RecipeRepository {

    fun observeRecipes(): Flow<List<RecipeWithIngredients>>

    fun observeRecipeCount(): Flow<Int>

    /** Observe a single recipe, re-emitting on any change (e.g. pin toggled). Emits null if absent. */
    fun observeRecipe(id: Long): Flow<RecipeWithIngredients?>

    suspend fun getRecipe(id: Long): RecipeWithIngredients?

    suspend fun setPinned(id: Long, pinned: Boolean)

    suspend fun addRecipe(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        tags: List<TagEntity> = emptyList(),
    ): Long

    /** Replace an existing recipe's header, ingredients and tags atomically. */
    suspend fun updateRecipe(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        tags: List<TagEntity> = emptyList(),
    )

    suspend fun deleteRecipe(id: Long)
}
