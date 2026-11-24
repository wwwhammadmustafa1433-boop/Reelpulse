package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.recommendation.ScoredVideo
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.PulseObsidian

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoOverlayControls(
    scoredVideo: ScoredVideo,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onSaveClick: () -> Unit,
    onFollowClick: () -> Unit,
    onExplainRecommendationClick: () -> Unit,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val video = scoredVideo.video

    val likeScale by animateFloatAsState(
        targetValue = if (scoredVideo.isLiked) 1.18f else 1.0f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 450f),
        label = "like_scale"
    )

    val discTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val discRotation by discTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_angle"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 12.dp, bottom = 18.dp)
    ) {
        // Right-side vertical TikTok-style action rail
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Creator Avatar + Follow quick-badge
            Box(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onFollowClick)
                    .testTag("follow_creator_button"),
                contentAlignment = Alignment.BottomCenter
            ) {
                CreatorAvatar(
                    avatarKey = video.creatorAvatarKey,
                    displayName = video.creatorName,
                    size = 50.dp,
                    borderColor = if (scoredVideo.isFollowed) PulseEmerald else Color.White
                )
                if (!video.isUserCreated) {
                    Box(
                        modifier = Modifier
                            .offset(y = 8.dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (scoredVideo.isFollowed) PulseEmerald else PulseCrimson)
                            .border(1.5.dp, PulseObsidian, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (scoredVideo.isFollowed) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = if (scoredVideo.isFollowed) "Following" else "Follow creator",
                            tint = if (scoredVideo.isFollowed) PulseObsidian else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            ActionRailButton(
                icon = if (scoredVideo.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = formatCompactCount(scoredVideo.totalLikesCount),
                tint = if (scoredVideo.isLiked) PulseCrimson else Color.White,
                contentDescription = "Like video",
                iconScale = likeScale,
                testTag = "like_button",
                onClick = onLikeClick
            )

            ActionRailButton(
                icon = if (scoredVideo.totalCommentsCount > 0) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                label = formatCompactCount(scoredVideo.totalCommentsCount),
                tint = Color.White,
                contentDescription = "Open comments",
                testTag = "comment_button",
                onClick = onCommentClick
            )

            ActionRailButton(
                icon = if (scoredVideo.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                label = if (scoredVideo.isSaved) "Saved" else "Save",
                tint = if (scoredVideo.isSaved) PulseCyan else Color.White,
                contentDescription = "Save video",
                testTag = "save_button",
                onClick = onSaveClick
            )

            ActionRailButton(
                icon = Icons.Filled.Share,
                label = formatCompactCount(video.sharesCount),
                tint = Color.White,
                contentDescription = "Share video",
                testTag = "share_button",
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Watch \"${video.title}\" by ${video.creatorHandle} on ReelPulse! ${video.tagsCsv}"
                        )
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Short"))
                }
            )

            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .testTag("mute_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute audio" else "Mute audio",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Spinning Vinyl Sound Disc
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .rotate(discRotation)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(Color(0xFF1F2437), Color(0xFF3A415E), Color(0xFF13151F), Color(0xFF3A415E))
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CreatorAvatar(
                    avatarKey = video.creatorAvatarKey,
                    displayName = video.creatorName,
                    size = 20.dp,
                    borderColor = Color.Transparent
                )
            }
        }

        // Bottom-left creator info, caption, tags, sound & recommendation match pill
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tappable Personalized Match Pill
            Surface(
                onClick = onExplainRecommendationClick,
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.58f),
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = PulseCyan.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(50)
                    )
                    .testTag("recommendation_reason_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = PulseCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${scoredVideo.matchScore}% Match",
                        style = MaterialTheme.typography.labelSmall,
                        color = PulseCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• ${scoredVideo.primaryReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = video.creatorHandle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = video.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = video.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.95f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (video.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    video.tags.take(4).forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelMedium,
                            color = PulseCyan,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onTagClick(tag) }
                                .padding(horizontal = 2.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = video.soundTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ActionRailButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    contentDescription: String,
    testTag: String,
    iconScale: Float = 1f,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(32.dp)
                .scale(iconScale)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}
