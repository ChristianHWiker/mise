package io.github.chwi.recipecalculator.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.chwi.recipecalculator.core.security.IntegrityChecker
import io.github.chwi.recipecalculator.core.security.IntegrityVerdict
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hosts the Phase 4 Integrity verdict for the lifetime of the activity. Runs the check once on
 * first composition; the banner subscribes to the resulting [verdict] and renders only when
 * non-trusted. In play, [IntegrityChecker] is the no-op that returns [IntegrityVerdict.Trusted]
 * immediately so the banner never appears.
 */
@HiltViewModel
class IntegrityViewModel @Inject constructor(
    private val checker: IntegrityChecker,
) : ViewModel() {

    private val _verdict = MutableStateFlow<IntegrityVerdict>(IntegrityVerdict.Trusted)
    val verdict: StateFlow<IntegrityVerdict> = _verdict.asStateFlow()

    init {
        viewModelScope.launch { _verdict.value = checker.verify() }
    }
}

/**
 * Non-blocking warning shown if Play Integrity comes back with a definitively-untrusted verdict.
 * In this build "untrusted" is structurally unreachable (no backend to decrypt the token) — the
 * banner is wired so that if [IntegrityChecker] is ever upgraded to server-side verification,
 * the UI is already in place.
 */
@Composable
fun IntegrityWarningBanner(modifier: Modifier = Modifier) {
    val viewModel: IntegrityViewModel = hiltViewModel()
    val verdict by viewModel.verdict.collectAsState()

    // Subtle warm pill for now; deliberately not a blocking dialog (soft-fail).
    if (verdict is IntegrityVerdict.Untrusted) {
        val reason = (verdict as IntegrityVerdict.Untrusted).reason
        LaunchedEffect(reason) { /* hook point for analytics; intentionally empty */ }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFFFDF3E7))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Text(
                text = "Device integrity check failed — some features may be unavailable.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7C3F00),
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}
