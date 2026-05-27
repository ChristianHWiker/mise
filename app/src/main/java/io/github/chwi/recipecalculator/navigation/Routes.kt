package io.github.chwi.recipecalculator.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes. Each destination is a `@Serializable` type that Navigation Compose
 * encodes into the back stack, so navigation calls carry real arguments instead of string paths.
 */

/** Nested graph hosting the recipe flow; the start destination of the app. */
@Serializable
data object RecipesGraph

@Serializable
data object RecipesList

@Serializable
data class RecipeDetail(val recipeId: Long)

/** Editor for a new recipe ([recipeId] null) or an existing one. */
@Serializable
data class RecipeEditor(val recipeId: Long? = null)

/** Camera/OCR capture flow — stubbed in Phase 00, built out in the capture phase. */
@Serializable
data object Capture

@Serializable
data object Settings

/** Developer tools (insert/read-back harness). Reached from Settings. */
@Serializable
data object Dev
