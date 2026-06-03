package io.github.chwi.recipecalculator.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chwi.recipecalculator.core.rational.FractionStyle
import io.github.chwi.recipecalculator.core.theme.AccentTheme
import io.github.chwi.recipecalculator.core.theme.ThemeMode
import io.github.chwi.recipecalculator.core.units.UnitSystem
import io.github.chwi.recipecalculator.ui.theme.RecipeTheme
import io.github.chwi.recipecalculator.ui.theme.swatch

private val PagePadding = 22.dp

/**
 * Settings. Surfaces the two display preferences from the design handoff — fraction style and unit
 * system — persisted via DataStore and applied live across Home and Detail. Dark mode follows the
 * system. Also hosts the entry point to the developer tools.
 */
@Composable
fun SettingsScreen(
    onOpenDev: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val biometricLockEnabled by viewModel.biometricLockEnabled.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PagePadding)
            .padding(top = RecipeTheme.spacing.huge, bottom = RecipeTheme.spacing.huge),
    ) {
        Text(
            text = "SETTINGS",
            style = RecipeTheme.typography.kicker,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(RecipeTheme.spacing.md))
        Text(
            text = "Settings",
            style = RecipeTheme.typography.detailH1,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(RecipeTheme.spacing.huge))

        // Resolve what light/dark the swatches should preview, mirroring the theme's own logic.
        val previewDark = when (settings.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

        SettingGroup(label = "Theme") {
            ThemeMode.entries.forEach { mode ->
                OptionRow(
                    label = mode.displayName(),
                    selected = settings.themeMode == mode,
                    onClick = { viewModel.setThemeMode(mode) },
                )
            }
        }

        Spacer(Modifier.height(RecipeTheme.spacing.huge))

        SettingGroup(label = "Accent") {
            AccentTheme.entries.forEach { accent ->
                AccentRow(
                    label = accent.displayName(),
                    swatch = accent.swatch(previewDark),
                    selected = settings.accent == accent,
                    onClick = { viewModel.setAccent(accent) },
                )
            }
        }

        Spacer(Modifier.height(RecipeTheme.spacing.huge))

        SettingGroup(label = "Fraction style") {
            FractionStyle.entries.forEach { style ->
                OptionRow(
                    label = style.displayName(),
                    selected = settings.fractionStyle == style,
                    onClick = { viewModel.setFractionStyle(style) },
                )
            }
        }

        Spacer(Modifier.height(RecipeTheme.spacing.huge))

        SettingGroup(label = "Unit system") {
            UnitSystem.entries.forEach { system ->
                OptionRow(
                    label = system.displayName(),
                    selected = settings.unitSystem == system,
                    onClick = { viewModel.setUnitSystem(system) },
                )
            }
        }

        // Security group is only present in the portfolio flavor — in play, biometricLockAvailable
        // is hardwired to false by NoOpAppLockController and the whole section collapses out.
        if (viewModel.biometricLockAvailable) {
            Spacer(Modifier.height(RecipeTheme.spacing.huge))
            SettingGroup(label = "Security") {
                ToggleRow(
                    label = "Lock app with biometrics",
                    checked = biometricLockEnabled,
                    onCheckedChange = viewModel::setBiometricLockEnabled,
                )
            }
        }

        Spacer(Modifier.height(RecipeTheme.spacing.huge))

        TextButton(onClick = onOpenDev) {
            Text("Developer tools", style = RecipeTheme.typography.body, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SettingGroup(label: String, content: @Composable () -> Unit) {
    Text(
        text = label.uppercase(),
        style = RecipeTheme.typography.kicker,
        color = RecipeTheme.colors.muted,
    )
    Spacer(Modifier.height(RecipeTheme.spacing.sm))
    HorizontalDivider(color = RecipeTheme.colors.rule)
    content()
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = RecipeTheme.spacing.xl),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = RecipeTheme.typography.body,
            color = if (selected) MaterialTheme.colorScheme.onBackground else RecipeTheme.colors.muted,
        )
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    HorizontalDivider(color = RecipeTheme.colors.rule)
}

@Composable
private fun AccentRow(
    label: String,
    swatch: androidx.compose.ui.graphics.Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = RecipeTheme.spacing.xl),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(16.dp)
                    .background(swatch, CircleShape)
                    .border(1.dp, RecipeTheme.colors.rule, CircleShape),
            )
            Spacer(Modifier.width(RecipeTheme.spacing.md))
            Text(
                text = label,
                style = RecipeTheme.typography.body,
                color = if (selected) MaterialTheme.colorScheme.onBackground else RecipeTheme.colors.muted,
            )
        }
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    HorizontalDivider(color = RecipeTheme.colors.rule)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = RecipeTheme.spacing.xl),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = RecipeTheme.typography.body,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
    HorizontalDivider(color = RecipeTheme.colors.rule)
}

private fun FractionStyle.displayName(): String = when (this) {
    FractionStyle.INLINE -> "Inline  ·  1 1/2"
    FractionStyle.STACKED -> "Stacked  ·  1½"
    FractionStyle.DECIMAL -> "Decimal  ·  1.5"
}

private fun UnitSystem.displayName(): String = when (this) {
    UnitSystem.US -> "US  ·  cups, tsp, tbsp"
    UnitSystem.METRIC -> "Metric  ·  grams"
}

private fun ThemeMode.displayName(): String = when (this) {
    ThemeMode.SYSTEM -> "Follow system"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun AccentTheme.displayName(): String = when (this) {
    AccentTheme.TERRACOTTA -> "Terracotta"
    AccentTheme.SAGE -> "Sage"
    AccentTheme.PLUM -> "Plum"
    AccentTheme.SLATE -> "Slate"
}
