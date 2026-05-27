package io.github.chwi.recipecalculator.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A recipe header. Ordered ingredients live in [IngredientEntity] and free-form tags in
 * [TagEntity], both keyed by [id]. Steps are a small ordered list stored inline via a
 * type converter rather than a separate table.
 */
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val titleShort: String? = null,
    val category: String,
    val timeMinutes: Int,
    val difficulty: String,
    val servings: Int,
    val yieldUnit: String,
    val steps: List<String>,
    val photoUri: String? = null,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastCookedAt: Long? = null,
)
