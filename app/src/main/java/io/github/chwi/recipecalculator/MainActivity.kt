package io.github.chwi.recipecalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.chwi.recipecalculator.navigation.RecipeBottomBar
import io.github.chwi.recipecalculator.navigation.RecipeNavHost
import io.github.chwi.recipecalculator.navigation.RecipesList
import io.github.chwi.recipecalculator.navigation.Settings
import io.github.chwi.recipecalculator.ui.theme.RecipeCalculatorTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecipeCalculatorTheme {
                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()

                // The bottom bar belongs to the top-level destinations only; pushed screens
                // (recipe detail, editor, capture) present full-screen with their own back affordance.
                val showBottomBar = backStackEntry?.destination?.let { destination ->
                    destination.hasRoute(RecipesList::class) || destination.hasRoute(Settings::class)
                } ?: true

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { if (showBottomBar) RecipeBottomBar(navController) },
                ) { innerPadding ->
                    RecipeNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}
