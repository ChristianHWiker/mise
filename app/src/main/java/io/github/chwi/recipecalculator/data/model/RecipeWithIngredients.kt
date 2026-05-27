package io.github.chwi.recipecalculator.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * A recipe with its ingredient lines and tags. Room populates the [Relation] lists; ingredient
 * ordering is not guaranteed by the relation, so use [orderedIngredients].
 */
data class RecipeWithIngredients(
    @Embedded val recipe: RecipeEntity,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<IngredientEntity>,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val tags: List<TagEntity>,
) {
    val orderedIngredients: List<IngredientEntity>
        get() = ingredients.sortedBy { it.position }
}
