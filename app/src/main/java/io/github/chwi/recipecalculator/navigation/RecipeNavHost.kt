package io.github.chwi.recipecalculator.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import io.github.chwi.recipecalculator.ui.capture.CaptureScreen
import io.github.chwi.recipecalculator.ui.dev.DevScreen
import io.github.chwi.recipecalculator.ui.recipes.RecipeDetailScreen
import io.github.chwi.recipecalculator.ui.recipes.RecipeEditorScreen
import io.github.chwi.recipecalculator.ui.recipes.RecipesListScreen
import io.github.chwi.recipecalculator.ui.settings.SettingsScreen

/**
 * The single-activity navigation graph. The recipe flow (list → detail → editor, plus the capture
 * stub) is a nested graph so the bottom bar can treat it as one tab and preserve its back stack.
 */
@Composable
fun RecipeNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = RecipesGraph,
        modifier = modifier,
    ) {
        navigation<RecipesGraph>(startDestination = RecipesList) {
            composable<RecipesList> {
                RecipesListScreen(
                    onRecipeClick = { navController.navigate(RecipeDetail(it)) },
                    onAddRecipe = { navController.navigate(RecipeEditor()) },
                )
            }
            composable<RecipeDetail> {
                RecipeDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(RecipeEditor(it)) },
                )
            }
            composable<RecipeEditor> { entry ->
                val editor = entry.toRoute<RecipeEditor>()
                RecipeEditorScreen(
                    recipeId = editor.recipeId,
                    onSaved = { id ->
                        if (editor.recipeId == null) {
                            // New recipe: replace the editor entry so back from Detail returns to the list.
                            navController.popBackStack()
                            navController.navigate(RecipeDetail(id))
                        } else {
                            // Existing recipe: just pop back to the detail that's already on the stack.
                            navController.popBackStack()
                        }
                    },
                    onDeleted = {
                        navController.popBackStack(RecipesList, inclusive = false)
                    },
                    onCancel = { navController.popBackStack() },
                )
            }
            composable<Capture> { CaptureScreen() }
        }

        composable<Settings> {
            SettingsScreen(onOpenDev = { navController.navigate(Dev) })
        }
        composable<Dev> { DevScreen() }
    }
}
