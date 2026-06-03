package io.github.chwi.recipecalculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import io.github.chwi.recipecalculator.core.theme.AccentTheme
import io.github.chwi.recipecalculator.core.theme.ThemeMode

/**
 * App theme. Wraps Material 3 with the editorial palette and type scale, and provides the
 * extended tokens (rule/muted/accentSoft, the named type styles, spacing) through [RecipeTheme].
 *
 * Light vs. dark resolves from [themeMode] (SYSTEM defers to the OS, as the app originally did);
 * the brand [accent] selects one of the curated palettes. No dynamic color — the palette is the
 * brand. Both values are user preferences (Settings → Appearance), fed in at the top of the tree.
 */
@Composable
fun RecipeCalculatorTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentTheme = AccentTheme.SAGE,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) darkColorSchemeFor(accent) else lightColorSchemeFor(accent)
    val recipeColors = if (darkTheme) darkRecipeColorsFor(accent) else lightRecipeColorsFor(accent)

    CompositionLocalProvider(
        LocalRecipeColors provides recipeColors,
        LocalRecipeTypography provides DefaultRecipeTypography,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

/**
 * Accessor for the editorial design tokens that extend Material 3. Use alongside [MaterialTheme]:
 * `MaterialTheme.colorScheme.primary` for standard slots, `RecipeTheme.colors.rule` for the rest.
 */
object RecipeTheme {
    val colors: RecipeColors
        @Composable @ReadOnlyComposable get() = LocalRecipeColors.current

    val typography: RecipeTypography
        @Composable @ReadOnlyComposable get() = LocalRecipeTypography.current

    val spacing: Spacing get() = Spacing

    @Suppress("MemberVisibilityCanBePrivate")
    val radii: Radii get() = Radii
}
