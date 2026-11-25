package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.recommendation.RecommendationEngine
import com.example.recommendation.ScoredVideo
import com.example.recommendation.TrendingTopic
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseObsidian
import com.example.ui.theme.PulseOutline
import com.example.ui.theme.PulseSurface
import com.example.ui.theme.PulseSurfaceElevated
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.theme.PulseTextSecondary
import com.example.ui.viewmodel.CreatorSummary
import com.example.ui.viewmodel.ReelPulseUiState

private val discoverCategories = listOf("All", "Tech", "Dance", "Food", "Travel", "Music", "Fitness", "Art")

@Composable
fun DiscoverScreen(
    uiState: ReelPulseUiState,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onTrendingTagSelect: (String?) -> Unit = {},
    onVideoClick: (String) -> Unit,
    onCreatorClick: (CreatorSummary) -> Unit = {},
    onCreateWithTag: (String) -> Unit = {},
    onOpenGeminiChat: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val query = uiState.searchQuery.trim().lowercase()
    val queryNormalizedTag = RecommendationEngine.normalizeTagKey(query)
    val selectedTag = uiState.selectedTrendingTag
    val selectedTagKey = selectedTag?.let { RecommendationEngine.normalizeTagKey(it) }

    val matchingTrendingSuggestions = if (query.isEmpty()) {
        emptyList()
    } else {
        uiState.trendingTopics.filter { topic ->
            val topicKey = RecommendationEngine.normalizeTagKey(topic.tag)
            topicKey.contains(queryNormalizedTag) ||
                topic.categoryHint.lowercase().contains(query)
        }.take(8)
    }

    val filteredVideos = uiState.allScoredVideos.filter { item ->
        val matchesCategory = uiState.selectedCategoryFilter == "All" ||
            item.video.category.equals(uiState.selectedCategoryFilter, ignoreCase = true)
        val matchesSelectedTag = selectedTagKey == null ||
            item.video.tags.any { RecommendationEngine.normalizeTagKey(it) == selectedTagKey }
        val matchesQuery = query.isEmpty() ||
            item.video.title.lowercase().contains(query) ||
            item.video.caption.lowercase().contains(query) ||
            item.video.tagsCsv.lowercase().contains(query) ||
            (queryNormalizedTag.isNotEmpty() && item.video.tags.any {
                RecommendationEngine.normalizeTagKey(it).contains(queryNormalizedTag)
            }) ||
            item.video.category.lowercase().contains(query) ||
            item.video.creatorName.lowercase().contains(query) ||
            item.video.creatorHandle.lowercase().contains(query)
        matchesCategory && matchesSelectedTag && matchesQuery
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
                .widthIn(max = 920.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Discover",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PulseTextPrimary
                    )
                    Text(
                        text = "Explore creators, sounds & personalized picks",
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseTextSecondary
                    )
                }
                Surface(
                    onClick = onOpenGeminiChat,
                    shape = RoundedCornerShape(50),
                    color = PulseCyan.copy(alpha = 0.14f),
                    modifier = Modifier
                        .height(48.dp)
                        .border(1.dp, PulseCyan.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .testTag("discover_gemini_chat_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Open Gemini Chat",
                            tint = PulseCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Gemini Chat",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = PulseCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search by #trending tag, topic, or @creator") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = PulseTextSecondary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty() || selectedTag != null) {
                        IconButton(onClick = {
                            onSearchChange("")
                            onTrendingTagSelect(null)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear search and tag filter",
                                tint = PulseTextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PulseSurfaceElevated,
                    unfocusedContainerColor = PulseSurface,
                    focusedBorderColor = PulseCyan,
                    unfocusedBorderColor = PulseOutline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("discover_search_field")
            )

            if (matchingTrendingSuggestions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("discover_tag_suggestions_row")
                ) {
                    items(
                        items = matchingTrendingSuggestions,
                        key = { "suggest_${RecommendationEngine.normalizeTagKey(it.tag)}" }
                    ) { topic ->
                        val topicKey = RecommendationEngine.normalizeTagKey(topic.tag)
                        val isSelected = selectedTagKey == topicKey
                        Surface(
                            onClick = { onTrendingTagSelect(topic.tag) },
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) PulseCyan.copy(alpha = 0.24f) else PulseSurfaceElevated,
                            modifier = Modifier
                                .height(40.dp)
                                .border(
                                    1.dp,
                                    if (isSelected) PulseCyan else PulseCyan.copy(alpha = 0.45f),
                                    RoundedCornerShape(50)
                                )
                                .testTag("search_tag_suggestion_$topicKey")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Tag,
                                    contentDescription = null,
                                    tint = PulseCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = topic.tag.removePrefix("#"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PulseCyan
                                )
                                if (topic.videoCount > 0) {
                                    Text(
                                        text = "(${topic.videoCount})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PulseTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(discoverCategories) { category ->
                    val selected = uiState.selectedCategoryFilter == category
                    FilterChip(
                        selected = selected,
                        onClick = { onCategorySelect(category) },
                        label = {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PulseCrimson,
                            selectedLabelColor = Color.White,
                            containerColor = PulseSurfaceElevated,
                            labelColor = PulseTextSecondary
                        ),
                        border = null,
                        modifier = Modifier.testTag("category_chip_${category.lowercase()}")
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.trendingTopics.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.testTag("discover_trending_topics_section")
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
                                        tint = PulseCrimson,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = stringResource(R.string.trending_topics_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PulseTextPrimary
                                        )
                                        Text(
                                            text = stringResource(R.string.trending_topics_subtitle),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PulseTextSecondary
                                        )
                                    }
                                }
                                if (selectedTag != null) {
                                    Surface(
                                        onClick = { onTrendingTagSelect(null) },
                                        shape = RoundedCornerShape(50),
                                        color = PulseCyan.copy(alpha = 0.16f),
                                        modifier = Modifier
                                            .border(1.dp, PulseCyan.copy(alpha = 0.5f), RoundedCornerShape(50))
                                            .testTag("clear_trending_tag_filter")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = selectedTag,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = PulseCyan
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.Clear,
                                                contentDescription = "Clear tag filter",
                                                tint = PulseCyan,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(
                                    items = uiState.trendingTopics,
                                    key = { RecommendationEngine.normalizeTagKey(it.tag) }
                                ) { topic ->
                                    val topicKey = RecommendationEngine.normalizeTagKey(topic.tag)
                                    TrendingTopicCard(
                                        topic = topic,
                                        isSelected = selectedTagKey == topicKey,
                                        onClick = { onTrendingTagSelect(topic.tag) }
                                    )
                                }
                            }
                        }
                    }
                }

                val visibleCreators = uiState.knownCreators.filter { creator ->
                    creator.creatorId !in uiState.blockedCreatorIds &&
                        (query.isEmpty() ||
                            creator.creatorName.lowercase().contains(query) ||
                            creator.creatorHandle.lowercase().contains(query))
                }
                if (visibleCreators.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Creators",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = PulseTextPrimary
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(visibleCreators, key = { it.creatorId }) { creator ->
                                    Surface(
                                        onClick = { onCreatorClick(creator) },
                                        shape = RoundedCornerShape(50),
                                        color = PulseSurfaceElevated,
                                        modifier = Modifier
                                            .height(48.dp)
                                            .border(1.dp, PulseOutline, RoundedCornerShape(50))
                                            .testTag("discover_creator_${creator.creatorId}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(start = 6.dp, end = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CreatorAvatar(
                                                avatarKey = creator.creatorAvatarKey,
                                                displayName = creator.creatorName,
                                                size = 34.dp,
                                                borderColor = PulseCyan
                                            )
                                            Column {
                                                Text(
                                                    text = creator.creatorName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PulseTextPrimary
                                                )
                                                Text(
                                                    text = creator.creatorHandle,
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
                }

                if (uiState.interestProfile.topTags.isNotEmpty() && query.isEmpty() && selectedTag == null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    text = "Trending In Your Taste Graph",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = PulseCyan
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.interestProfile.topTags) { tag ->
                                    Surface(
                                        onClick = { onTrendingTagSelect(tag.tag) },
                                        shape = RoundedCornerShape(50),
                                        color = PulseCyan.copy(alpha = 0.14f),
                                        modifier = Modifier.border(
                                            1.dp,
                                            PulseCyan.copy(alpha = 0.35f),
                                            RoundedCornerShape(50)
                                        )
                                    ) {
                                        Text(
                                            text = tag.tag,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = PulseCyan,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedTag != null || query.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val summary = buildString {
                                append("${filteredVideos.size} ")
                                append(if (filteredVideos.size == 1) "video" else "videos")
                                if (selectedTag != null) {
                                    append(" tagged $selectedTag")
                                }
                                if (query.isNotEmpty()) {
                                    append(" matching \"${uiState.searchQuery.trim()}\"")
                                }
                            }
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PulseCyan
                            )
                        }
                    }
                }

                if (filteredVideos.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (selectedTag != null) {
                                    "No videos tagged $selectedTag yet"
                                } else {
                                    "No matching clips found"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = PulseTextPrimary
                            )
                            Text(
                                text = if (selectedTag != null) {
                                    "Be the first creator to post a short with $selectedTag."
                                } else {
                                    "Try clearing your search or choosing another trending tag."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            if (selectedTag != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { onCreateWithTag(selectedTag) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PulseCrimson,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("post_with_trending_tag_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.size(6.dp))
                                    Text(
                                        text = "Post with $selectedTag",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(filteredVideos, key = { it.video.id }) { item ->
                        DiscoverVideoCard(
                            scoredVideo = item,
                            highlightedTagKey = selectedTagKey ?: queryNormalizedTag.takeIf { it.isNotEmpty() },
                            onClick = { onVideoClick(item.video.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingTopicCard(
    topic: TrendingTopic,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val topicKey = RecommendationEngine.normalizeTagKey(topic.tag)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = when {
            isSelected -> PulseCyan.copy(alpha = 0.22f)
            topic.isHot -> PulseCrimson.copy(alpha = 0.13f)
            else -> PulseSurfaceElevated
        },
        modifier = Modifier
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = when {
                    isSelected -> PulseCyan
                    topic.isHot -> PulseCrimson.copy(alpha = 0.45f)
                    else -> PulseOutline
                },
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("trending_topic_chip_$topicKey")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (topic.isHot) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = "Hot trending topic",
                        tint = if (isSelected) PulseCyan else PulseCrimson,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = topic.tag,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) PulseCyan else PulseTextPrimary
                )
            }
            Text(
                text = if (topic.videoCount > 0) {
                    val clipLabel = if (topic.videoCount == 1) "1 clip" else "${topic.videoCount} clips"
                    "$clipLabel • ${formatCompactCount(topic.engagementScore.toInt())} pulse"
                } else {
                    "${topic.categoryHint} • Be first"
                },
                style = MaterialTheme.typography.labelSmall,
                color = PulseTextSecondary
            )
        }
    }
}

@Composable
private fun DiscoverVideoCard(
    scoredVideo: ScoredVideo,
    highlightedTagKey: String?,
    onClick: () -> Unit
) {
    val video = scoredVideo.video
    val accent = Color(video.accentHex)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.42f), PulseSurface, PulseObsidian)
                )
            )
            .border(1.dp, PulseOutline, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("discover_card_${video.id}")
    ) {
        // Top row: Match badge + play indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.58f)
            ) {
                Text(
                    text = "${scoredVideo.matchScore}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = PulseCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(16.dp)
                )
            }
        }

        // Center creator portrait
        CreatorAvatar(
            avatarKey = video.creatorAvatarKey,
            displayName = video.creatorName,
            size = 56.dp,
            borderColor = accent,
            modifier = Modifier.align(Alignment.Center)
        )

        // Bottom info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (video.tags.isNotEmpty()) {
                val orderedTags = if (highlightedTagKey != null) {
                    video.tags.sortedByDescending {
                        RecommendationEngine.normalizeTagKey(it).contains(highlightedTagKey)
                    }
                } else {
                    video.tags
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    orderedTags.take(2).forEach { tag ->
                        val isHit = highlightedTagKey != null &&
                            RecommendationEngine.normalizeTagKey(tag).contains(highlightedTagKey)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isHit) PulseCyan.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.48f)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isHit) PulseCyan else Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = video.creatorHandle,
                    style = MaterialTheme.typography.bodySmall,
                    color = PulseTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = PulseCrimson,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = formatCompactCount(scoredVideo.totalLikesCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}
