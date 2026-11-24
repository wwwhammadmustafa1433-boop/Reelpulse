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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.recommendation.ScoredVideo
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
import com.example.ui.viewmodel.ReelPulseUiState

private val discoverCategories = listOf("All", "Tech", "Dance", "Food", "Travel", "Music", "Fitness", "Art")

@Composable
fun DiscoverScreen(
    uiState: ReelPulseUiState,
    onSearchChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val query = uiState.searchQuery.trim().lowercase()
    val filteredVideos = uiState.allScoredVideos.filter { item ->
        val matchesCategory = uiState.selectedCategoryFilter == "All" ||
            item.video.category.equals(uiState.selectedCategoryFilter, ignoreCase = true)
        val matchesQuery = query.isEmpty() ||
            item.video.title.lowercase().contains(query) ||
            item.video.caption.lowercase().contains(query) ||
            item.video.tagsCsv.lowercase().contains(query) ||
            item.video.creatorHandle.lowercase().contains(query)
        matchesCategory && matchesQuery
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

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search shorts, #tags, or @creators") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = PulseTextSecondary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear search",
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
                if (uiState.interestProfile.topTags.isNotEmpty() && query.isEmpty()) {
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
                                        onClick = { onSearchChange(tag.tag) },
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

                if (filteredVideos.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No matching clips found",
                                style = MaterialTheme.typography.titleMedium,
                                color = PulseTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try clearing your search or choosing another category.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTextSecondary
                            )
                        }
                    }
                } else {
                    items(filteredVideos, key = { it.video.id }) { item ->
                        DiscoverVideoCard(
                            scoredVideo = item,
                            onClick = { onVideoClick(item.video.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverVideoCard(
    scoredVideo: ScoredVideo,
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
