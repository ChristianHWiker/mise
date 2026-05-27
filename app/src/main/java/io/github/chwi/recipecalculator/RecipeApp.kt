package io.github.chwi.recipecalculator

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.chwi.recipecalculator.data.seed.DatabaseSeeder
import javax.inject.Inject

/**
 * Application entry point. [HiltAndroidApp] generates the Hilt dependency-injection
 * container and triggers code generation for all other `@AndroidEntryPoint` classes.
 */
@HiltAndroidApp
class RecipeApp : Application() {

    @Inject lateinit var seeder: DatabaseSeeder

    override fun onCreate() {
        super.onCreate()
        seeder.seedIfEmpty()
    }
}
