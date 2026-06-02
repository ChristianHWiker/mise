package io.github.chwi.recipecalculator

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.chwi.recipecalculator.core.security.AppBackgroundObserver
import io.github.chwi.recipecalculator.navigation.RecipeBottomBar
import io.github.chwi.recipecalculator.navigation.RecipeNavHost
import io.github.chwi.recipecalculator.navigation.RecipesList
import io.github.chwi.recipecalculator.navigation.Settings
import io.github.chwi.recipecalculator.ui.security.IntegrityWarningBanner
import io.github.chwi.recipecalculator.ui.security.LockGate
import io.github.chwi.recipecalculator.ui.security.LockGateViewModel
import io.github.chwi.recipecalculator.ui.theme.RecipeCalculatorTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    // Required by androidx.biometric.BiometricPrompt, which needs a FragmentManager. The play
    // flavor pulls in fragment-ktx but never instantiates a biometric prompt — see
    // [AppLockController.isAvailable] for the gate.
    @Inject lateinit var backgroundObserver: AppBackgroundObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecipeCalculatorTheme {
                // Resolve the LockGate VM once at the activity level so the background hook below
                // and the gate Composable share state (both go through Hilt's same retained VM).
                val lockGateVm: LockGateViewModel = hiltViewModel()
                LaunchedEffect(Unit) {
                    backgroundObserver.start(onBackgrounded = lockGateVm::onAppBackgrounded)
                }

                LockGate {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Soft-fail integrity warning slot. Shows only when the verdict is
                        // Untrusted; otherwise this Composable emits nothing.
                        IntegrityWarningBanner()

                        val navController = rememberNavController()
                        val backStackEntry by navController.currentBackStackEntryAsState()

                        // The bottom bar belongs to the top-level destinations only; pushed
                        // screens (recipe detail, editor, capture) present full-screen with
                        // their own back affordance.
                        val showBottomBar = backStackEntry?.destination?.let { destination ->
                            destination.hasRoute(RecipesList::class) ||
                                destination.hasRoute(Settings::class)
                        } ?: true

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = { if (showBottomBar) RecipeBottomBar(navController) },
                        ) { innerPadding ->
                            RecipeNavHost(
                                navController = navController,
                                modifier = Modifier.padding(innerPadding),
                            )
                        }
                    }
                }
            }
        }
    }
}
