package io.github.chwi.recipecalculator.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Editorial palette (verbatim from the design handoff) ─────────────────────
// Light
private val LightBg = Color(0xFFFAF6EC)
private val LightSurface = Color(0xFFFDFBF3)
private val LightText = Color(0xFF1F1A14)
private val LightMuted = Color(0xFF7A6F5D)
private val LightMutedSoft = Color(0xFFB4A895)
private val LightRule = Color(0xFFE5DCC7)
private val LightAccent = Color(0xFFA04830)
private val LightAccentSoft = Color(0xFFF1DCCC)

// Dark
private val DarkBg = Color(0xFF181410)
private val DarkSurface = Color(0xFF221C15)
private val DarkText = Color(0xFFF1EAD8)
private val DarkMuted = Color(0xFFA39780)
private val DarkMutedSoft = Color(0xFF6E6451)
private val DarkRule = Color(0xFF322A1F)
private val DarkAccent = Color(0xFFDC8462)
private val DarkAccentSoft = Color(0xFF3A2418)

/**
 * Editorial tokens that have no natural slot in Material 3's [androidx.compose.material3.ColorScheme]
 * (hairline rules, the two muted ink levels, the soft-accent surface). Exposed through
 * [LocalRecipeColors] / [RecipeTheme] so screens can reference the handoff names directly.
 */
@Immutable
data class RecipeColors(
    val muted: Color,
    val mutedSoft: Color,
    val rule: Color,
    val accentSoft: Color,
)

val LightRecipeColors = RecipeColors(
    muted = LightMuted,
    mutedSoft = LightMutedSoft,
    rule = LightRule,
    accentSoft = LightAccentSoft,
)

val DarkRecipeColors = RecipeColors(
    muted = DarkMuted,
    mutedSoft = DarkMutedSoft,
    rule = DarkRule,
    accentSoft = DarkAccentSoft,
)

val LocalRecipeColors = staticCompositionLocalOf { LightRecipeColors }

// ── Material 3 color schemes ─────────────────────────────────────────────────
// The editorial palette mapped onto Material slots so stock M3 components inherit it.
val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightSurface,
    primaryContainer = LightAccentSoft,
    onPrimaryContainer = LightText,
    secondary = LightMuted,
    onSecondary = LightSurface,
    tertiary = LightAccent,
    onTertiary = LightSurface,
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurface,
    onSurfaceVariant = LightMuted,
    outline = LightRule,
    outlineVariant = LightRule,
    scrim = Color(0x66140C04), // rgba(20,12,4,0.4) sheet backdrop
)

val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkBg,
    primaryContainer = DarkAccentSoft,
    onPrimaryContainer = DarkText,
    secondary = DarkMuted,
    onSecondary = DarkBg,
    tertiary = DarkAccent,
    onTertiary = DarkBg,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkMuted,
    outline = DarkRule,
    outlineVariant = DarkRule,
    scrim = Color(0x66000000),
)
