package io.github.chwi.recipecalculator.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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

    @Update
    suspend fun updateRecipe(recipe: RecipeEntity)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsFor(recipeId: Long)

    @Query("DELETE FROM tags WHERE recipeId = :recipeId")
    suspend fun deleteTagsFor(recipeId: Long)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: Long)

    /**
     * Replace a recipe's children wholesale: update the header row, then delete and reinsert
     * ingredients and tags stamped with the existing recipe id. Ingredient/tag rows have no
     * user-facing identity, so swap-and-replace is simpler and correct vs. diffing.
     */
    @Transaction
    suspend fun updateRecipeWithIngredients(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        tags: List<TagEntity>,
    ) {
        updateRecipe(recipe)
        deleteIngredientsFor(recipe.id)
        deleteTagsFor(recipe.id)
        insertIngredients(ingredients.map { it.copy(id = 0, recipeId = recipe.id) })
        insertTags(tags.map { it.copy(id = 0, recipeId = recipe.id) })
    }
}
