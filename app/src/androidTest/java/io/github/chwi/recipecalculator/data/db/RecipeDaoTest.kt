package io.github.chwi.recipecalculator.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.chwi.recipecalculator.data.model.IngredientEntity
import io.github.chwi.recipecalculator.data.model.RecipeEntity
import kotlinx.coroutines.runBlocking
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
}
