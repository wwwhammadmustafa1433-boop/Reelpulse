package com.example.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.CameraFilterPreset
import com.example.data.remote.AccountVisibility
import com.example.data.remote.LiveStreamSession
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.LiveBroadcastOverlay
import com.example.ui.components.LiveEndedSummaryCard
import com.example.ui.components.LivePreSetupOverlay
import com.example.ui.components.PulsingLiveBadge
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCrimsonDark
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseObsidian
import com.example.ui.theme.PulseOutline
import com.example.ui.theme.PulseSurface
import com.example.ui.theme.PulseSurfaceElevated
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.theme.PulseTextSecondary
import com.example.ui.viewmodel.LiveRoomMode
import com.example.ui.viewmodel.LiveStreamUiState

@Composable
fun LiveScreen(
    uiState: LiveStreamUiState,
    userDisplayName: String,
    userHandle: String,
    accountVisibility: AccountVisibility,
    onOpenGoLiveSetup: () -> Unit,
    onOpenHub: () -> Unit,
    onUpdateDraftTitle: (String) -> Unit,
    onUpdateDraftCategory: (String) -> Unit,
    onSelectFilter: (CameraFilterPreset) -> Unit,
    onStartBroadcast: () -> Unit,
    onJoinStream: (LiveStreamSession) -> Unit,
    onEndOrLeaveStream: () -> Unit,
    onSendChatMessage: (String) -> Unit,
    onSendHeartReaction: () -> Unit,
    onDismissError: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val needsCamera = uiState.mode == LiveRoomMode.PRE_LIVE_SETUP ||
        uiState.mode == LiveRoomMode.HOST_BROADCASTING

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var useFrontCamera by remember { mutableStateOf(true) }
    var torchEnabled by remember { mutableStateOf(false) }
    var micMuted by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPermission = results[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    LaunchedEffect(needsCamera) {
        if (needsCamera && !hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            )
        }
    }

    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(previewView, uiState.selectedFilter) {
        val colorFilter = uiState.selectedFilter.toAndroidColorFilter()
        if (colorFilter != null) {
            previewView.setLayerType(
                View.LAYER_TYPE_HARDWARE,
                Paint().apply { this.colorFilter = colorFilter }
            )
        } else {
            previewView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
    }

    DisposableEffect(lifecycleOwner, needsCamera, hasCameraPermission, useFrontCamera) {
        if (!needsCamera || !hasCameraPermission) {
            return@DisposableEffect onDispose {}
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        var disposed = false

        cameraProviderFuture.addListener({
            if (disposed) return@addListener
            try {
                val provider = cameraProviderFuture.get()
                val selector = if (useFrontCamera && provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    cameraError = "Camera sensor unavailable — broadcasting in Audio/Studio stage mode."
                    return@addListener
                }

                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }
                provider.unbindAll()
                val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview)
                boundCamera = camera
                torchEnabled = false
                cameraError = null
            } catch (e: Exception) {
                Log.e("LiveScreen", "Failed to bind CameraX preview for Live", e)
                cameraError = "Camera preview unavailable — studio stage mode active."
            }
        }, executor)

        onDispose {
            disposed = true
        }
    }

    BackHandler {
        when (uiState.mode) {
            LiveRoomMode.HOST_BROADCASTING,
            LiveRoomMode.VIEWING_STREAM -> onEndOrLeaveStream()
            LiveRoomMode.PRE_LIVE_SETUP -> onOpenHub()
            LiveRoomMode.HUB,
            LiveRoomMode.ENDED_SUMMARY -> onClose()
        }
    }

    fun shareCurrentStream(stream: LiveStreamSession?) {
        val id = stream?.id ?: "studio"
        val title = stream?.title ?: uiState.draftTitle.ifBlank { "ReelPulse Live" }
        val handle = stream?.hostHandle ?: userHandle
        val shareLink = "https://reelpulse.app/live/$id"
        val shareText = "Join $handle LIVE on ReelPulse: \"$title\"\n$shareLink"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "$title — LIVE on ReelPulse")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(intent, "Invite to Live").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            clipboardManager.setText(AnnotatedString(shareLink))
            Toast.makeText(context, "Live link copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseObsidian)
    ) {
        when (uiState.mode) {
            LiveRoomMode.HUB -> {
                LiveHubContent(
                    uiState = uiState,
                    onGoLiveClick = onOpenGoLiveSetup,
                    onJoinStream = onJoinStream,
                    onDismissError = onDismissError,
                    onClose = onClose
                )
            }

            LiveRoomMode.PRE_LIVE_SETUP,
            LiveRoomMode.HOST_BROADCASTING,
            LiveRoomMode.VIEWING_STREAM -> {
                val activeFilter = if (uiState.mode == LiveRoomMode.VIEWING_STREAM) {
                    CameraFilterPreset.fromId(uiState.activeStream?.filterId ?: "normal")
                } else {
                    uiState.selectedFilter
                }

                // Live camera preview (when host) or stylized live broadcast stage (viewer/fallback)
                if (needsCamera && hasCameraPermission && cameraError == null) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LiveStageBackdrop(
                        hostName = uiState.activeStream?.hostName ?: userDisplayName,
                        hostHandle = uiState.activeStream?.hostHandle ?: userHandle,
                        streamTitle = uiState.activeStream?.title ?: uiState.draftTitle.ifBlank { "Live Studio" },
                        hasCameraPermission = hasCameraPermission,
                        isHostSetup = needsCamera,
                        onGrantPermission = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            )
                        },
                        onOpenSettings = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                            )
                        }
                    )
                }

                // Atmospheric filter layers
                activeFilter.tintOverlayColor?.let { tint ->
                    Box(modifier = Modifier.fillMaxSize().background(tint))
                }
                activeFilter.vignetteBrush()?.let { vignette ->
                    Box(modifier = Modifier.fillMaxSize().background(vignette))
                }
                if (activeFilter.hasScanlines) {
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

                if (uiState.mode == LiveRoomMode.PRE_LIVE_SETUP) {
                    LivePreSetupOverlay(
                        draftTitle = uiState.draftTitle,
                        draftCategory = uiState.draftCategory,
                        selectedFilter = uiState.selectedFilter,
                        accountVisibility = accountVisibility,
                        isStarting = uiState.isStartingStream,
                        errorMessage = uiState.errorMessage,
                        flashAvailable = boundCamera?.cameraInfo?.hasFlashUnit() == true && !useFrontCamera,
                        torchEnabled = torchEnabled,
                        onTitleChange = onUpdateDraftTitle,
                        onCategorySelect = onUpdateDraftCategory,
                        onFilterSelect = onSelectFilter,
                        onFlipCamera = { useFrontCamera = !useFrontCamera },
                        onToggleFlash = {
                            val next = !torchEnabled
                            torchEnabled = next
                            boundCamera?.cameraControl?.enableTorch(next)
                        },
                        onStartLive = onStartBroadcast,
                        onDismissError = onDismissError,
                        onClose = onOpenHub
                    )
                } else {
                    val stream = uiState.activeStream
                    LiveBroadcastOverlay(
                        stream = stream,
                        isHost = uiState.mode == LiveRoomMode.HOST_BROADCASTING,
                        selectedFilter = activeFilter,
                        elapsedSeconds = uiState.elapsedSeconds,
                        chatMessages = uiState.chatMessages,
                        heartPulseTick = uiState.heartPulseTick,
                        micMuted = micMuted,
                        isEnding = uiState.isEndingStream,
                        errorMessage = uiState.errorMessage,
                        onFlipCamera = { useFrontCamera = !useFrontCamera },
                        onToggleMic = { micMuted = !micMuted },
                        onSelectFilter = onSelectFilter,
                        onSendChat = onSendChatMessage,
                        onSendHeart = onSendHeartReaction,
                        onShareStream = { shareCurrentStream(stream) },
                        onEndOrLeave = onEndOrLeaveStream,
                        onDismissError = onDismissError
                    )
                }
            }

            LiveRoomMode.ENDED_SUMMARY -> {
                LiveEndedSummaryCard(
                    title = uiState.summaryTitle,
                    durationSeconds = uiState.summaryDurationSeconds,
                    totalLikes = uiState.summaryLikes,
                    onGoLiveAgain = onOpenGoLiveSetup,
                    onBackToHub = onOpenHub,
                    onClose = onClose
                )
            }
        }
    }
}

@Composable
private fun LiveHubContent(
    uiState: LiveStreamUiState,
    onGoLiveClick: () -> Unit,
    onJoinStream: (LiveStreamSession) -> Unit,
    onDismissError: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("live_hub_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = PulseTextPrimary
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "ReelPulse Live",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTextPrimary
                                )
                                PulsingLiveBadge()
                            }
                            Text(
                                text = "Broadcast live or join active creator rooms",
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onGoLiveClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PulseCrimson),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("live_hub_go_live_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Podcasts,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Go LIVE", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            if (uiState.errorMessage != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PulseCrimson.copy(alpha = 0.16f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PulseCrimson.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = uiState.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseCrimson,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismissError) {
                                Text("Dismiss", color = Color.White)
                            }
                        }
                    }
                }
            }

            // Hero Go Live Studio Card
            item {
                Surface(
                    onClick = onGoLiveClick,
                    shape = RoundedCornerShape(22.dp),
                    color = PulseSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(listOf(PulseCrimson, PulseCyan)),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .testTag("live_hero_go_live_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        PulseCrimson.copy(alpha = 0.18f),
                                        PulseSurfaceElevated,
                                        PulseCyan.copy(alpha = 0.12f)
                                    )
                                )
                            )
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(listOf(PulseCrimson, PulseCrimsonDark))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sensors,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Start a Live Broadcast",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PulseTextPrimary
                            )
                            Text(
                                text = "Stream with real-time camera filters, live viewer chat, and instant heart reactions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTextSecondary
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Live Now (${uiState.liveStreams.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PulseTextPrimary
                )
            }

            if (uiState.isLoadingStreams) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PulseCrimson)
                    }
                }
            } else if (uiState.liveStreams.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PulseSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PulseOutline, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Podcasts,
                                contentDescription = null,
                                tint = PulseCyan,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No live broadcasts right now",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PulseTextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Be the first creator to go live and chat with viewers in real time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(uiState.liveStreams, key = { it.id }) { stream ->
                    LiveStreamRoomCard(
                        stream = stream,
                        onClick = { onJoinStream(stream) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveStreamRoomCard(
    stream: LiveStreamSession,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = PulseSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PulseOutline, RoundedCornerShape(18.dp))
            .testTag("live_room_card_${stream.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CreatorAvatar(
                avatarKey = "local_user",
                displayName = stream.hostName,
                size = 54.dp,
                borderColor = PulseCrimson
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PulsingLiveBadge()
                    Text(
                        text = stream.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (stream.visibility == "personal") {
                        Text(
                            text = "• Personal",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseCrimson
                        )
                    }
                }
                Text(
                    text = stream.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PulseTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stream.hostHandle,
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseTextSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = PulseTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatCompactCount(stream.viewerCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTextSecondary
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = PulseCrimson,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatCompactCount(stream.likeCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTextSecondary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PulseCrimson.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Watch live stream",
                    tint = PulseCrimson
                )
            }
        }
    }
}

@Composable
private fun LiveStageBackdrop(
    hostName: String,
    hostHandle: String,
    streamTitle: String,
    hasCameraPermission: Boolean,
    isHostSetup: Boolean,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        PulseObsidian,
                        PulseCrimson.copy(alpha = 0.25f),
                        PulseSurface,
                        PulseObsidian
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CreatorAvatar(
                avatarKey = "local_user",
                displayName = hostName,
                size = 86.dp,
                borderColor = PulseCrimson
            )
            Text(
                text = hostHandle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PulseCyan
            )
            Text(
                text = streamTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            if (isHostSetup && !hasCameraPermission) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Allow camera access to stream video, or go live in audio stage mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PulseTextSecondary,
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onGrantPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = PulseCrimson)
                    ) {
                        Text("Allow Camera", color = Color.White)
                    }
                    OutlinedButton(onClick = onOpenSettings) {
                        Text("Settings", color = PulseCyan)
                    }
                }
            }
        }
    }
}
