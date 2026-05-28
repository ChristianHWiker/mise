package io.github.chwi.recipecalculator.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import io.github.chwi.recipecalculator.ui.capture.CaptureScreen
import io.github.chwi.recipecalculator.ui.capture.CaptureViewModel
import io.github.chwi.recipecalculator.ui.capture.ConfirmCaptureScreen
import io.github.chwi.recipecalculator.ui.dev.DevScreen
import io.github.chwi.recipecalculator.ui.recipes.RecipeDetailScreen
import io.github.chwi.recipecalculator.ui.recipes.RecipeEditorScreen
import io.github.chwi.recipecalculator.ui.recipes.RecipesListScreen
import io.github.chwi.recipecalculator.ui.settings.SettingsScreen

/**
 * The single-activity navigation graph. The recipe flow (list → detail → editor) lives in one nested
 * graph; the OCR capture flow lives in its own nested graph so [CaptureScreen] and
 * [ConfirmCaptureScreen] can share a [CaptureViewModel] scoped to that graph's back-stack entry.
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
                    onCaptureRecipe = { navController.navigate(CaptureGraph) },
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
                            // New recipe (incl. from-capture): replace the editor entry so back
                            // from Detail returns to the list, not to the editor.
                            navController.popBackStack()
                            navController.navigate(RecipeDetail(id))
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onDeleted = {
                        navController.popBackStack(RecipesList, inclusive = false)
                    },
                    onCancel = { navController.popBackStack() },
                )
            }
        }

        // Capture flow: CaptureScreen → ConfirmCaptureScreen, sharing a graph-scoped VM so the
        // confirm step reads the parsed rows produced by capture without a route argument.
        navigation<CaptureGraph>(startDestination = Capture) {
            composable<Capture> { entry ->
                val parentEntry = remember(entry) { navController.getBackStackEntry(CaptureGraph) }
                val viewModel: CaptureViewModel = hiltViewModel(parentEntry)
                CaptureScreen(
                    onBack = { navController.popBackStack(RecipesList, inclusive = false) },
                    onParsed = { navController.navigate(ConfirmCapture) },
                    viewModel = viewModel,
                )
            }
            composable<ConfirmCapture> { entry ->
                val parentEntry = remember(entry) { navController.getBackStackEntry(CaptureGraph) }
                val viewModel: CaptureViewModel = hiltViewModel(parentEntry)
                ConfirmCaptureScreen(
                    onBack = { navController.popBackStack() },
                    onContinueToEditor = {
                        // Pop the whole capture graph so back from the editor goes to the list.
                        navController.popBackStack(RecipesList, inclusive = false)
                        navController.navigate(RecipeEditor(recipeId = null, fromCapture = true))
                    },
                    viewModel = viewModel,
                )
            }
        }

        composable<Settings> {
            SettingsScreen(onOpenDev = { navController.navigate(Dev) })
        }
        composable<Dev> { DevScreen() }
    }
}
