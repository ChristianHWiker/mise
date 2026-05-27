package io.github.chwi.recipecalculator.ui.capture

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.chwi.recipecalculator.ui.common.PlaceholderScreen

/** Camera + on-device OCR capture flow. Route stub for Phase 00. */
@Composable
fun CaptureScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        kicker = "Capture",
        title = "Scan a recipe",
        note = "CameraX + ML Kit text recognition will live here.",
        modifier = modifier,
    )
}
