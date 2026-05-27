package io.github.chwi.recipecalculator.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.github.chwi.recipecalculator.data.model.IngredientEntity
import io.github.chwi.recipecalculator.data.model.RecipeEntity
import io.github.chwi.recipecalculator.data.model.RecipeWithIngredients
import io.github.chwi.recipecalculator.data.model.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Insert
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Insert
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Insert
    suspend fun insertTags(tags: List<TagEntity>)

    /**
     * Insert a recipe together with its ingredients and tags in one transaction, stamping each
     * child row with the generated recipe id. Returns the new recipe id.
     */
    @Transaction
    suspend fun insertRecipeWithIngredients(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        tags: List<TagEntity>,
    ): Long {
        val recipeId = insertRecipe(recipe)
        insertIngredients(ingredients.map { it.copy(recipeId = recipeId) })
        insertTags(tags.map { it.copy(recipeId = recipeId) })
        return recipeId
    }

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY createdAt DESC")
    fun observeRecipesWithIngredients(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getRecipeWithIngredients(id: Long): RecipeWithIngredients?

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeRecipeWithIngredients(id: Long): Flow<RecipeWithIngredients?>

    @Query("SELECT COUNT(*) FROM recipes")
    fun observeRecipeCount(): Flow<Int>

    @Query("UPDATE recipes SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)
}
