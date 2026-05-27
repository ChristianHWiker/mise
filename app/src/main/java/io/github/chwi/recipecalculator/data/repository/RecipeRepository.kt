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

    suspend fun getRecipe(id: Long): RecipeWithIngredients?

    suspend fun addRecipe(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        tags: List<TagEntity> = emptyList(),
    ): Long
}
