package io.github.chwi.recipecalculator.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes. Each destination is a `@Serializable` type that Navigation Compose
 * encodes into the back stack, so navigation calls carry real arguments instead of string paths.
 */

/** Nested graph hosting the recipe flow; the start destination of the app. */
@Serializable
data object RecipesGraph

/** Nested graph hosting the OCR capture flow (Capture → ConfirmCapture). */
@Serializable
data object CaptureGraph

@Serializable
data object RecipesList

@Serializable
data class RecipeDetail(val recipeId: Long)

/**
 * Editor for a new recipe ([recipeId] null) or an existing one.
 *
 * When [fromCapture] is `true`, the editor consumes any pending [io.github.chwi.recipecalculator
 * .ui.capture.CaptureHandoff] payload at startup and seeds the draft from it.
 */
@Serializable
data class RecipeEditor(val recipeId: Long? = null, val fromCapture: Boolean = false)

/** OCR capture flow — the live preview / picker entry. */
@Serializable
data object Capture

/** Confirmation step that follows a successful OCR pass; reads from the capture-graph VM. */
@Serializable
data object ConfirmCapture

@Serializable
data object Settings

/** Developer tools (insert/read-back harness). Reached from Settings. */
@Serializable
data object Dev
