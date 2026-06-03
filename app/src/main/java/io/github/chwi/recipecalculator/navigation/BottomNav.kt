package io.github.chwi.recipecalculator.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.chwi.recipecalculator.ui.theme.RecipeTheme
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlin.reflect.KClass

/** A bottom-navigation tab: its start route, label, and icon. */
data class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
)

val TopLevelDestinations = listOf(
    TopLevelDestination(RecipesGraph, RecipesGraph::class, "Recipes", Icons.AutoMirrored.Filled.List),
    TopLevelDestination(Settings, Settings::class, "Settings", Icons.Filled.Settings),
)

@Composable
fun RecipeBottomBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Pin the bar to the editorial background and brand the items. Without an explicit
    // containerColor, M3's NavigationBar uses `surfaceContainer` — a slot this palette doesn't
    // define, so it fell back to M3's lavender-grey default and looked like a foreign component.
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        TopLevelDestinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any {
                it.hasRoute(destination.routeClass)
            } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        // Keep a single copy per tab and preserve each tab's state.
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = RecipeTheme.colors.muted,
                    unselectedTextColor = RecipeTheme.colors.muted,
                ),
            )
        }
    }
}
