package io.github.chwi.recipecalculator.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One ingredient line of a recipe.
 *
 * The quantity is stored as the exact rational [qtyNum] / [qtyDen] — the load-bearing data-model
 * decision that lets the app add and scale quantities without floating-point drift. Map to/from
 * [io.github.chwi.recipecalculator.core.rational.Rational] at the domain boundary.
 *
 * [gramsPerCup] is the ingredient density used for ingredient-aware unit conversion; null for
 * countable or volume-only items (eggs, extracts).
 */
@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipeId")],
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val position: Int,
    val name: String,
    val qtyNum: Int,
    val qtyDen: Int,
    val unit: String,
    val gramsPerCup: Int? = null,
    val modifier: String? = null,
)
