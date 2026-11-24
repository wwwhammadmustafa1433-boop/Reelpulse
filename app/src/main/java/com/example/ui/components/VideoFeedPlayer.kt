package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.example.data.local.VideoEntity
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseObsidian
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin

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

    var isManuallyPaused by remember(video.id) { mutableStateOf(false) }
    var isBuffering by remember(video.id) { mutableStateOf(true) }
    var playbackFailed by remember(video.id) { mutableStateOf(false) }
    var progress by remember(video.id) { mutableFloatStateOf(0f) }
    var activeWatchMs by remember(video.id) { mutableLongStateOf(0L) }
    var showHeartBurst by remember { mutableStateOf(false) }
    var heartTapOffset by remember { mutableStateOf(Offset.Zero) }
    val heartScale = remember { Animatable(0.4f) }

    val exoPlayer = remember(video.id) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
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

    // Double-tap heart animation trigger
    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            heartScale.snapTo(0.4f)
            heartScale.animateTo(1.25f, tween(220, easing = FastOutSlowInEasing))
            heartScale.animateTo(1.0f, tween(140))
            delay(320L)
            showHeartBurst = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseObsidian)
            .pointerInput(video.id) {
                detectTapGestures(
                    onTap = {
                        isManuallyPaused = !isManuallyPaused
                    },
                    onDoubleTap = { offset ->
                        heartTapOffset = offset
                        showHeartBurst = true
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

        // Double-tap burst heart at tap location
        if (showHeartBurst) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Liked",
                tint = PulseCrimson,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (heartTapOffset.x - 48.dp.toPx()).roundToInt(),
                            y = (heartTapOffset.y - 48.dp.toPx()).roundToInt()
                        )
                    }
                    .size((84f * heartScale.value).dp)
            )
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
