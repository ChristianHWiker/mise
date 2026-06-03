package io.github.chwi.recipecalculator.data.seed

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.chwi.recipecalculator.R
import io.github.chwi.recipecalculator.data.repository.RecipeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the starter recipe into an empty database. Invoked once from [RecipeApp][
 * io.github.chwi.recipecalculator.RecipeApp] on launch; the empty check makes it idempotent, so it
 * never duplicates rows on subsequent starts or after the user adds their own recipes.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RecipeRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun seedIfEmpty() {
        scope.launch {
            if (repository.observeRecipeCount().first() == 0) {
                repository.addRecipe(
                    SampleData.recipe.copy(photoUri = sampleCookiePhotoUri()),
                    SampleData.ingredients,
                )
            }
        }
    }

    /**
     * Copy the bundled sample-cookie drawable into the same `recipe_photos/` directory the photo
     * picker uses and return a `file://` URI to it — the identical code path as a user-added photo,
     * so the image renders through the same loader without any special-casing. Returns null on I/O
     * failure (the recipe just falls back to the gradient placeholder).
     */
    private fun sampleCookiePhotoUri(): String? = try {
        val dir = File(context.filesDir, "recipe_photos").apply { mkdirs() }
        val target = File(dir, "sample_cookies.jpg")
        if (!target.exists()) {
            context.resources.openRawResource(R.drawable.sample_cookies).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        }
        Uri.fromFile(target).toString()
    } catch (_: Throwable) {
        null
    }
}
