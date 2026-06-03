package io.github.chwi.recipecalculator.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.chwi.recipecalculator.core.theme.AccentTheme

// ── Editorial palette (verbatim from the design handoff) ─────────────────────
// The neutrals — cream background, ink, muted ink levels, hairline rules — are constant across
// every accent. Only the accent and its soft tint swap (see [AccentSpec] below).
// Light
private val LightBg = Color(0xFFFAF6EC)
private val LightSurface = Color(0xFFFDFBF3)
private val LightText = Color(0xFF1F1A14)
private val LightMuted = Color(0xFF7A6F5D)
private val LightMutedSoft = Color(0xFFB4A895)
private val LightRule = Color(0xFFE5DCC7)

// Dark
private val DarkBg = Color(0xFF181410)
private val DarkSurface = Color(0xFF221C15)
private val DarkText = Color(0xFFF1EAD8)
private val DarkMuted = Color(0xFFA39780)
private val DarkMutedSoft = Color(0xFF6E6451)
private val DarkRule = Color(0xFF322A1F)

// ── Accent palettes ──────────────────────────────────────────────────────────
// Each [AccentTheme] maps to four colours: the accent and its soft surface tint, in light and dark.
// The originally-shipped accent was a warm brown (#A04830) that read as muddy on cream; the
// terracotta entry here is the cleaner burnt-orange tuning that replaced it.
@Immutable
data class AccentSpec(
    val lightAccent: Color,
    val lightAccentSoft: Color,
    val darkAccent: Color,
    val darkAccentSoft: Color,
)

private val Terracotta = AccentSpec(
    lightAccent = Color(0xFFC2410C),
    lightAccentSoft = Color(0xFFF4DCCB),
    darkAccent = Color(0xFFF0936A),
    darkAccentSoft = Color(0xFF3A2418),
)
private val Sage = AccentSpec(
    lightAccent = Color(0xFF3F6B4E),
    lightAccentSoft = Color(0xFFDCE8DE),
    darkAccent = Color(0xFF8FBF9F),
    darkAccentSoft = Color(0xFF1E2C22),
)
private val Plum = AccentSpec(
    lightAccent = Color(0xFF7B3550),
    lightAccentSoft = Color(0xFFEAD7DF),
    darkAccent = Color(0xFFD58AA6),
    darkAccentSoft = Color(0xFF2E1B23),
)
private val Slate = AccentSpec(
    lightAccent = Color(0xFF3A5A78),
    lightAccentSoft = Color(0xFFD6E0EA),
    darkAccent = Color(0xFF8FB3D9),
    darkAccentSoft = Color(0xFF1A2530),
)

fun AccentTheme.spec(): AccentSpec = when (this) {
    AccentTheme.TERRACOTTA -> Terracotta
    AccentTheme.SAGE -> Sage
    AccentTheme.PLUM -> Plum
    AccentTheme.SLATE -> Slate
}

/** The accent colour as rendered in [dark] vs. light — for swatches in the settings picker. */
fun AccentTheme.swatch(dark: Boolean): Color = spec().let { if (dark) it.darkAccent else it.lightAccent }

/**
 * Editorial tokens that have no natural slot in Material 3's [ColorScheme] (hairline rules, the two
 * muted ink levels, the soft-accent surface). Exposed through [LocalRecipeColors] / [RecipeTheme]
 * so screens can reference the handoff names directly.
 */
@Immutable
data class RecipeColors(
    val muted: Color,
    val mutedSoft: Color,
    val rule: Color,
    val accentSoft: Color,
)

private val LightRecipeColorsBase = RecipeColors(
    muted = LightMuted,
    mutedSoft = LightMutedSoft,
    rule = LightRule,
    accentSoft = Sage.lightAccentSoft,
)

private val DarkRecipeColorsBase = RecipeColors(
    muted = DarkMuted,
    mutedSoft = DarkMutedSoft,
    rule = DarkRule,
    accentSoft = Sage.darkAccentSoft,
)

/** Default for the composition local before a theme is applied (matches the default accent). */
val LocalRecipeColors = staticCompositionLocalOf { LightRecipeColorsBase }

fun lightRecipeColorsFor(accent: AccentTheme): RecipeColors =
    LightRecipeColorsBase.copy(accentSoft = accent.spec().lightAccentSoft)

fun darkRecipeColorsFor(accent: AccentTheme): RecipeColors =
    DarkRecipeColorsBase.copy(accentSoft = accent.spec().darkAccentSoft)

// ── Material 3 color schemes ─────────────────────────────────────────────────
// The editorial palette mapped onto Material slots so stock M3 components inherit it. The
// surfaceContainer* family is set explicitly to the cream surface — left unset, M3 derives its own
// tonal (lavender-grey) defaults, which is what made the NavigationBar and dropdown menus look
// like they belonged to a different app.
fun lightColorSchemeFor(accent: AccentTheme): ColorScheme {
    val a = accent.spec()
    return lightColorScheme(
        primary = a.lightAccent,
        onPrimary = LightSurface,
        primaryContainer = a.lightAccentSoft,
        onPrimaryContainer = LightText,
        secondary = LightMuted,
        onSecondary = LightSurface,
        tertiary = a.lightAccent,
        onTertiary = LightSurface,
        background = LightBg,
        onBackground = LightText,
        surface = LightSurface,
        onSurface = LightText,
        surfaceVariant = LightSurface,
        onSurfaceVariant = LightMuted,
        surfaceContainerLowest = LightBg,
        surfaceContainerLow = LightSurface,
        surfaceContainer = LightSurface,
        surfaceContainerHigh = LightSurface,
        surfaceContainerHighest = LightSurface,
        outline = LightRule,
        outlineVariant = LightRule,
        scrim = Color(0x66140C04), // rgba(20,12,4,0.4) sheet backdrop
    )
}

fun darkColorSchemeFor(accent: AccentTheme): ColorScheme {
    val a = accent.spec()
    return darkColorScheme(
        primary = a.darkAccent,
        onPrimary = DarkBg,
        primaryContainer = a.darkAccentSoft,
        onPrimaryContainer = DarkText,
        secondary = DarkMuted,
        onSecondary = DarkBg,
        tertiary = a.darkAccent,
        onTertiary = DarkBg,
        background = DarkBg,
        onBackground = DarkText,
        surface = DarkSurface,
        onSurface = DarkText,
        surfaceVariant = DarkSurface,
        onSurfaceVariant = DarkMuted,
        surfaceContainerLowest = DarkBg,
        surfaceContainerLow = DarkSurface,
        surfaceContainer = DarkSurface,
        surfaceContainerHigh = DarkSurface,
        surfaceContainerHighest = DarkSurface,
        outline = DarkRule,
        outlineVariant = DarkRule,
        scrim = Color(0x66000000),
    )
}
