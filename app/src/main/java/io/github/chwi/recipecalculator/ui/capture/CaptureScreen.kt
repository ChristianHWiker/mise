package io.github.chwi.recipecalculator.ui.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.chwi.recipecalculator.ui.theme.RecipeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt

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

            is CaptureStage.Cropping -> CropStage(
                sourceUri = Uri.parse(stage.sourceUri),
                onConfirm = viewModel::onCropConfirmed,
                onCancel = viewModel::reset,
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
    var camera by remember { mutableStateOf<Camera?>(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }

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
                        camera = provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture,
                        )
                        zoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                    } catch (_: Throwable) {
                        // The state machine falls back to error if capture is attempted with no
                        // viable camera binding (e.g. emulator without a virtual camera).
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier
                .fillMaxSize()
                // Pinch to zoom: scale the live optical/digital zoom (and therefore the captured
                // photo) so the user can fill the frame with just the ingredient list.
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        val cam = camera ?: return@detectTransformGestures
                        val zoomState = cam.cameraInfo.zoomState.value ?: return@detectTransformGestures
                        val next = (zoomState.zoomRatio * zoomChange)
                            .coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                        cam.cameraControl.setZoomRatio(next)
                        zoomRatio = next
                    }
                },
        )

        if (zoomRatio > 1.04f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(percent = 50))
                    .padding(horizontal = RecipeTheme.spacing.lg, vertical = RecipeTheme.spacing.xs),
            ) {
                Text(
                    text = String.format(java.util.Locale.US, "%.1f×", zoomRatio),
                    style = RecipeTheme.typography.caption,
                    color = Color.White,
                )
            }
        }

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
                text = "PINCH TO ZOOM · FRAME THE LIST",
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

// ── Crop: trim to the ingredient list before OCR ─────────────────────────────

@Composable
private fun CropStage(
    sourceUri: Uri,
    onConfirm: (CropRegion) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    // Downsampled, EXIF-corrected bitmap for display only; the actual crop re-decodes at full res.
    val bitmap by produceState<ImageBitmap?>(initialValue = null, sourceUri) {
        value = withContext(Dispatchers.Default) {
            decodeOrientedBitmap(context, sourceUri, maxEdge = 1600)?.asImageBitmap()
        }
    }
    var region by remember { mutableStateOf(CropRegion(0.06f, 0.06f, 0.94f, 0.94f)) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (val image = bitmap) {
            null -> CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center),
            )

            else -> CropCanvas(
                image = image,
                onRegionChange = { region = it },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RecipeTheme.spacing.xl, vertical = RecipeTheme.spacing.xxl),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Filled.Close, contentDescription = "Discard photo", tint = Color.White)
            }
            Text(
                text = "DRAG TO CROP",
                style = RecipeTheme.typography.kicker,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.size(48.dp))
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = RecipeTheme.spacing.xl, vertical = RecipeTheme.spacing.xxxl),
            horizontalArrangement = Arrangement.spacedBy(RecipeTheme.spacing.lg, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryButtonOnDark(label = "Use full image", onClick = { onConfirm(CropRegion.Full) })
            PrimaryButton(label = "Scan crop", onClick = { onConfirm(region) })
        }
    }
}

/**
 * Renders the captured [image] fit-to-screen with a draggable, corner-resizable crop rectangle.
 * The rectangle is tracked in container pixels; [onRegionChange] reports it back as resolution-
 * independent [CropRegion] fractions of the displayed image so the ViewModel can crop the full-res
 * original.
 */
@Composable
private fun CropCanvas(
    image: ImageBitmap,
    onRegionChange: (CropRegion) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val cw = with(density) { maxWidth.toPx() }
        val ch = with(density) { maxHeight.toPx() }
        val bw = image.width.toFloat()
        val bh = image.height.toFloat()

        // The fitted (letterboxed) image rectangle inside the container — overlay math hangs off this.
        val scale = min(cw / bw, ch / bh)
        val dispW = bw * scale
        val dispH = bh * scale
        val imgLeft = (cw - dispW) / 2f
        val imgTop = (ch - dispH) / 2f
        val imgRight = imgLeft + dispW
        val imgBottom = imgTop + dispH
        val minSize = with(density) { 56.dp.toPx() }

        var crop by remember(cw, ch, bw, bh) {
            mutableStateOf(
                Rect(
                    imgLeft + dispW * 0.06f,
                    imgTop + dispH * 0.06f,
                    imgRight - dispW * 0.06f,
                    imgBottom - dispH * 0.06f,
                ),
            )
        }
        LaunchedEffect(crop, dispW, dispH) {
            onRegionChange(
                CropRegion(
                    left = ((crop.left - imgLeft) / dispW).coerceIn(0f, 1f),
                    top = ((crop.top - imgTop) / dispH).coerceIn(0f, 1f),
                    right = ((crop.right - imgLeft) / dispW).coerceIn(0f, 1f),
                    bottom = ((crop.bottom - imgTop) / dispH).coerceIn(0f, 1f),
                ),
            )
        }

        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )

        // Dim everything outside the crop rectangle, then outline it.
        Canvas(Modifier.fillMaxSize()) {
            val dim = Color.Black.copy(alpha = 0.55f)
            drawRect(dim, topLeft = Offset(0f, 0f), size = Size(cw, crop.top))
            drawRect(dim, topLeft = Offset(0f, crop.bottom), size = Size(cw, ch - crop.bottom))
            drawRect(dim, topLeft = Offset(0f, crop.top), size = Size(crop.left, crop.height))
            drawRect(
                dim,
                topLeft = Offset(crop.right, crop.top),
                size = Size(cw - crop.right, crop.height),
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(crop.left, crop.top),
                size = Size(crop.width, crop.height),
                style = Stroke(width = with(density) { 2.dp.toPx() }),
            )
        }

        // Drag the interior to move the whole selection.
        Box(
            Modifier
                .offset { IntOffset(crop.left.roundToInt(), crop.top.roundToInt()) }
                .size(
                    with(density) { crop.width.toDp() },
                    with(density) { crop.height.toDp() },
                )
                .pointerInput(imgLeft, imgTop, imgRight, imgBottom) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        val newLeft = (crop.left + drag.x).coerceIn(imgLeft, imgRight - crop.width)
                        val newTop = (crop.top + drag.y).coerceIn(imgTop, imgBottom - crop.height)
                        crop = Rect(newLeft, newTop, newLeft + crop.width, newTop + crop.height)
                    }
                },
        )

        // Four corner handles, each resizing its own edges.
        CropHandle(corner = Offset(crop.left, crop.top), density = density) { dx, dy ->
            crop = Rect(
                (crop.left + dx).coerceIn(imgLeft, crop.right - minSize),
                (crop.top + dy).coerceIn(imgTop, crop.bottom - minSize),
                crop.right,
                crop.bottom,
            )
        }
        CropHandle(corner = Offset(crop.right, crop.top), density = density) { dx, dy ->
            crop = Rect(
                crop.left,
                (crop.top + dy).coerceIn(imgTop, crop.bottom - minSize),
                (crop.right + dx).coerceIn(crop.left + minSize, imgRight),
                crop.bottom,
            )
        }
        CropHandle(corner = Offset(crop.left, crop.bottom), density = density) { dx, dy ->
            crop = Rect(
                (crop.left + dx).coerceIn(imgLeft, crop.right - minSize),
                crop.top,
                crop.right,
                (crop.bottom + dy).coerceIn(crop.top + minSize, imgBottom),
            )
        }
        CropHandle(corner = Offset(crop.right, crop.bottom), density = density) { dx, dy ->
            crop = Rect(
                crop.left,
                crop.top,
                (crop.right + dx).coerceIn(crop.left + minSize, imgRight),
                (crop.bottom + dy).coerceIn(crop.top + minSize, imgBottom),
            )
        }
    }
}

/** A round, draggable corner handle centered on [corner]; reports drag deltas in pixels. */
@Composable
private fun CropHandle(
    corner: Offset,
    density: androidx.compose.ui.unit.Density,
    onDrag: (dx: Float, dy: Float) -> Unit,
) {
    val touch = 44.dp
    val half = with(density) { touch.toPx() / 2f }
    Box(
        Modifier
            .offset { IntOffset((corner.x - half).roundToInt(), (corner.y - half).roundToInt()) }
            .size(touch)
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    onDrag(drag.x, drag.y)
                }
            },
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .size(18.dp)
                .background(Color.White, CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
        )
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

/** Secondary button for dark/camera surfaces — white outline and label. */
@Composable
private fun SecondaryButtonOnDark(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .padding(horizontal = RecipeTheme.spacing.xxxl, vertical = RecipeTheme.spacing.xl),
    ) {
        Text(text = label, style = RecipeTheme.typography.body, color = Color.White)
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
