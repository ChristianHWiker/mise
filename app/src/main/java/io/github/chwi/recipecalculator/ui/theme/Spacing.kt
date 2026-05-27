package io.github.chwi.recipecalculator.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The editorial spacing scale: `4, 6, 8, 10, 12, 14, 18, 22, 28` dp. Spacing is theme-independent,
 * so this is a plain object rather than a [androidx.compose.runtime.CompositionLocal]. Access via
 * [RecipeTheme.spacing] for symmetry with colors and typography.
 */
object Spacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 6.dp
    val sm: Dp = 8.dp
    val md: Dp = 10.dp
    val lg: Dp = 12.dp
    val xl: Dp = 14.dp
    val xxl: Dp = 18.dp
    val xxxl: Dp = 22.dp
    val huge: Dp = 28.dp
}

/** Corner radii. Intentionally angular (2–3 dp) for the editorial, hairline-card feel. */
object Radii {
    val card: Dp = 3.dp
    val hairlineCard: Dp = 2.dp
}
