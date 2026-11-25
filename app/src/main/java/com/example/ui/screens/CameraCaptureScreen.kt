package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.CameraFilterPreset
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseObsidian
import com.example.ui.theme.PulseOutline
import com.example.ui.theme.PulseSurface
import com.example.ui.theme.PulseSurfaceElevated
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.theme.PulseTextSecondary
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val MAX_RECORDING_DURATION_MS = 60_000L

@Composable
fun CameraCaptureScreen(
    initialFilter: CameraFilterPreset,
    onVideoRecorded: (videoUri: String, filterPreset: CameraFilterPreset, durationMs: Long) -> Unit,
    onGallerySelected: (Uri, CameraFilterPreset) -> Unit,
    onSwitchToLive: (CameraFilterPreset) -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionRequestedOnce = true
        hasCameraPermission = results[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        hasAudioPermission = results[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    var selectedFilter by remember { mutableStateOf(initialFilter) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var showFilterStrip by remember { mutableStateOf(true) }
    var zoomLevel by remember { mutableFloatStateOf(1f) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationMs by remember { mutableLongStateOf(0L) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Zero-permission Photo Picker shortcut inside Camera Studio
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onGallerySelected(uri, selectedFilter)
        }
    }

    // Request camera + mic permissions immediately if not granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    // Apply hardware-accelerated ColorMatrix filter directly onto PreviewView's TextureView layer
    LaunchedEffect(previewView, selectedFilter) {
        val colorFilter = selectedFilter.toAndroidColorFilter()
        if (colorFilter != null) {
            val paint = Paint().apply { this.colorFilter = colorFilter }
            previewView.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
        } else {
            previewView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
    }

    // Bind CameraX Preview + VideoCapture whenever permissions or lens selector changes
    DisposableEffect(lifecycleOwner, hasCameraPermission, useFrontCamera) {
        if (!hasCameraPermission) {
            return@DisposableEffect onDispose {}
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        var disposed = false

        cameraProviderFuture.addListener({
            if (disposed) return@addListener
            try {
                val cameraProvider = cameraProviderFuture.get()
                val selector = if (useFrontCamera && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    cameraError = "No camera sensor detected on this device. You can pick a video from your gallery."
                    return@addListener
                }

                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }

                val qualitySelector = QualitySelector.fromOrderedList(
                    listOf(Quality.FHD, Quality.HD, Quality.SD),
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                )
                val recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .build()
                val capture = VideoCapture.withOutput(recorder)

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    capture
                )
                boundCamera = camera
                videoCapture = capture
                torchEnabled = false
                zoomLevel = 1f
                cameraError = null
            } catch (e: Exception) {
                Log.e("CameraCapture", "Failed to bind CameraX", e)
                cameraError = "Unable to start camera preview: ${e.localizedMessage ?: "hardware unavailable"}"
            }
        }, executor)

        onDispose {
            disposed = true
            if (isRecording) {
                activeRecording?.stop()
                activeRecording = null
                isRecording = false
            }
        }
    }

    BackHandler {
        if (isRecording) {
            activeRecording?.stop()
            activeRecording = null
            isRecording = false
        }
        onClose()
    }

    fun startOrStopRecording() {
        val capture = videoCapture ?: return
        val current = activeRecording
        if (isRecording && current != null) {
            current.stop()
            activeRecording = null
            return
        }

        val recordingsDir = File(context.filesDir, "recordings").apply { mkdirs() }
        val outputFile = File(recordingsDir, "reel_${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(outputFile).build()

        var pending = capture.output.prepareRecording(context, outputOptions)
        if (hasAudioPermission) {
            pending = pending.withAudioEnabled()
        }

        val filterSnapshot = selectedFilter
        recordingDurationMs = 0L

        activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    isRecording = true
                }
                is VideoRecordEvent.Status -> {
                    val elapsedMs = TimeUnit.NANOSECONDS.toMillis(event.recordingStats.recordedDurationNanos)
                    recordingDurationMs = elapsedMs
                    if (elapsedMs >= MAX_RECORDING_DURATION_MS && isRecording) {
                        activeRecording?.stop()
                    }
                }
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    activeRecording = null
                    if (!event.hasError()) {
                        val finalDuration = recordingDurationMs.coerceAtLeast(1_500L)
                        onVideoRecorded(
                            Uri.fromFile(outputFile).toString(),
                            filterSnapshot,
                            finalDuration
                        )
                    } else {
                        outputFile.delete()
                        cameraError = "Recording failed (code ${event.error}). Try again or pick from gallery."
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseObsidian)
    ) {
        if (hasCameraPermission && cameraError == null) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Atmospheric filter overlays matching VideoFeedPlayer playback
            selectedFilter.tintOverlayColor?.let { tint ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(tint)
                )
            }
            selectedFilter.vignetteBrush()?.let { vignette ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(vignette)
                )
            }
            if (selectedFilter.hasScanlines) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 6.dp.toPx()
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.14f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        y += step
                    }
                }
            }
        } else {
            CameraPermissionOrErrorState(
                hasCameraPermission = hasCameraPermission,
                permissionRequestedOnce = permissionRequestedOnce,
                cameraError = cameraError,
                onRequestPermissions = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                    )
                },
                onOpenSettings = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                },
                onPickFromGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                }
            )
        }

        // Top Bar: Close + Recording Progress / Timer + Active Filter badge
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            if (isRecording) {
                LinearProgressIndicator(
                    progress = {
                        (recordingDurationMs.toFloat() / MAX_RECORDING_DURATION_MS.toFloat()).coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = PulseCrimson,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (isRecording) {
                            activeRecording?.stop()
                            activeRecording = null
                            isRecording = false
                        }
                        onClose()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .testTag("camera_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close camera",
                        tint = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(PulseCrimson, CircleShape)
                            )
                            Text(
                                text = formatTimer(recordingDurationMs) + " / 01:00",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = PulseCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Filter: ${selectedFilter.displayName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // Right Side Vertical Camera Tools Rail (Flip, Flash, Filters, Zoom)
        if (hasCameraPermission && cameraError == null) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CameraToolAction(
                    icon = Icons.Filled.Cameraswitch,
                    label = "Flip",
                    enabled = !isRecording,
                    onClick = { useFrontCamera = !useFrontCamera },
                    testTag = "camera_flip_button"
                )

                val flashAvailable = boundCamera?.cameraInfo?.hasFlashUnit() == true && !useFrontCamera
                CameraToolAction(
                    icon = if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    label = if (torchEnabled) "Flash On" else "Flash",
                    enabled = flashAvailable,
                    active = torchEnabled,
                    onClick = {
                        val next = !torchEnabled
                        torchEnabled = next
                        boundCamera?.cameraControl?.enableTorch(next)
                    },
                    testTag = "camera_flash_button"
                )

                CameraToolAction(
                    icon = Icons.Filled.AutoAwesome,
                    label = "Filters",
                    active = showFilterStrip,
                    onClick = { showFilterStrip = !showFilterStrip },
                    testTag = "camera_filters_toggle"
                )

                Surface(
                    onClick = {
                        val nextZoom = if (zoomLevel < 1.5f) 2f else 1f
                        zoomLevel = nextZoom
                        boundCamera?.cameraControl?.setZoomRatio(nextZoom)
                    },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (zoomLevel >= 1.5f) "2x" else "1x",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (zoomLevel >= 1.5f) PulseCyan else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bottom Filter Strip + Shutter + Gallery Shortcut
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(
                visible = showFilterStrip,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(CameraFilterPreset.entries, key = { it.id }) { preset ->
                        val isSelected = selectedFilter == preset
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedFilter = preset }
                                .padding(4.dp)
                                .semantics(mergeDescendants = true) {}
                                .testTag("filter_chip_${preset.id}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(preset.swatchColors))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.5.dp,
                                        color = if (isSelected) PulseCyan else Color.White.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) PulseCyan else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Picker Shortcut
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isRecording) {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                        .padding(6.dp)
                        .semantics(mergeDescendants = true) {}
                        .testTag("camera_gallery_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = "Pick from Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Gallery",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }

                // Record Shutter Button
                RecordShutterButton(
                    isRecording = isRecording,
                    enabled = hasCameraPermission && cameraError == null && videoCapture != null,
                    onClick = ::startOrStopRecording
                )

                // Placeholder balance for symmetrical shutter alignment
                Spacer(modifier = Modifier.width(60.dp))
            }

            // TikTok-style bottom mode switcher: Video | LIVE
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.16f),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("camera_mode_video")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Video",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    onClick = {
                        if (!isRecording) {
                            onSwitchToLive(selectedFilter)
                        }
                    },
                    enabled = !isRecording,
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.45f),
                    modifier = Modifier
                        .height(48.dp)
                        .border(1.dp, PulseCrimson.copy(alpha = 0.55f), RoundedCornerShape(50))
                        .testTag("camera_mode_live")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(PulseCrimson)
                        )
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isRecording) Color.White.copy(alpha = 0.4f) else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordShutterButton(
    isRecording: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "record_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.09f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shutter_scale"
    )

    Box(
        modifier = Modifier
            .size(84.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .border(
                width = 4.dp,
                color = if (isRecording) PulseCrimson else Color.White,
                shape = CircleShape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(7.dp)
            .testTag("camera_record_button"),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PulseCrimson),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = "Stop recording",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(if (enabled) PulseCrimson else PulseCrimson.copy(alpha = 0.45f))
            )
        }
    }
}

@Composable
private fun CameraToolAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    active: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp)
            .semantics(mergeDescendants = true) {}
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (active) PulseCyan.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.48f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    !enabled -> Color.White.copy(alpha = 0.35f)
                    active -> PulseCyan
                    else -> Color.White
                },
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) Color.White else Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun CameraPermissionOrErrorState(
    hasCameraPermission: Boolean,
    permissionRequestedOnce: Boolean,
    cameraError: String?,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickFromGallery: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseObsidian)
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PulseSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PulseOutline, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(PulseSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = null,
                        tint = PulseCrimson,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = if (!hasCameraPermission) {
                        "Camera & Microphone Access"
                    } else {
                        "Camera Unavailable"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PulseTextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = cameraError
                        ?: "Allow camera and microphone permissions to shoot short videos with live filters inside ReelPulse.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PulseTextSecondary,
                    textAlign = TextAlign.Center
                )

                if (!hasCameraPermission) {
                    Button(
                        onClick = onRequestPermissions,
                        colors = ButtonDefaults.buttonColors(containerColor = PulseCrimson),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Allow Camera & Mic", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    if (permissionRequestedOnce) {
                        OutlinedButton(
                            onClick = onOpenSettings,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Open App Settings", color = PulseCyan)
                        }
                    }
                }

                OutlinedButton(
                    onClick = onPickFromGallery,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Pick Video from Gallery", color = PulseTextPrimary)
                }
            }
        }
    }
}

private fun formatTimer(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
