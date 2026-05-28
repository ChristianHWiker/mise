package io.github.chwi.recipecalculator.data.ocr

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device OCR over an image URI. Wraps ML Kit Text Recognition behind a coroutine-friendly
 * interface so callers (the capture ViewModel) don't pull ML Kit types up the stack.
 *
 * Runs entirely on-device — no network, no privacy story to defend.
 */
interface OcrService {
    /** Recognize text in [image] (content:// or file:// URI). Returns lines in reading order. */
    suspend fun recognizeText(image: Uri): OcrResult
}

data class OcrResult(
    val lines: List<String>,
    /** Mean ML Kit element confidence across all recognized text. 0..1; 0 when nothing is recognized. */
    val confidence: Float,
)

/** ML Kit Latin text recognizer; sufficient for recipes in EU/NA languages. */
@Singleton
class MlKitOcrService @Inject constructor(
    @ApplicationContext private val context: Context,
) : OcrService {

    private val recognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognizeText(image: Uri): OcrResult = withContext(Dispatchers.Default) {
        val input = InputImage.fromFilePath(context, image)
        val visionText = suspendCancellableCoroutine { cont ->
            recognizer.process(input)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

        // Each Block contains Lines in reading order — flatten and keep their text verbatim.
        val lines = visionText.textBlocks.flatMap { block -> block.lines.map { it.text } }

        // Phase-3 debugging: dump everything ML Kit returned so we can see exactly what shape the
        // parser receives on real photos. Strip once the parser is tuned to actual OCR output.
        Log.d(TAG, "── raw OCR (${visionText.textBlocks.size} blocks, ${lines.size} lines) ──")
        visionText.textBlocks.forEachIndexed { bIdx, block ->
            Log.d(TAG, "  block[$bIdx]:")
            block.lines.forEachIndexed { lIdx, line ->
                Log.d(TAG, "    line[$lIdx]: ${line.text}")
            }
        }
        Log.d(TAG, "── full text ──\n${visionText.text}")

        // ML Kit doesn't surface per-element confidence on the public API in stable releases,
        // so we approximate: lines that look ingredient-shaped (digits + a letter) get full
        // credit, others half. The parser does the heavy lifting downstream anyway.
        val signal = if (lines.isEmpty()) 0f else {
            val shaped = lines.count { it.any(Char::isDigit) && it.any(Char::isLetter) }
            (shaped.toFloat() / lines.size).coerceIn(0f, 1f)
        }

        OcrResult(lines = lines, confidence = signal)
    }

    private companion object {
        const val TAG = "RecipeOcr"
    }
}
