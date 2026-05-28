package io.github.chwi.recipecalculator.ui.recipes

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.chwi.recipecalculator.data.model.IngredientEntity
import io.github.chwi.recipecalculator.data.model.RecipeEntity
import io.github.chwi.recipecalculator.data.model.RecipeWithIngredients
import io.github.chwi.recipecalculator.data.model.TagEntity
import io.github.chwi.recipecalculator.data.repository.RecipeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeEditorViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun `new recipe with empty title fails validation`() = runTest {
        val vm = RecipeEditorViewModel(savedState(recipeId = null), FakeRepo())
        vm.save()
        val errors = vm.state.value.errors
        assertTrue("expected title error", errors.title)
        assertTrue("expected no-valid-ingredient error", errors.noValidIngredient)
    }

    @Test fun `new recipe with unparseable quantity flags the row`() = runTest {
        val vm = RecipeEditorViewModel(savedState(recipeId = null), FakeRepo())
        vm.onTitleChange("Test")
        vm.onServingsChange("4")
        val key = vm.state.value.ingredients.first().key
        vm.updateIngredient(key) { it.copy(qtyText = "abc", name = "flour") }
        vm.save()
        assertTrue(key in vm.state.value.errors.ingredientKeys)
    }

    @Test fun `save creates a new recipe with parsed entities`() = runTest {
        val repo = FakeRepo()
        val vm = RecipeEditorViewModel(savedState(recipeId = null), repo)
        vm.onTitleChange("Cookies")
        vm.onCategoryChange("Cookies")
        vm.onTimeChange("30")
        vm.onServingsChange("12")
        vm.onYieldUnitChange("cookies")
        vm.onTagsChange("Bake, Holiday, bake")
        val key = vm.state.value.ingredients.first().key
        vm.updateIngredient(key) { it.copy(qtyText = "1 1/2", unit = "cup", name = "flour") }

        vm.results.test {
            vm.save()
            val result = awaitItem()
            assertTrue("expected Created, got $result", result is SaveResult.Created)
        }

        val saved = repo.lastAdded
        assertNotNull(saved)
        assertEquals("Cookies", saved!!.recipe.title)
        assertEquals(12, saved.recipe.servings)
        assertEquals(30, saved.recipe.timeMinutes)
        assertEquals(1, saved.ingredients.size)
        val ing = saved.ingredients.first()
        assertEquals("flour", ing.name)
        assertEquals(3, ing.qtyNum)
        assertEquals(2, ing.qtyDen)
        assertEquals("cup", ing.unit)
        assertEquals(120, ing.gramsPerCup) // density auto-filled from lookup
        assertEquals(listOf("bake", "holiday"), saved.tags.map { it.name }) // dedup + lowercase
    }

    @Test fun `loading existing recipe populates the draft`() = runTest {
        val existing = sampleRecipe(id = 7L)
        val repo = FakeRepo(existing = existing)
        val vm = RecipeEditorViewModel(savedState(recipeId = 7L), repo)
        // give the init load a chance to run on the unconfined dispatcher
        val state = vm.state.value
        assertEquals("Cookies", state.title)
        assertEquals("12", state.servings)
        assertEquals(1, state.ingredients.size)
        assertEquals("1 1/2", state.ingredients.first().qtyText)
        assertEquals("weeknight, bake", state.tagsInput)
    }

    @Test fun `editing preserves createdAt`() = runTest {
        val existing = sampleRecipe(id = 7L).run {
            copy(recipe = recipe.copy(createdAt = 1_000L))
        }
        val repo = FakeRepo(existing = existing)
        val vm = RecipeEditorViewModel(savedState(recipeId = 7L), repo)
        vm.onTitleChange("Better cookies")
        vm.results.test {
            vm.save()
            assertTrue(awaitItem() is SaveResult.Updated)
        }
        assertEquals(1_000L, repo.lastUpdated?.recipe?.createdAt)
        assertEquals("Better cookies", repo.lastUpdated?.recipe?.title)
    }

    @Test fun `delete emits Deleted and calls repository`() = runTest {
        val repo = FakeRepo(existing = sampleRecipe(id = 7L))
        val vm = RecipeEditorViewModel(savedState(recipeId = 7L), repo)
        vm.results.test {
            vm.delete()
            assertEquals(SaveResult.Deleted, awaitItem())
        }
        assertEquals(7L, repo.lastDeleted)
    }

    @Test fun `move ingredient reorders the list`() = runTest {
        val vm = RecipeEditorViewModel(savedState(recipeId = null), FakeRepo())
        vm.addIngredient()
        vm.addIngredient()
        val before = vm.state.value.ingredients.map { it.key }
        vm.moveIngredient(0, 2)
        val after = vm.state.value.ingredients.map { it.key }
        assertEquals(listOf(before[1], before[2], before[0]), after)
    }

    @Test fun `remove ingredient with single row keeps one fresh row`() = runTest {
        val vm = RecipeEditorViewModel(savedState(recipeId = null), FakeRepo())
        val key = vm.state.value.ingredients.first().key
        vm.removeIngredient(key)
        assertEquals(1, vm.state.value.ingredients.size)
        assertTrue(vm.state.value.ingredients.first().key != key)
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun savedState(recipeId: Long?): SavedStateHandle =
        SavedStateHandle(buildMap { if (recipeId != null) put("recipeId", recipeId) })

    private fun sampleRecipe(id: Long): RecipeWithIngredients =
        RecipeWithIngredients(
            recipe = RecipeEntity(
                id = id,
                title = "Cookies",
                category = "Cookies",
                timeMinutes = 30,
                difficulty = "Easy",
                servings = 12,
                yieldUnit = "cookies",
                steps = listOf("Mix", "Bake"),
            ),
            ingredients = listOf(
                IngredientEntity(
                    id = 100, recipeId = id, position = 0,
                    name = "flour", qtyNum = 3, qtyDen = 2, unit = "cup",
                    gramsPerCup = 120, modifier = null,
                ),
            ),
            tags = listOf(
                TagEntity(id = 1, recipeId = id, name = "weeknight"),
                TagEntity(id = 2, recipeId = id, name = "bake"),
            ),
        )
}

/** Minimal in-memory test double for [RecipeRepository]. */
private class FakeRepo(
    private val existing: RecipeWithIngredients? = null,
) : RecipeRepository {

    var lastAdded: RecipeWithIngredients? = null
    var lastUpdated: RecipeWithIngredients? = null
    var lastDeleted: Long? = null

    override fun observeRecipes(): Flow<List<RecipeWithIngredients>> = flowOf(emptyList())
    override fun observeRecipeCount(): Flow<Int> = MutableStateFlow(0)
    override fun observeRecipe(id: Long): Flow<RecipeWithIngredients?> = MutableStateFlow(existing)
    override suspend fun getRecipe(id: Long): RecipeWithIngredients? = existing?.takeIf { it.recipe.id == id }
    override suspend fun setPinned(id: Long, pinned: Boolean) {}

    override suspend fun addRecipe(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        tags: List<TagEntity>,
    ): Long {
        lastAdded = RecipeWithIngredients(recipe.copy(id = 42), ingredients, tags)
        return 42L
    }

    override suspend fun updateRecipe(
        recipe: RecipeEntity,
        ingredients: List<IngredientEntity>,
        tags: List<TagEntity>,
    ) {
        lastUpdated = RecipeWithIngredients(recipe, ingredients, tags)
    }

    override suspend fun deleteRecipe(id: Long) {
        lastDeleted = id
    }
}
