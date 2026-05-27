package io.github.chwi.recipecalculator.data.repository

import io.github.chwi.recipecalculator.data.db.RecipeDao
import io.github.chwi.recipecalculator.data.model.IngredientEntity
import io.github.chwi.recipecalculator.data.model.RecipeEntity
import io.github.chwi.recipecalculator.data.model.RecipeWithIngredients
import io.github.chwi.recipecalculator.data.model.TagEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepositoryImpl @Inject constructor(
    private val dao: RecipeDao,
) : RecipeRepository {

    override fun observeRecipes(): Flow<List<RecipeWithIngredients>> =
        dao.observeRecipesWithIngredients()

    override fun observeRecipeCount(): Flow<Int> = dao.observeRecipeCount()

    override suspend fun getRecipe(id: Long): RecipeWithIngredients? =
        dao.getRecipeWithIngredients(id)

    override suspend fun addRecipe(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        tags: List<TagEntity>,
    ): Long = dao.insertRecipeWithIngredients(recipe, ingredients, tags)
}
