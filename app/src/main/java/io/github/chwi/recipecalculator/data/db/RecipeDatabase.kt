package io.github.chwi.recipecalculator.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.chwi.recipecalculator.data.model.IngredientEntity
import io.github.chwi.recipecalculator.data.model.RecipeEntity
import io.github.chwi.recipecalculator.data.model.TagEntity

/**
 * Room database. Schemas are exported to `app/schemas` (configured via the KSP `room.schemaLocation`
 * argument) so migrations can be verified by instrumented tests as the version grows.
 *
 * Version history:
 *  - v1: initial schema — recipes, ingredients, tags.
 */
@Database(
    entities = [RecipeEntity::class, IngredientEntity::class, TagEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao

    companion object {
        const val NAME = "recipe.db"
    }
}
