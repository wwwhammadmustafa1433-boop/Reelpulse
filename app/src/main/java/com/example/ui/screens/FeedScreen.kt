package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CommentEntity
import com.example.recommendation.ScoredVideo
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.components.RecommendationBreakdownSheet
import com.example.ui.components.VideoFeedPlayer
import com.example.ui.components.VideoOverlayControls
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseObsidian
import com.example.ui.theme.PulseSurfaceElevated
import com.example.ui.theme.PulseTextSecondary
import com.example.ui.viewmodel.FeedTab
import com.example.ui.viewmodel.ReelPulseUiState
import com.example.ui.viewmodel.ReelPulseViewModel

@Composable
fun FeedScreen(
    uiState: ReelPulseUiState,
    activeComments: List<CommentEntity>,
    viewModel: ReelPulseViewModel,
    onNavigateToDiscover: (String?) -> Unit,
    onNavigateToAlgorithmLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeFeed = when (uiState.selectedFeedTab) {
        FeedTab.FOR_YOU -> uiState.forYouFeed
        FeedTab.FOLLOWING -> uiState.followingFeed
    }

    var isMuted by remember { mutableStateOf(false) }
    var commentsTarget by remember { mutableStateOf<ScoredVideo?>(null) }
    var explainTarget by remember { mutableStateOf<ScoredVideo?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseObsidian)
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = PulseCrimson,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (activeFeed.isEmpty() && uiState.selectedFeedTab == FeedTab.FOLLOWING) {
            EmptyFollowingState(
                onExploreForYou = { viewModel.selectFeedTab(FeedTab.FOR_YOU) },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp)
            )
        } else if (activeFeed.isNotEmpty()) {
            val pagerState = rememberPagerState(
                initialPage = 0,
                pageCount = { activeFeed.size }
            )

            // Handle jumping directly to a specific video from Discover or Profile
            LaunchedEffect(uiState.activeVideoJumpId, activeFeed) {
                val jumpId = uiState.activeVideoJumpId ?: return@LaunchedEffect
                val index = activeFeed.indexOfFirst { it.video.id == jumpId }
                if (index >= 0) {
                    pagerState.scrollToPage(index)
                }
                viewModel.clearJumpRequest()
            }

            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                key = { idx -> activeFeed[idx].video.id },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("video_vertical_pager")
            ) { page ->
                val item = activeFeed[page]
                val isCurrentPage = pagerState.currentPage == page &&
                    commentsTarget == null &&
                    explainTarget == null

                Box(modifier = Modifier.fillMaxSize()) {
                    VideoFeedPlayer(
                        video = item.video,
                        isActive = isCurrentPage,
                        isMuted = isMuted,
                        onDoubleTapLike = { viewModel.ensureLiked(item) },
                        onWatchProgressRecorded = { video, watchMs ->
                            viewModel.recordWatchSession(video, watchMs)
                        }
                    )

                    VideoOverlayControls(
                        scoredVideo = item,
                        isMuted = isMuted,
                        onToggleMute = { isMuted = !isMuted },
                        onLikeClick = { viewModel.toggleLike(item) },
                        onCommentClick = {
                            commentsTarget = item
                            viewModel.openComments(item.video.id)
                        },
                        onSaveClick = { viewModel.toggleSave(item) },
                        onFollowClick = { viewModel.toggleFollow(item) },
                        onExplainRecommendationClick = { explainTarget = item },
                        onTagClick = { tag -> onNavigateToDiscover(tag) }
                    )
                }
            }
        }

        // Top floating overlay bar: Following | For You + Quick Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onNavigateToAlgorithmLab,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.42f), CircleShape)
                    .testTag("feed_algorithm_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "Open recommendation engine",
                    tint = PulseCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeedTabPill(
                        label = stringResource(R.string.tab_following),
                        selected = uiState.selectedFeedTab == FeedTab.FOLLOWING,
                        testTag = "tab_following",
                        onClick = { viewModel.selectFeedTab(FeedTab.FOLLOWING) }
                    )
                    FeedTabPill(
                        label = stringResource(R.string.tab_for_you),
                        selected = uiState.selectedFeedTab == FeedTab.FOR_YOU,
                        testTag = "tab_for_you",
                        onClick = { viewModel.selectFeedTab(FeedTab.FOR_YOU) }
                    )
                }
            }

            IconButton(
                onClick = { onNavigateToDiscover(null) },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.42f), CircleShape)
                    .testTag("feed_search_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Discover videos",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        commentsTarget?.let { target ->
            CommentsBottomSheet(
                videoTitle = target.video.title,
                comments = activeComments,
                onPostComment = { text -> viewModel.postComment(target.video.id, text) },
                onDismiss = {
                    commentsTarget = null
                    viewModel.openComments(null)
                }
            )
        }

        explainTarget?.let { target ->
            val latestScored = uiState.forYouFeed.find { it.video.id == target.video.id } ?: target
            RecommendationBreakdownSheet(
                scoredVideo = latestScored,
                onOpenAlgorithmLab = onNavigateToAlgorithmLab,
                onDismiss = { explainTarget = null }
            )
        }
    }
}

@Composable
private fun FeedTabPill(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) PulseCrimson else Color.Transparent,
        label = "tab_pill_bg"
    )
    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun EmptyFollowingState(
    onExploreForYou: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = PulseSurfaceElevated,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.PeopleOutline,
                    contentDescription = null,
                    tint = PulseCyan,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Text(
            text = stringResource(R.string.empty_following_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.empty_following_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = PulseTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onExploreForYou,
            colors = ButtonDefaults.buttonColors(containerColor = PulseCrimson),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Text("Switch to For You")
        }
    }
}
