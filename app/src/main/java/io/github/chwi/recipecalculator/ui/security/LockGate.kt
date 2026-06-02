package io.github.chwi.recipecalculator.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chwi.recipecalculator.core.security.AppLockController
import io.github.chwi.recipecalculator.core.security.UnlockResult
import io.github.chwi.recipecalculator.ui.theme.RecipeTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Wraps [content] so the user sees a lock screen instead of app content whenever the lock is
 * armed (i.e. enabled in settings AND not yet satisfied this session). In the play flavor
 * `AppLockController.isLockEnabled` is always false so this wrapper is structurally a no-op —
 * the cost is one extra Composable layer with no allocations beyond a state read.
 */
@Composable
fun LockGate(content: @Composable () -> Unit) {
    val viewModel: LockGateViewModel = hiltViewModel()
    val activity = LocalContext.current as? FragmentActivity
    val locked by viewModel.isLocked.collectAsState()

    // Kick off the first unlock attempt automatically when arriving locked from a cold start.
    LaunchedEffect(locked, activity) {
        if (locked && activity != null) viewModel.tryUnlock(activity)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        if (locked) LockScreen(onUnlock = { activity?.let(viewModel::tryUnlock) })
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Recipe Calculator",
            style = RecipeTheme.typography.detailH1,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Locked",
            style = RecipeTheme.typography.body,
            color = RecipeTheme.colors.muted,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onUnlock) { Text("Unlock") }
    }
}

/**
 * State holder for [LockGate]. The lock is "armed" iff the controller reports
 * `isLockEnabled == true` and it hasn't been satisfied yet for the current session.
 * Backgrounding the app re-arms the lock via [onAppBackgrounded] (wired in MainActivity to
 * `ProcessLifecycleOwner` in the portfolio flavor).
 */
@HiltViewModel
class LockGateViewModel @Inject constructor(
    private val controller: AppLockController,
) : ViewModel() {

    // True between a successful unlock and the next backgrounding event. Cold-start defaults to
    // false, so the gate arms automatically as soon as the controller hydrates `isLockEnabled`.
    private val unlockedThisSession = MutableStateFlow(false)

    val isLocked: StateFlow<Boolean> = combine(
        controller.isLockEnabled,
        unlockedThisSession,
    ) { enabled, unlocked -> enabled && !unlocked }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = false)

    fun tryUnlock(activity: FragmentActivity) {
        if (!isLocked.value) return
        viewModelScope.launch {
            when (controller.requestUnlock(activity)) {
                UnlockResult.Success -> unlockedThisSession.value = true
                UnlockResult.Cancelled, is UnlockResult.Error -> {
                    // Stay locked; the user can tap "Unlock" on the lock screen to retry.
                }
            }
        }
    }

    /** Called by MainActivity when the process moves to the background — re-arm for next foreground. */
    fun onAppBackgrounded() {
        unlockedThisSession.value = false
    }
}
