package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.example.camera.CameraFilterPreset
import com.example.data.local.VideoEntity
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseObsidian
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private data class HeartBurstEvent(
    val id: Long,
    val offset: Offset,
    val rotationDeg: Float
)

@Composable
fun VideoFeedPlayer(
    video: VideoEntity,
    isActive: Boolean,
    isMuted: Boolean,
    onDoubleTapLike: () -> Unit,
    onWatchProgressRecorded: (VideoEntity, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hapticFeedback = LocalHapticFeedback.current

    var isManuallyPaused by remember(video.id) { mutableStateOf(false) }
    var isBuffering by remember(video.id) { mutableStateOf(true) }
    var playbackFailed by remember(video.id) { mutableStateOf(false) }
    var progress by remember(video.id) { mutableFloatStateOf(0f) }
    var activeWatchMs by remember(video.id) { mutableLongStateOf(0L) }
    val heartBursts = remember(video.id) { mutableStateListOf<HeartBurstEvent>() }
    var nextBurstId by remember(video.id) { mutableLongStateOf(0L) }

    val filterPreset = remember(video.filterId) { CameraFilterPreset.fromId(video.filterId) }

    @androidx.annotation.OptIn(UnstableApi::class)
    val exoPlayer = remember(video.id, filterPreset) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            val effects = filterPreset.toMedia3Effects()
            if (effects.isNotEmpty()) {
                setVideoEffects(effects)
            }
            setMediaItem(MediaItem.fromUri(video.videoUrl))
            prepare()
        }
    }

    // Sync mute state
    LaunchedEffect(exoPlayer, isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Sync active page + manual pause
    LaunchedEffect(exoPlayer, isActive, isManuallyPaused) {
        exoPlayer.playWhenReady = isActive && !isManuallyPaused
    }

    // Track player state & errors
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackFailed = true
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    // Lifecycle pause/resume + watch time recording on disposal or deactivation
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.playWhenReady = false
                Lifecycle.Event.ON_RESUME -> if (isActive && !isManuallyPaused) {
                    exoPlayer.playWhenReady = true
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (activeWatchMs > 0L) {
                onWatchProgressRecorded(video, activeWatchMs)
                activeWatchMs = 0L
            }
            exoPlayer.release()
        }
    }

    // Emit watch progress when page becomes inactive
    LaunchedEffect(isActive) {
        if (!isActive && activeWatchMs > 0L) {
            onWatchProgressRecorded(video, activeWatchMs)
            activeWatchMs = 0L
        }
        if (isActive) {
            isManuallyPaused = false
        }
    }

    // Track real playback progress & elapsed watch time while active and playing
    LaunchedEffect(exoPlayer, isActive, isManuallyPaused, playbackFailed) {
        while (isActive) {
            if (!isManuallyPaused) {
                activeWatchMs += 250L
                val duration = exoPlayer.duration.takeIf { it > 0L } ?: video.durationMs
                val pos = if (!playbackFailed && exoPlayer.duration > 0L) {
                    exoPlayer.currentPosition
                } else {
                    (activeWatchMs % duration)
                }
                progress = (pos.toFloat() / duration.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
            }
            delay(250L)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseObsidian)
            .testTag("video_player_surface")
            .pointerInput(video.id) {
                detectTapGestures(
                    onTap = {
                        isManuallyPaused = !isManuallyPaused
                    },
                    onDoubleTap = { offset ->
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        val tilt = Random.nextInt(-18, 19).toFloat()
                        heartBursts.add(
                            HeartBurstEvent(
                                id = nextBurstId++,
                                offset = offset,
                                rotationDeg = tilt
                            )
                        )
                        onDoubleTapLike()
                    }
                )
            }
    ) {
        if (!playbackFailed) {
            ContentFrame(
                player = exoPlayer,
                modifier = Modifier.fillMaxSize(),
                surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                contentScale = ContentScale.Crop,
                shutter = {
                    AnimatedPosterBackdrop(
                        video = video,
                        isBuffering = isBuffering,
                        isFallbackMode = false
                    )
                }
            )
        } else {
            AnimatedPosterBackdrop(
                video = video,
                isBuffering = false,
                isFallbackMode = true
            )
        }

        // Optional atmospheric filter overlays (tint, vignette, VHS scanlines)
        filterPreset.tintOverlayColor?.let { tint ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tint)
            )
        }
        filterPreset.vignetteBrush()?.let { vignette ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(vignette)
            )
        }
        if (filterPreset.hasScanlines) {
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

        // Top & bottom cinematic vignette scrims for guaranteed text readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Center pause indicator
        AnimatedVisibility(
            visible = isManuallyPaused,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.8f),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        // Concurrent double-tap burst heart overlays at exact tap coordinates
        heartBursts.forEach { burst ->
            key(burst.id) {
                DoubleTapHeartOverlay(
                    event = burst,
                    onAnimationFinished = { finishedId ->
                        heartBursts.removeAll { it.id == finishedId }
                    }
                )
            }
        }

        // Slim bottom playback progress indicator
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.5.dp)
                .align(Alignment.BottomCenter),
            color = PulseCrimson,
            trackColor = Color.White.copy(alpha = 0.22f),
            strokeCap = StrokeCap.Butt
        )
    }
}

@Composable
private fun DoubleTapHeartOverlay(
    event: HeartBurstEvent,
    onAnimationFinished: (Long) -> Unit
) {
    val scale = remember(event.id) { Animatable(0.35f) }
    val alpha = remember(event.id) { Animatable(1f) }
    val driftY = remember(event.id) { Animatable(0f) }
    val sparkProgress = remember(event.id) { Animatable(0f) }

    LaunchedEffect(event.id) {
        coroutineScope {
            launch {
                scale.animateTo(
                    targetValue = 1.28f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                scale.animateTo(1.04f, tween(130, easing = FastOutSlowInEasing))
            }
            launch {
                sparkProgress.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
            }
            launch {
                delay(240L)
                driftY.animateTo(-72f, tween(460, easing = FastOutSlowInEasing))
            }
            launch {
                delay(420L)
                alpha.animateTo(0f, tween(280, easing = LinearEasing))
            }
        }
        onAnimationFinished(event.id)
    }

    val boxSize = 136.dp
    Box(
        modifier = Modifier
            .offset {
                val halfPx = (boxSize / 2).toPx()
                IntOffset(
                    x = (event.offset.x - halfPx).roundToInt(),
                    y = (event.offset.y - halfPx + driftY.value.dp.toPx()).roundToInt()
                )
            }
            .size(boxSize)
            .alpha(alpha.value)
            .testTag("double_tap_heart_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val progress = sparkProgress.value
            val ringRadius = (size.minDimension * 0.22f) + (size.minDimension * 0.28f * progress)
            val ringAlpha = (1f - progress).coerceIn(0f, 1f) * 0.65f
            if (ringAlpha > 0.01f) {
                drawCircle(
                    color = PulseCrimson.copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx() * (1f - progress * 0.6f))
                )
                val particleCount = 6
                val particleOrbit = (size.minDimension * 0.26f) + (size.minDimension * 0.24f * progress)
                for (i in 0 until particleCount) {
                    val angle = Math.toRadians((i * (360.0 / particleCount)) - 15.0)
                    val px = center.x + (particleOrbit * cos(angle)).toFloat()
                    val py = center.y + (particleOrbit * sin(angle)).toFloat()
                    drawCircle(
                        color = if (i % 2 == 0) PulseCrimson.copy(alpha = ringAlpha)
                        else PulseCyan.copy(alpha = ringAlpha),
                        radius = 4.dp.toPx() * (1f - progress * 0.5f),
                        center = Offset(px, py)
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Liked",
            tint = PulseCrimson,
            modifier = Modifier
                .size(94.dp)
                .scale(scale.value)
                .rotate(event.rotationDeg)
        )
    }
}

@Composable
private fun AnimatedPosterBackdrop(
    video: VideoEntity,
    isBuffering: Boolean,
    isFallbackMode: Boolean
) {
    val accent = Color(video.accentHex)
    val transition = rememberInfiniteTransition(label = "pulse_backdrop")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        PulseObsidian,
                        accent.copy(alpha = 0.35f),
                        PulseObsidian
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.32f), Color.Transparent),
                    center = Offset(w * (0.3f + 0.15f * sin(phase)), h * 0.38f),
                    radius = w * 0.7f
                ),
                center = Offset(w * (0.3f + 0.15f * sin(phase)), h * 0.38f),
                radius = w * 0.7f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(PulseCyan.copy(alpha = 0.18f), Color.Transparent),
                    center = Offset(w * 0.72f, h * (0.62f + 0.08f * sin(phase + 1.5f))),
                    radius = w * 0.55f
                ),
                center = Offset(w * 0.72f, h * (0.62f + 0.08f * sin(phase + 1.5f))),
                radius = w * 0.55f
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CreatorAvatar(
                avatarKey = video.creatorAvatarKey,
                displayName = video.creatorName,
                size = 84.dp,
                borderColor = accent
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = PulseCyan,
                    strokeWidth = 2.dp
                )
            } else if (isFallbackMode) {
                Text(
                    text = "Visual Pulse Preview",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseCyan
                )
            }
        }
    }
}
