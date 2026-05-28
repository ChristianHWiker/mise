package io.github.chwi.recipecalculator.ui.capture

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.chwi.recipecalculator.core.parser.ParsedLine
import io.github.chwi.recipecalculator.core.parser.parseIngredientBlock
import io.github.chwi.recipecalculator.core.parser.refineForIngredients
import io.github.chwi.recipecalculator.data.ocr.OcrService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Where the user is in the capture flow. */
sealed interface CaptureStage {
    /** Start screen: choose camera or gallery. */
    data object Idle : CaptureStage
    /** Live CameraX preview waiting for a shutter tap. */
    data object LivePreview : CaptureStage
    /** OCR is running on the captured/selected image. */
    data object Recognizing : CaptureStage
    /** OCR + parse finished; rows ready for confirmation. */
    data class Confirm(val rows: List<ParsedLine>) : CaptureStage
    /** Something went wrong — message shown, user can retry. */
    data class Error(val message: String) : CaptureStage
}

data class CaptureUiState(
    val stage: CaptureStage = CaptureStage.Idle,
    /** Last image we ran OCR on; kept so the confirm screen can offer a "re-scan" affordance later. */
    val sourceUri: String? = null,
)

/**
 * Owns the OCR pipeline state for the capture/confirmation flow. Scoped to a nav-graph (the capture
 * graph) so [CaptureScreen] and [ConfirmCaptureScreen] share the same instance — the confirm screen
 * reads parsed rows produced by the capture step without a route argument.
 */
@HiltViewModel
class CaptureViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocr: OcrService,
    private val handoff: CaptureHandoff,
) : ViewModel() {

    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state.asStateFlow()

    fun openCamera() = _state.update { it.copy(stage = CaptureStage.LivePreview) }

    fun cancelCamera() = _state.update { it.copy(stage = CaptureStage.Idle) }

    /** Called by the screen once CameraX writes a JPEG to disk, or the picker hands back a URI. */
    fun onImageReady(uri: Uri) {
        _state.update { it.copy(stage = CaptureStage.Recognizing, sourceUri = uri.toString()) }
        viewModelScope.launch {
            try {
                val result = ocr.recognizeText(uri)
                val rawParsed = parseIngredientBlock(result.lines.joinToString("\n"))
                val parsed = refineForIngredients(rawParsed)
                Log.d(TAG, "── parser raw (${rawParsed.size} rows) → refined (${parsed.size} rows) ──")
                parsed.forEachIndexed { idx, p ->
                    Log.d(
                        TAG,
                        "  [$idx] qty=${p.qty} unit=${p.unit} name='${p.name}' " +
                            "mod='${p.modifier ?: ""}' conf=${p.confidence} raw='${p.rawText}'",
                    )
                }
                _state.update {
                    it.copy(
                        stage = if (parsed.isEmpty()) {
                            CaptureStage.Error("No text recognized — try again with a clearer photo.")
                        } else {
                            CaptureStage.Confirm(parsed)
                        },
                    )
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(stage = CaptureStage.Error(t.message ?: "Could not read the image."))
                }
            }
        }
    }

    /** Replace a row when the user edits it on the confirm screen. */
    fun updateRow(index: Int, transform: (ParsedLine) -> ParsedLine) {
        val current = _state.value.stage as? CaptureStage.Confirm ?: return
        val next = current.rows.toMutableList().also {
            if (index in it.indices) it[index] = transform(it[index])
        }
        _state.update { it.copy(stage = current.copy(rows = next)) }
    }

    fun removeRow(index: Int) {
        val current = _state.value.stage as? CaptureStage.Confirm ?: return
        val next = current.rows.toMutableList().also {
            if (index in it.indices) it.removeAt(index)
        }
        _state.update { it.copy(stage = current.copy(rows = next)) }
    }

    /** Stage the (possibly user-edited) rows for the editor to consume on its next open. */
    fun stageForEditor() {
        val rows = (_state.value.stage as? CaptureStage.Confirm)?.rows ?: return
        handoff.stage(rows)
    }

    fun reset() {
        _state.value = CaptureUiState()
    }

    /**
     * Allocate a destination file under `filesDir/ocr_captures/` for a CameraX capture. The
     * directory is created lazily; the file is returned so the screen can hand it to ImageCapture
     * and later read it back as a URI.
     */
    fun newCaptureFile(): File {
        val dir = File(context.filesDir, "ocr_captures").apply { mkdirs() }
        return File(dir, "capture-${System.currentTimeMillis()}.jpg")
    }

    private companion object {
        const val TAG = "RecipeOcr"
    }
}
