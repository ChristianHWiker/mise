package io.github.chwi.recipecalculator.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.chwi.recipecalculator.R

// ── Font families (Downloadable Fonts via Google Play Services) ──────────────
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val SourceSerif4 = GoogleFont("Source Serif 4")
private val Geist = GoogleFont("Geist")

/** Serif family — titles, quantities, step numbers (the editorial voice). */
val SerifFamily = FontFamily(
    Font(googleFont = SourceSerif4, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = SourceSerif4, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = SourceSerif4, fontProvider = provider, weight = FontWeight.Normal, style = FontStyle.Italic),
)

/** Sans family — body, UI labels, captions. */
val SansFamily = FontFamily(
    Font(googleFont = Geist, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = Geist, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = Geist, fontProvider = provider, weight = FontWeight.SemiBold),
)

private const val TNUM = "tnum" // tabular figures keep numeric columns aligned

/**
 * The editorial type scale, named per the design handoff. Exposed via [LocalRecipeTypography] /
 * [RecipeTheme] so screens use the exact tokens (`RecipeTheme.typography.mastheadH1`) rather than
 * approximating with Material's semantic slots.
 */
@Immutable
data class RecipeTypography(
    val mastheadH1: TextStyle,
    val detailH1: TextStyle,
    val sectionH2: TextStyle,
    val sheetTitle: TextStyle,
    val stepperValue: TextStyle,
    val ingredientQty: TextStyle,
    val stepNumber: TextStyle,
    val body: TextStyle,
    val caption: TextStyle,
    val kicker: TextStyle,
)

val DefaultRecipeTypography = RecipeTypography(
    mastheadH1 = TextStyle(
        fontFamily = SerifFamily, fontWeight = FontWeight.Normal,
        fontSize = 40.sp, lineHeight = 39.2.sp, letterSpacing = (-0.02).em,
    ),
    detailH1 = TextStyle(
        fontFamily = SerifFamily, fontWeight = FontWeight.Normal,
        fontSize = 30.sp, lineHeight = 33.sp, letterSpacing = (-0.012).em,
    ),
    sectionH2 = TextStyle(
        fontFamily = SerifFamily, fontWeight = FontWeight.Normal,
        fontSize = 18.sp, lineHeight = 21.6.sp,
    ),
    sheetTitle = TextStyle(
        fontFamily = SerifFamily, fontWeight = FontWeight.Normal,
        fontSize = 22.sp, lineHeight = 26.4.sp, letterSpacing = (-0.01).em,
    ),
    stepperValue = TextStyle(
        fontFamily = SerifFamily, fontWeight = FontWeight.Normal,
        fontSize = 38.sp, lineHeight = 38.sp, fontFeatureSettings = TNUM,
    ),
    ingredientQty = TextStyle(
        fontFamily = SerifFamily, fontWeight = FontWeight.Normal,
        fontSize = 19.sp, lineHeight = 19.sp, fontFeatureSettings = TNUM,
    ),
    stepNumber = TextStyle(
        fontFamily = SerifFamily, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Italic,
        fontSize = 24.sp, lineHeight = 24.sp,
    ),
    body = TextStyle(
        fontFamily = SansFamily, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 21.sp,
    ),
    caption = TextStyle(
        fontFamily = SansFamily, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.8.sp,
    ),
    kicker = TextStyle(
        fontFamily = SansFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp, lineHeight = 11.sp, letterSpacing = 0.14.em,
    ),
)

val LocalRecipeTypography = staticCompositionLocalOf { DefaultRecipeTypography }

/**
 * Material 3 [Typography] baseline so stock components inherit the editorial fonts: serif for the
 * larger display/headline/title slots, Geist for body and labels.
 */
val AppTypography = Typography(
    displayLarge = DefaultRecipeTypography.mastheadH1,
    headlineLarge = DefaultRecipeTypography.detailH1,
    headlineMedium = DefaultRecipeTypography.detailH1,
    titleLarge = DefaultRecipeTypography.sectionH2,
    titleMedium = DefaultRecipeTypography.sectionH2,
    bodyLarge = DefaultRecipeTypography.body,
    bodyMedium = DefaultRecipeTypography.body,
    bodySmall = DefaultRecipeTypography.caption,
    labelLarge = DefaultRecipeTypography.body.copy(fontWeight = FontWeight.Medium),
    labelMedium = DefaultRecipeTypography.caption,
    labelSmall = DefaultRecipeTypography.kicker,
)
