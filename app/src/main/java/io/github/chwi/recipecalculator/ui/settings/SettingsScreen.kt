package io.github.chwi.recipecalculator.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.chwi.recipecalculator.ui.theme.RecipeTheme

/**
 * Settings. Phase 00 placeholder; will expose fraction style and unit system (per the handoff)
 * in a later phase. Includes an entry point to the developer tools.
 */
@Composable
fun SettingsScreen(
    onOpenDev: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(RecipeTheme.spacing.huge),
        verticalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.lg),
    ) {
        Text(
            text = "SETTINGS",
            style = RecipeTheme.typography.kicker,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Settings",
            style = RecipeTheme.typography.detailH1,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Fraction style and unit system will live here.",
            style = RecipeTheme.typography.body,
            color = RecipeTheme.colors.muted,
        )
        OutlinedButton(onClick = onOpenDev) {
            Text("Developer tools")
        }
    }
}
