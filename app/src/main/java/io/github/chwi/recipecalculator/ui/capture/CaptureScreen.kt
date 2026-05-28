package io.github.chwi.recipecalculator.ui.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chwi.recipecalculator.ui.theme.RecipeTheme
import java.io.File
import java.util.UUID

private val PagePadding = 22.dp

/**
 * Entry point for the OCR capture flow. Owns four in-place states (idle / live preview / recognizing
 * / error) driven by [CaptureViewModel.state]. When the VM transitions to [CaptureStage.Confirm],
 * the host route navigates to the confirmation screen — this composable doesn't render confirm.
 */
@Composable
fun CaptureScreen(
    onBack: () -> Unit,
    onParsed: () -> Unit,
    viewModel: CaptureViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.stage) {
        if (state.stage is CaptureStage.Confirm) onParsed()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { picked: Uri? ->
        if (picked != null) {
            val copied = copyToOcrCaptures(context, picked)
            if (copied != null) viewModel.onImageReady(copied)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val stage = state.stage) {
            CaptureStage.Idle -> IdleStage(
                onBack = onBack,
                onUseCamera = viewModel::openCamera,
                onPickFromGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )

            CaptureStage.LivePreview -> CameraStage(
                onCancel = viewModel::cancelCamera,
                onCaptured = viewModel::onImageReady,
                allocateFile = viewModel::newCaptureFile,
            )

            CaptureStage.Recognizing -> RecognizingStage()

            is CaptureStage.Error -> ErrorStage(
                message = stage.message,
                onRetry = viewModel::reset,
                onBack = onBack,
            )

            is CaptureStage.Confirm -> {
                // Transient: the LaunchedEffect above is already navigating away.
            }
        }
    }
}

// ── Idle: pick a path ────────────────────────────────────────────────────────

@Composable
private fun IdleStage(
    onBack: () -> Unit,
    onUseCamera: () -> Unit,
    onPickFromGallery: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = PagePadding)
                .padding(top = RecipeTheme.spacing.huge),
            verticalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.xxl),
        ) {
            Text(
                text = "CAPTURE",
                style = RecipeTheme.typography.kicker,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Scan a recipe",
                style = RecipeTheme.typography.detailH1,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Photograph an ingredient list — printed page, book, or screenshot. " +
                    "We'll read it on-device and let you fix any mis-parses before saving.",
                style = RecipeTheme.typography.body,
                color = RecipeTheme.colors.muted,
            )
            Spacer(Modifier.height(RecipeTheme.spacing.lg))
            ActionCard(
                icon = Icons.Filled.CameraAlt,
                title = "Use camera",
                subtitle = "Live preview · best for printed pages",
                onClick = onUseCamera,
            )
            ActionCard(
                icon = Icons.Filled.PhotoLibrary,
                title = "Pick from photos",
                subtitle = "Choose an existing image",
                onClick = onPickFromGallery,
            )
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RecipeTheme.colors.rule, RoundedCornerShape(RecipeTheme.radii.card))
            .clickable(onClick = onClick)
            .padding(horizontal = RecipeTheme.spacing.xl, vertical = RecipeTheme.spacing.xxl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(RecipeTheme.spacing.xl))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = RecipeTheme.typography.sectionH2,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(RecipeTheme.spacing.xxs))
            Text(
                text = subtitle,
                style = RecipeTheme.typography.caption,
                color = RecipeTheme.colors.muted,
            )
        }
    }
}

// ── LivePreview: CameraX ─────────────────────────────────────────────────────

@Composable
private fun CameraStage(
    onCancel: () -> Unit,
    onCaptured: (Uri) -> Unit,
    allocateFile: () -> File,
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(cameraPermissionGranted(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted: Boolean -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        CameraPermissionRationale(
            onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onCancel = onCancel,
        )
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val providerFuture = ProcessCameraProvider.getInstance(ctx)
                providerFuture.addListener({
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture,
                        )
                    } catch (_: Throwable) {
                        // The state machine falls back to error if capture is attempted with no
                        // viable camera binding (e.g. emulator without a virtual camera).
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RecipeTheme.spacing.xl, vertical = RecipeTheme.spacing.xxl),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = Color.White)
            }
            Text(
                text = "FRAME THE INGREDIENTS",
                style = RecipeTheme.typography.kicker,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.size(48.dp))
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(72.dp)
                .border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                .background(Color.White, CircleShape)
                .clickable {
                    val target = allocateFile()
                    val output = ImageCapture.OutputFileOptions.Builder(target).build()
                    imageCapture.takePicture(
                        output,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                onCaptured(Uri.fromFile(target))
                            }

                            override fun onError(exception: ImageCaptureException) {
                                // OCR will fail on the placeholder URI and the VM will surface an
                                // error state — keeps a single error path.
                                onCaptured(Uri.fromFile(target))
                            }
                        },
                    )
                },
        )
    }
}

@Composable
private fun CameraPermissionRationale(onRequest: () -> Unit, onCancel: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = PagePadding, vertical = RecipeTheme.spacing.huge),
        verticalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.xl, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "CAMERA",
            style = RecipeTheme.typography.kicker,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Camera access is needed to scan recipes.",
            style = RecipeTheme.typography.detailH1.copy(fontSize = 22.sp),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Text recognition runs entirely on your device — nothing is uploaded.",
            style = RecipeTheme.typography.body,
            color = RecipeTheme.colors.muted,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.lg)) {
            PrimaryButton(label = "Allow", onClick = onRequest)
            SecondaryButton(label = "Cancel", onClick = onCancel)
        }
    }
}

// ── Recognizing ──────────────────────────────────────────────────────────────

@Composable
private fun RecognizingStage() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.xl, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text(
            text = "Reading recipe…",
            style = RecipeTheme.typography.body,
            color = RecipeTheme.colors.muted,
        )
    }
}

// ── Error ────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorStage(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar(onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = PagePadding),
            verticalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.xl, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "COULDN'T READ",
                style = RecipeTheme.typography.kicker,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = message,
                style = RecipeTheme.typography.body,
                color = RecipeTheme.colors.muted,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(label = "Try again", onClick = onRetry)
        }
    }
}

// ── Shared chrome ────────────────────────────────────────────────────────────

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = RecipeTheme.spacing.lg, vertical = RecipeTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
internal fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(horizontal = RecipeTheme.spacing.xxxl, vertical = RecipeTheme.spacing.xl),
    ) {
        Text(text = label, style = RecipeTheme.typography.body, color = Color.White)
    }
}

@Composable
internal fun SecondaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, RecipeTheme.colors.rule, RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(horizontal = RecipeTheme.spacing.xxxl, vertical = RecipeTheme.spacing.xl),
    ) {
        Text(
            text = label,
            style = RecipeTheme.typography.body,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun cameraPermissionGranted(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

/** Copy a picker-supplied URI into `filesDir/ocr_captures/<uuid>.jpg`. */
private fun copyToOcrCaptures(context: Context, source: Uri): Uri? {
    return try {
        val dir = File(context.filesDir, "ocr_captures").apply { mkdirs() }
        val target = File(dir, "${UUID.randomUUID()}.jpg")
        val streamed = context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { input.copyTo(it) }
            true
        } ?: false
        if (streamed) Uri.fromFile(target) else null
    } catch (_: Throwable) {
        null
    }
}
