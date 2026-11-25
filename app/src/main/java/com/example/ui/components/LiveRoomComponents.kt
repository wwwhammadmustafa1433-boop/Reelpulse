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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.camera.CameraFilterPreset
import com.example.data.remote.AccountVisibility
import com.example.data.remote.LiveChatMessage
import com.example.data.remote.LiveStreamSession
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCrimsonDark
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseObsidian
import com.example.ui.theme.PulseOutline
import com.example.ui.theme.PulseSurface
import com.example.ui.theme.PulseSurfaceElevated
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.theme.PulseTextSecondary
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

private val liveCategories = listOf("Music", "Tech", "Dance", "Food", "Travel", "Fitness", "Art")

private data class FloatingLiveHeart(
    val id: Long,
    val horizontalDriftPx: Float,
    val isCyan: Boolean
)

@Composable
fun PulsingLiveBadge(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "live_dot")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_dot_alpha"
    )
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = PulseCrimson,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .alpha(dotAlpha)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Text(
                text = "LIVE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun LivePreSetupOverlay(
    draftTitle: String,
    draftCategory: String,
    selectedFilter: CameraFilterPreset,
    accountVisibility: AccountVisibility,
    isStarting: Boolean,
    errorMessage: String?,
    flashAvailable: Boolean,
    torchEnabled: Boolean,
    onTitleChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onFilterSelect: (CameraFilterPreset) -> Unit,
    onFlipCamera: () -> Unit,
    onToggleFlash: () -> Unit,
    onStartLive: () -> Unit,
    onDismissError: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top scrim + controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("live_setup_close_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close live setup",
                    tint = Color.White
                )
            }

            val isPersonal = accountVisibility == AccountVisibility.PERSONAL
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.58f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isPersonal) Icons.Filled.Lock else Icons.Filled.Public,
                        contentDescription = null,
                        tint = if (isPersonal) PulseCrimson else PulseCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isPersonal) "Personal Live" else "Public Live",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onFlipCamera,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("live_flip_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cameraswitch,
                        contentDescription = "Flip camera",
                        tint = Color.White
                    )
                }
                if (flashAvailable) {
                    IconButton(
                        onClick = onToggleFlash,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = "Toggle flash",
                            tint = if (torchEnabled) PulseCyan else Color.White
                        )
                    }
                }
            }
        }

        // Bottom Setup Sheet
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = PulseObsidian.copy(alpha = 0.88f),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .border(1.dp, PulseOutline, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PulseCrimson.copy(alpha = 0.18f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = errorMessage,
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

                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = onTitleChange,
                    label = { Text("Live Stream Title") },
                    placeholder = { Text("Add a title for your Live...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PulseSurfaceElevated,
                        unfocusedContainerColor = PulseSurface,
                        focusedBorderColor = PulseCyan,
                        unfocusedBorderColor = PulseOutline,
                        focusedTextColor = PulseTextPrimary,
                        unfocusedTextColor = PulseTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_title_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(liveCategories) { category ->
                        FilterChip(
                            selected = draftCategory == category,
                            onClick = { onCategorySelect(category) },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PulseCrimson,
                                selectedLabelColor = Color.White,
                                containerColor = PulseSurfaceElevated,
                                labelColor = PulseTextSecondary
                            ),
                            border = null
                        )
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(CameraFilterPreset.entries, key = { it.id }) { preset ->
                        val selected = preset == selectedFilter
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onFilterSelect(preset) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(preset.swatchColors))
                                    .border(
                                        width = if (selected) 2.5.dp else 1.dp,
                                        color = if (selected) PulseCyan else PulseOutline,
                                        shape = CircleShape
                                    )
                            )
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) PulseCyan else PulseTextSecondary
                            )
                        }
                    }
                }

                Button(
                    onClick = onStartLive,
                    enabled = !isStarting,
                    colors = ButtonDefaults.buttonColors(containerColor = PulseCrimson),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_live_broadcast_button")
                ) {
                    if (isStarting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Starting Live...", fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Podcasts,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Go LIVE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveBroadcastOverlay(
    stream: LiveStreamSession?,
    isHost: Boolean,
    selectedFilter: CameraFilterPreset,
    elapsedSeconds: Long,
    chatMessages: List<LiveChatMessage>,
    heartPulseTick: Long,
    micMuted: Boolean,
    isEnding: Boolean,
    errorMessage: String?,
    onFlipCamera: () -> Unit,
    onToggleMic: () -> Unit,
    onSelectFilter: (CameraFilterPreset) -> Unit,
    onSendChat: (String) -> Unit,
    onSendHeart: () -> Unit,
    onShareStream: () -> Unit,
    onEndOrLeave: () -> Unit,
    onDismissError: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var chatDraft by rememberSaveable { mutableStateOf("") }
    var showFilterStrip by remember { mutableStateOf(false) }
    val chatListState = rememberLazyListState()
    val floatingHearts = remember { mutableStateListOf<FloatingLiveHeart>() }
    var nextHeartId by remember { mutableLongStateOf(0L) }

    LaunchedEffect(heartPulseTick) {
        if (heartPulseTick > 0L) {
            floatingHearts.add(
                FloatingLiveHeart(
                    id = nextHeartId++,
                    horizontalDriftPx = Random.nextInt(-36, 37).toFloat(),
                    isCyan = Random.nextBoolean()
                )
            )
        }
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            chatListState.animateScrollToItem(chatMessages.lastIndex)
        }
    }

    fun submitChat() {
        val text = chatDraft.trim()
        if (text.isNotEmpty()) {
            onSendChat(text)
            chatDraft = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top & Bottom gradient readability scrims
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
        )

        // Top Header: Host Pill + Live Badge + Timer + Viewer Count + End/Leave
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.56f),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Row(
                    modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CreatorAvatar(
                        avatarKey = "local_user",
                        displayName = stream?.hostName ?: "Creator",
                        size = 34.dp,
                        borderColor = PulseCrimson
                    )
                    Column {
                        Text(
                            text = stream?.hostHandle ?: "@creator",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stream?.title ?: "Live Studio",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    PulsingLiveBadge()
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.56f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = "Viewers",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatCompactCount(stream?.viewerCount ?: 1),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• ${formatLiveDuration(elapsedSeconds)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTextSecondary
                        )
                    }
                }

                Button(
                    onClick = onEndOrLeave,
                    enabled = !isEnding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHost) PulseCrimson else Color.Black.copy(alpha = 0.65f)
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .testTag("end_live_button")
                ) {
                    Text(
                        text = if (isHost) "End" else "Leave",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Right-side Host & Reaction Rail
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isHost) {
                IconButton(
                    onClick = onFlipCamera,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cameraswitch,
                        contentDescription = "Flip camera",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = onToggleMic,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (micMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = if (micMuted) "Unmute microphone" else "Mute microphone",
                        tint = if (micMuted) PulseCrimson else Color.White
                    )
                }
                IconButton(
                    onClick = { showFilterStrip = !showFilterStrip },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (showFilterStrip) PulseCyan.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Live filters",
                        tint = if (showFilterStrip) PulseCyan else Color.White
                    )
                }
            }

            IconButton(
                onClick = onShareStream,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("live_share_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share live stream",
                    tint = Color.White
                )
            }
        }

        // Floating animated heart reactions
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 84.dp)
                .size(width = 90.dp, height = 240.dp)
        ) {
            floatingHearts.forEach { heart ->
                key(heart.id) {
                    FloatingLiveHeartView(
                        heart = heart,
                        onFinished = { id -> floatingHearts.removeAll { it.id == id } }
                    )
                }
            }
        }

        // Bottom Live Chat Feed + Optional Host Filter Strip + Chat Composer + Like Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PulseCrimson.copy(alpha = 0.22f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissError) {
                            Text("OK", color = PulseCyan)
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isHost && showFilterStrip) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    items(CameraFilterPreset.entries, key = { it.id }) { preset ->
                        val selected = preset == selectedFilter
                        Surface(
                            onClick = { onSelectFilter(preset) },
                            shape = RoundedCornerShape(50),
                            color = if (selected) PulseCyan.copy(alpha = 0.24f) else PulseSurfaceElevated
                        ) {
                            Text(
                                text = preset.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) PulseCyan else Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                state = chatListState,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .heightIn(max = 190.dp)
                    .testTag("live_chat_list"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(chatMessages, key = { it.id }) { msg ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.48f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = msg.senderHandle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = PulseCyan
                            )
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = chatDraft,
                    onValueChange = { if (it.length <= 250) chatDraft = it },
                    placeholder = {
                        Text("Say something...", color = Color.White.copy(alpha = 0.7f))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submitChat() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Black.copy(alpha = 0.56f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.46f),
                        focusedBorderColor = PulseCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("live_chat_input")
                )

                IconButton(
                    onClick = { submitChat() },
                    enabled = chatDraft.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (chatDraft.isNotBlank()) PulseCyan else Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                        .testTag("live_chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send live chat",
                        tint = if (chatDraft.isNotBlank()) PulseObsidian else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSendHeart()
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("live_like_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PulseCrimson),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Send heart",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = formatCompactCount(stream?.likeCount ?: 0),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingLiveHeartView(
    heart: FloatingLiveHeart,
    onFinished: (Long) -> Unit
) {
    val yProgress = remember(heart.id) { Animatable(0f) }
    val alpha = remember(heart.id) { Animatable(1f) }

    LaunchedEffect(heart.id) {
        coroutineScope {
            launch {
                yProgress.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
            }
            launch {
                alpha.animateTo(0f, tween(1100, easing = LinearEasing))
            }
        }
        onFinished(heart.id)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset {
                IntOffset(
                    x = (24.dp.toPx() + heart.horizontalDriftPx * yProgress.value).roundToInt(),
                    y = ((1f - yProgress.value) * 200.dp.toPx()).roundToInt()
                )
            }
            .alpha(alpha.value)
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = if (heart.isCyan) PulseCyan else PulseCrimson,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun LiveEndedSummaryCard(
    title: String,
    durationSeconds: Long,
    totalLikes: Int,
    onGoLiveAgain: () -> Unit,
    onBackToHub: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseObsidian)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = PulseSurface,
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .border(1.dp, PulseOutline, RoundedCornerShape(24.dp))
                .testTag("live_ended_summary")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(PulseCrimson.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Podcasts,
                        contentDescription = null,
                        tint = PulseCrimson,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Live Broadcast Ended",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PulseTextPrimary
                )
                Text(
                    text = title.ifBlank { "ReelPulse Live" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = PulseCyan
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PulseSurfaceElevated, RoundedCornerShape(16.dp))
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatLiveDuration(durationSeconds),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PulseTextPrimary
                        )
                        Text(
                            text = "Duration",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTextSecondary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatCompactCount(totalLikes),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PulseCrimson
                        )
                        Text(
                            text = "Hearts",
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTextSecondary
                        )
                    }
                }

                Button(
                    onClick = onGoLiveAgain,
                    colors = ButtonDefaults.buttonColors(containerColor = PulseCrimson),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Go LIVE Again", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackToHub,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Live Hub", color = PulseCyan)
                    }
                    OutlinedButton(
                        onClick = onClose,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Done", color = PulseTextPrimary)
                    }
                }
            }
        }
    }
}

private fun formatLiveDuration(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val mins = safe / 60L
    val secs = safe % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
