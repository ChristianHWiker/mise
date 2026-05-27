package io.github.chwi.recipecalculator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * App theme. Wraps Material 3 with the editorial palette and type scale, and provides the
 * extended tokens (rule/muted/accentSoft, the named type styles, spacing) through
 * [RecipeTheme]. Dark mode follows the system; no dynamic color — the brand palette is fixed.
 */
@Composable
fun RecipeCalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val recipeColors = if (darkTheme) DarkRecipeColors else LightRecipeColors

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
