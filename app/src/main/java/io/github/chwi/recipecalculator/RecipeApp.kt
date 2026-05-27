package io.github.chwi.recipecalculator

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. [HiltAndroidApp] generates the Hilt dependency-injection
 * container and triggers code generation for all other `@AndroidEntryPoint` classes.
 */
@HiltAndroidApp
class RecipeApp : Application()
