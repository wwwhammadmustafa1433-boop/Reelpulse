package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.example.R
import com.example.camera.CameraFilterPreset
import com.example.data.remote.AccountVisibility
import com.example.recommendation.RecommendationEngine
import com.example.recommendation.TrendingTopic
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseObsidian
import com.example.ui.theme.PulseOutline
import com.example.ui.theme.PulseSurface
import com.example.ui.theme.PulseSurfaceElevated
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.theme.PulseTextSecondary

private val createCategories = listOf("Tech", "Dance", "Food", "Travel", "Music", "Fitness", "Art")
const val SAMPLE_STUDIO_VIDEO_URL =
    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4"

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateScreen(
    draftVideoUri: String?,
    draftFilterPreset: CameraFilterPreset,
    draftSourceType: String,
    draftTagsCsv: String = "",
    trendingTopics: List<TrendingTopic> = emptyList(),
    accountVisibility: AccountVisibility = AccountVisibility.PUBLIC,
    isPublishing: Boolean,
    publishError: String?,
    onOpenCamera: () -> Unit,
    onGoLive: () -> Unit = {},
    onGalleryVideoPicked: (Uri) -> Unit,
    onUseSampleClip: () -> Unit,
    onClearVideo: () -> Unit = {},
    onFilterSelected: (CameraFilterPreset) -> Unit,
    onTagsChange: (String) -> Unit = {},
    onToggleTrendingTag: (String) -> Unit = {},
    onPublish: (
        title: String,
        caption: String,
        category: String,
        tagsCsv: String,
        soundTitle: String
    ) -> Unit,
    onDismissPublishError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tech") }
    var soundTitle by remember { mutableStateOf("Original Studio Sound") }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    val activeTags = remember(draftTagsCsv) {
        draftTagsCsv.split(",", " ")
            .map { RecommendationEngine.normalizeTagDisplay(it) }
            .filter { it.isNotEmpty() && it != "#" }
            .distinctBy { RecommendationEngine.normalizeTagKey(it) }
            .take(10)
    }
    val activeTagKeys = remember(activeTags) {
        activeTags.map { RecommendationEngine.normalizeTagKey(it) }.toSet()
    }

    // Zero-permission Android Photo Picker for Video selection (Google Play compliant)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            validationMessage = null
            onGalleryVideoPicked(uri)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseObsidian)
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.create_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PulseTextPrimary
                    )
                    Text(
                        text = "Record a clip or select a video from your device, add a title and optional tags, then upload.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseTextSecondary
                    )
                }

                val isPersonal = accountVisibility == AccountVisibility.PERSONAL
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isPersonal) PulseCrimson.copy(alpha = 0.16f) else PulseCyan.copy(alpha = 0.14f),
                    modifier = Modifier.border(
                        1.dp,
                        if (isPersonal) PulseCrimson.copy(alpha = 0.45f) else PulseCyan.copy(alpha = 0.45f),
                        RoundedCornerShape(50)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (isPersonal) Icons.Filled.Lock else Icons.Filled.Public,
                            contentDescription = null,
                            tint = if (isPersonal) PulseCrimson else PulseCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isPersonal) "Personal" else "Public",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPersonal) PulseCrimson else PulseCyan
                        )
                    }
                }
            }

            if (publishError != null || validationMessage != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PulseCrimson.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PulseCrimson.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = publishError ?: validationMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = PulseCrimson,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            validationMessage = null
                            onDismissPublishError()
                        }) {
                            Text("Dismiss", color = Color.White)
                        }
                    }
                }
            }

            // Video Preview Container
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PulseSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .border(1.dp, PulseOutline, RoundedCornerShape(20.dp))
            ) {
                val previewUrl = draftVideoUri
                if (previewUrl != null) {
                    val previewPlayer = remember(previewUrl, draftFilterPreset) {
                        ExoPlayer.Builder(context).build().apply {
                            repeatMode = Player.REPEAT_MODE_ONE
                            volume = 0f
                            val effects = draftFilterPreset.toMedia3Effects()
                            if (effects.isNotEmpty()) {
                                setVideoEffects(effects)
                            }
                            setMediaItem(MediaItem.fromUri(previewUrl))
                            prepare()
                            playWhenReady = true
                        }
                    }
                    DisposableEffect(previewPlayer) {
                        onDispose { previewPlayer.release() }
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        ContentFrame(
                            player = previewPlayer,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp)),
                            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                            contentScale = ContentScale.Crop
                        )

                        draftFilterPreset.tintOverlayColor?.let { tint ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(tint)
                            )
                        }
                        draftFilterPreset.vignetteBrush()?.let { vignette ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(vignette)
                            )
                        }
                        if (draftFilterPreset.hasScanlines) {
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

                        // Source + Filter Badges
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.Black.copy(alpha = 0.65f)
                            ) {
                                Text(
                                    text = when (draftSourceType) {
                                        "camera" -> "App Camera"
                                        "sample" -> "Studio Sample"
                                        else -> "Device Gallery"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            if (draftFilterPreset != CameraFilterPreset.NORMAL) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = PulseCyan.copy(alpha = 0.22f)
                                ) {
                                    Text(
                                        text = "Filter: ${draftFilterPreset.displayName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PulseCyan,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                        // Clear selected video button
                        Surface(
                            onClick = onClearVideo,
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .size(36.dp)
                                .testTag("clear_draft_video_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove selected video",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = null,
                            tint = PulseCyan,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Record or select a video to upload",
                            style = MaterialTheme.typography.titleSmall,
                            color = PulseTextPrimary
                        )
                        Text(
                            text = "Open the App Camera with live filters or pick a video from your gallery",
                            style = MaterialTheme.typography.bodySmall,
                            color = PulseTextSecondary
                        )
                    }
                }
            }

            // Primary Capture & Gallery Selection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        validationMessage = null
                        onOpenCamera()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulseCrimson,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("open_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Record Video", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        validationMessage = null
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PulseSurfaceElevated,
                        contentColor = PulseTextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("pick_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.VideoLibrary,
                        contentDescription = null,
                        tint = PulseCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Select Video", fontWeight = FontWeight.SemiBold)
                }
            }

            // Go LIVE Broadcast Entry
            Surface(
                onClick = {
                    validationMessage = null
                    onGoLive()
                },
                shape = RoundedCornerShape(14.dp),
                color = PulseCrimson.copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, PulseCrimson.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .testTag("create_go_live_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Podcasts,
                        contentDescription = null,
                        tint = PulseCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Go LIVE • Stream with Live Filters",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (draftVideoUri == null) {
                OutlinedButton(
                    onClick = {
                        validationMessage = null
                        onUseSampleClip()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("use_sample_clip_button")
                ) {
                    Text("Load Studio Sample Clip", color = PulseCyan)
                }
            }

            // Filter Picker Strip (works on both Camera and Gallery previews)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = PulseCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Video Filter (${draftFilterPreset.displayName})",
                        style = MaterialTheme.typography.labelLarge,
                        color = PulseTextPrimary
                    )
                }
                LazyRow(
                    contentPadding = PaddingValues(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(CameraFilterPreset.entries, key = { it.id }) { preset ->
                        val selected = preset == draftFilterPreset
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onFilterSelected(preset) }
                                .padding(4.dp)
                                .testTag("create_filter_${preset.id}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
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
            }

            // Title Field (prominent, required)
            OutlinedTextField(
                value = title,
                onValueChange = {
                    if (it.length <= 100) {
                        title = it
                        validationMessage = null
                    }
                },
                label = { Text("Title *") },
                placeholder = { Text("Give your video a title...") },
                supportingText = {
                    Text(
                        text = "${title.length}/100 • Required to upload",
                        color = PulseTextSecondary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PulseSurfaceElevated,
                    unfocusedContainerColor = PulseSurface,
                    focusedBorderColor = PulseCrimson,
                    unfocusedBorderColor = PulseOutline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_title_input")
            )

            OutlinedTextField(
                value = caption,
                onValueChange = {
                    if (it.length <= 500) {
                        caption = it
                        validationMessage = null
                    }
                },
                label = { Text("Caption (optional)") },
                placeholder = { Text("Add a description or leave blank to use your title...") },
                supportingText = {
                    Text(
                        text = "${caption.length}/500 • Optional",
                        color = PulseTextSecondary
                    )
                },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PulseSurfaceElevated,
                    unfocusedContainerColor = PulseSurface,
                    focusedBorderColor = PulseCyan,
                    unfocusedBorderColor = PulseOutline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_caption_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Category (used by For You ranker)",
                    style = MaterialTheme.typography.labelLarge,
                    color = PulseTextPrimary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    createCategories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
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
            }

            if (trendingTopics.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("create_trending_topics_section")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = PulseCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.create_trending_topics_label),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = PulseTextPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PulseSurfaceElevated
                        ) {
                            Text(
                                text = "${activeTags.size}/10 tags",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeTags.size >= 10) PulseCrimson else PulseCyan,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    val createTopics = remember(trendingTopics) {
                        trendingTopics
                            .sortedWith(
                                compareByDescending<TrendingTopic> { it.isHot }
                                    .thenByDescending { it.isCurated }
                                    .thenByDescending { it.engagementScore }
                            )
                            .take(16)
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(
                            items = createTopics,
                            key = { RecommendationEngine.normalizeTagKey(it.tag) }
                        ) { topic ->
                            val tagKey = RecommendationEngine.normalizeTagKey(topic.tag)
                            val isSelected = tagKey in activeTagKeys
                            Surface(
                                onClick = { onToggleTrendingTag(topic.tag) },
                                shape = RoundedCornerShape(50),
                                color = when {
                                    isSelected -> PulseCyan.copy(alpha = 0.22f)
                                    topic.isHot -> PulseCrimson.copy(alpha = 0.14f)
                                    else -> PulseSurfaceElevated
                                },
                                modifier = Modifier
                                    .height(48.dp)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = when {
                                            isSelected -> PulseCyan
                                            topic.isHot -> PulseCrimson.copy(alpha = 0.45f)
                                            else -> PulseOutline
                                        },
                                        shape = RoundedCornerShape(50)
                                    )
                                    .testTag("create_trending_tag_$tagKey")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    when {
                                        isSelected -> Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Tagged",
                                            tint = PulseCyan,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        topic.isHot -> Icon(
                                            imageVector = Icons.Filled.LocalFireDepartment,
                                            contentDescription = "Hot topic",
                                            tint = PulseCrimson,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    Text(
                                        text = topic.tag,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected || topic.isHot) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) PulseCyan else PulseTextPrimary
                                    )
                                    if (topic.videoCount > 0) {
                                        Text(
                                            text = formatCompactCount(topic.videoCount),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PulseTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = draftTagsCsv,
                onValueChange = onTagsChange,
                label = { Text("Tags (optional, comma-separated)") },
                placeholder = { Text("#NeonVibes, #tech (leave blank for none)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PulseSurfaceElevated,
                    unfocusedContainerColor = PulseSurface,
                    focusedBorderColor = PulseCyan,
                    unfocusedBorderColor = PulseOutline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_tags_input")
            )

            if (activeTags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeTags.forEach { tag ->
                        Surface(
                            onClick = { onToggleTrendingTag(tag) },
                            shape = RoundedCornerShape(50),
                            color = PulseSurfaceElevated,
                            modifier = Modifier.border(
                                1.dp,
                                PulseCyan.copy(alpha = 0.35f),
                                RoundedCornerShape(50)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PulseCyan,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove $tag",
                                    tint = PulseTextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = soundTitle,
                onValueChange = { if (it.length <= 100) soundTitle = it },
                label = { Text("Sound / Audio Credit") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PulseSurfaceElevated,
                    unfocusedContainerColor = PulseSurface,
                    focusedBorderColor = PulseCyan,
                    unfocusedBorderColor = PulseOutline
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (draftVideoUri == null) {
                        validationMessage = "Please record a video or select one from your device first."
                    } else if (title.trim().isEmpty()) {
                        validationMessage = "Please enter a title before uploading."
                    } else {
                        validationMessage = null
                        onPublish(title, caption, selectedCategory, draftTagsCsv, soundTitle)
                    }
                },
                enabled = !isPublishing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PulseCrimson,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("publish_video_button")
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = "Saving to Firestore...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.CloudDone,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.create_publish),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
