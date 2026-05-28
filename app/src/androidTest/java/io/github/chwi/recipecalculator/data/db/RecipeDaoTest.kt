package io.github.chwi.recipecalculator.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chwi.recipecalculator.data.model.IngredientEntity
import io.github.chwi.recipecalculator.data.model.RecipeEntity
import io.github.chwi.recipecalculator.data.model.TagEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeDaoTest {

    private lateinit var db: RecipeDatabase
    private lateinit var dao: RecipeDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()
        dao = db.recipeDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertRecipeWithIngredients_readsBackInOrder() = runBlocking {
        val recipe = RecipeEntity(
            title = "Test cookies",
            category = "Cookies",
            timeMinutes = 45,
            difficulty = "Easy",
            servings = 12,
            yieldUnit = "cookies",
            steps = listOf("Mix", "Bake"),
        )
        val ingredients = listOf(
            IngredientEntity(recipeId = 0, position = 1, name = "Sugar", qtyNum = 3, qtyDen = 4, unit = "cup"),
            IngredientEntity(recipeId = 0, position = 0, name = "Flour", qtyNum = 9, qtyDen = 4, unit = "cup"),
        )

        val id = dao.insertRecipeWithIngredients(recipe, ingredients, emptyList())
        val readBack = dao.getRecipeWithIngredients(id)

        assertNotNull(readBack)
        requireNotNull(readBack)
        assertEquals("Test cookies", readBack.recipe.title)
        assertEquals(listOf("Mix", "Bake"), readBack.recipe.steps)
        // Ordered by position: Flour (0) before Sugar (1).
        assertEquals(listOf("Flour", "Sugar"), readBack.orderedIngredients.map { it.name })
        assertEquals(9, readBack.orderedIngredients.first().qtyNum)
        assertEquals(4, readBack.orderedIngredients.first().qtyDen)
    }

    @Test
    fun updateRecipeWithIngredients_replacesChildren() = runBlocking {
        val id = dao.insertRecipeWithIngredients(
            recipe = RecipeEntity(
                title = "Original",
                category = "Cookies",
                timeMinutes = 30,
                difficulty = "Easy",
                servings = 12,
                yieldUnit = "cookies",
                steps = listOf("Mix"),
            ),
            ingredients = listOf(
                IngredientEntity(recipeId = 0, position = 0, name = "Flour", qtyNum = 1, qtyDen = 1, unit = "cup"),
            ),
            tags = listOf(TagEntity(recipeId = 0, name = "old")),
        )

        val updatedRecipe = RecipeEntity(
            id = id,
            title = "Updated",
            category = "Bread",
            timeMinutes = 60,
            difficulty = "Medium",
            servings = 8,
            yieldUnit = "loaves",
            steps = listOf("Knead", "Rest", "Bake"),
        )
        val updatedIngredients = listOf(
            IngredientEntity(recipeId = 0, position = 0, name = "Bread flour", qtyNum = 3, qtyDen = 1, unit = "cup", gramsPerCup = 127),
            IngredientEntity(recipeId = 0, position = 1, name = "Yeast", qtyNum = 2, qtyDen = 1, unit = "tsp"),
        )
        val updatedTags = listOf(
            TagEntity(recipeId = 0, name = "weeknight"),
            TagEntity(recipeId = 0, name = "bake"),
        )

        dao.updateRecipeWithIngredients(updatedRecipe, updatedIngredients, updatedTags)
        val readBack = requireNotNull(dao.getRecipeWithIngredients(id))

        assertEquals("Updated", readBack.recipe.title)
        assertEquals("Bread", readBack.recipe.category)
        assertEquals(listOf("Knead", "Rest", "Bake"), readBack.recipe.steps)
        assertEquals(listOf("Bread flour", "Yeast"), readBack.orderedIngredients.map { it.name })
        assertEquals(setOf("weeknight", "bake"), readBack.tags.map { it.name }.toSet())
    }

    @Test
    fun deleteRecipe_cascadesToChildren() = runBlocking {
        val id = dao.insertRecipeWithIngredients(
            recipe = RecipeEntity(
                title = "Going away",
                category = "Cookies",
                timeMinutes = 0,
                difficulty = "Easy",
                servings = 1,
                yieldUnit = "batch",
                steps = emptyList(),
            ),
            ingredients = listOf(
                IngredientEntity(recipeId = 0, position = 0, name = "Salt", qtyNum = 1, qtyDen = 4, unit = "tsp"),
            ),
            tags = listOf(TagEntity(recipeId = 0, name = "scratch")),
        )

        dao.deleteRecipe(id)

        // The recipe is gone; its ingredients and tags cascade-delete with it (FK ON DELETE CASCADE).
        assertNull(dao.getRecipeWithIngredients(id))
    }
}
