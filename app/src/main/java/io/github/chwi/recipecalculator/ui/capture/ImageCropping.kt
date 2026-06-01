package io.github.chwi.recipecalculator.ui.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

/**
 * A crop selection expressed as fractions (0..1) of the display-oriented image's width and height.
 * Resolution-independent, so the UI can compute it against a downsampled preview while the actual
 * crop runs against the full-resolution bitmap.
 */
data class CropRegion(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    /** True when the selection covers (essentially) the whole frame — nothing to crop. */
    val isWholeImage: Boolean
        get() = left <= 0.005f && top <= 0.005f && right >= 0.995f && bottom >= 0.995f

    companion object {
        val Full = CropRegion(0f, 0f, 1f, 1f)
    }
}

/**
 * Decode [uri] into a bitmap with its EXIF rotation already applied, optionally downsampled so the
 * longest edge is at most [maxEdge] px (0 = full resolution). CameraX and gallery JPEGs carry their
 * orientation in EXIF, which [BitmapFactory] ignores — so we rotate explicitly, otherwise the crop
 * overlay and the OCR input would be sideways. Returns null if the image can't be read.
 */
fun decodeOrientedBitmap(context: Context, uri: Uri, maxEdge: Int = 0): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val opts = BitmapFactory.Options()
        if (maxEdge > 0) {
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxEdge) sample *= 2
            opts.inSampleSize = sample
        }
        val raw = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val degrees = context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        if (degrees == 0f) {
            raw
        } else {
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true).also {
                if (it != raw) raw.recycle()
            }
        }
    } catch (_: Throwable) {
        null
    }
}

/**
 * Crop the full-resolution image at [sourceUri] to [region] and write the result as a JPEG into
 * `filesDir/ocr_captures`, returning the new file URI. Falls back to [sourceUri] unchanged when the
 * region is the whole image or anything goes wrong — the caller then just OCRs the original.
 */
fun cropToOcrFile(context: Context, sourceUri: Uri, region: CropRegion): Uri {
    if (region.isWholeImage) return sourceUri
    val full = decodeOrientedBitmap(context, sourceUri) ?: return sourceUri
    return try {
        val w = full.width
        val h = full.height
        val left = (region.left * w).roundToInt().coerceIn(0, w - 1)
        val top = (region.top * h).roundToInt().coerceIn(0, h - 1)
        val right = (region.right * w).roundToInt().coerceIn(left + 1, w)
        val bottom = (region.bottom * h).roundToInt().coerceIn(top + 1, h)

        val cropped = Bitmap.createBitmap(full, left, top, right - left, bottom - top)
        val dir = File(context.filesDir, "ocr_captures").apply { mkdirs() }
        val out = File(dir, "crop-${UUID.randomUUID()}.jpg")
        out.outputStream().use { cropped.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        if (cropped != full) cropped.recycle()
        Uri.fromFile(out)
    } catch (_: Throwable) {
        sourceUri
    } finally {
        full.recycle()
    }
}
