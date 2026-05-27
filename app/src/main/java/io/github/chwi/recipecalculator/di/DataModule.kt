package io.github.chwi.recipecalculator.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.chwi.recipecalculator.data.db.RecipeDao
import io.github.chwi.recipecalculator.data.db.RecipeDatabase
import io.github.chwi.recipecalculator.data.repository.RecipeRepository
import io.github.chwi.recipecalculator.data.repository.RecipeRepositoryImpl
import javax.inject.Singleton

/** Provides the Room database and DAO. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RecipeDatabase =
        Room.databaseBuilder(context, RecipeDatabase::class.java, RecipeDatabase.NAME).build()

    @Provides
    fun provideRecipeDao(database: RecipeDatabase): RecipeDao = database.recipeDao()
}

/** Binds repository interfaces to their implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindRecipeRepository(impl: RecipeRepositoryImpl): RecipeRepository
}
