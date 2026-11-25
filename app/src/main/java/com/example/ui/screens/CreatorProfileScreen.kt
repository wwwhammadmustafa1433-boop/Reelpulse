package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.recommendation.ScoredVideo
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.formatCompactCount
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseEmerald
import com.example.ui.theme.PulseObsidian
import com.example.ui.theme.PulseOutline
import com.example.ui.theme.PulseSurface
import com.example.ui.theme.PulseSurfaceElevated
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.theme.PulseTextSecondary

@Composable
fun CreatorProfileScreen(
    creatorId: String,
    creatorName: String,
    creatorHandle: String,
    creatorAvatarKey: String,
    creatorVideos: List<ScoredVideo>,
    isFollowing: Boolean,
    isBlocked: Boolean,
    isUpdatingBlock: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onToggleFollow: () -> Unit,
    onBlockUser: () -> Unit,
    onUnblockUser: () -> Unit,
    onVideoClick: (String) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBlockDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val totalLikes = creatorVideos.sumOf { it.totalLikesCount }
    val primaryCategory = creatorVideos.firstOrNull()?.video?.category ?: "Creator"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PulseObsidian)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp)
        ) {
            // Top bar with Back navigation and quick Block/Unblock action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("creator_profile_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PulseTextPrimary
                    )
                }

                Text(
                    text = creatorHandle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PulseTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(
                    onClick = {
                        if (isBlocked) onUnblockUser() else showBlockDialog = true
                    },
                    enabled = !isUpdatingBlock,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("creator_profile_block_icon_button")
                ) {
                    Icon(
                        imageVector = if (isBlocked) Icons.Filled.Shield else Icons.Filled.Block,
                        contentDescription = if (isBlocked) "Unblock user" else "Block user",
                        tint = if (isBlocked) PulseCyan else PulseCrimson
                    )
                }
            }

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PulseCrimson.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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

            // Creator Profile Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CreatorAvatar(
                    avatarKey = creatorAvatarKey,
                    displayName = creatorName,
                    size = 84.dp,
                    borderColor = if (isBlocked) PulseTextSecondary else PulseCyan
                )

                Text(
                    text = creatorName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PulseTextPrimary
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = creatorHandle,
                        style = MaterialTheme.typography.labelLarge,
                        color = PulseCyan
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = PulseSurfaceElevated
                    ) {
                        Text(
                            text = primaryCategory,
                            style = MaterialTheme.typography.labelSmall,
                            color = PulseTextSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                // Stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CreatorStatItem(
                        value = if (isBlocked) "0" else creatorVideos.size.toString(),
                        label = "Visible Shorts"
                    )
                    CreatorStatItem(
                        value = if (isBlocked) "0" else formatCompactCount(totalLikes),
                        label = "Total Likes"
                    )
                    CreatorStatItem(
                        value = when {
                            isBlocked -> "Blocked"
                            isFollowing -> "Following"
                            else -> "Public"
                        },
                        label = "Status"
                    )
                }

                // Primary Actions: Follow + Block User
                if (isBlocked) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PulseCrimson.copy(alpha = 0.14f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PulseCrimson.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Block,
                                    contentDescription = null,
                                    tint = PulseCrimson,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.user_blocked_title, creatorHandle),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTextPrimary
                                )
                            }
                            Text(
                                text = stringResource(R.string.user_blocked_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = PulseTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onUnblockUser,
                                    enabled = !isUpdatingBlock,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("unblock_user_button")
                                ) {
                                    if (isUpdatingBlock) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = PulseCyan,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = stringResource(R.string.unblock_user),
                                            color = PulseCyan,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Button(
                                    onClick = onBack,
                                    colors = ButtonDefaults.buttonColors(containerColor = PulseSurfaceElevated),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("back_to_feed_after_block_button")
                                ) {
                                    Text("Back to Feed", color = PulseTextPrimary)
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onToggleFollow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) PulseSurfaceElevated else PulseCrimson,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("creator_profile_follow_button")
                        ) {
                            Icon(
                                imageVector = if (isFollowing) Icons.Filled.Check else Icons.Filled.PersonAdd,
                                contentDescription = null,
                                tint = if (isFollowing) PulseEmerald else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { showBlockDialog = true },
                            enabled = !isUpdatingBlock,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("block_user_button")
                        ) {
                            if (isUpdatingBlock) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = PulseCrimson,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Block,
                                    contentDescription = null,
                                    tint = PulseCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = stringResource(R.string.block_user),
                                    color = PulseCrimson,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Creator Shorts Grid (Hidden when user is blocked)
            if (isBlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(PulseSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Block,
                                contentDescription = null,
                                tint = PulseCrimson,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            text = "Content hidden",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PulseTextPrimary
                        )
                        Text(
                            text = "Videos from $creatorHandle will not appear in your feed while blocked.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PulseTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (creatorVideos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No videos posted by $creatorHandle yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PulseTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 118.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(creatorVideos, key = { it.video.id }) { item ->
                        CreatorVideoGridTile(
                            scoredVideo = item,
                            onClick = { onVideoClick(item.video.id) }
                        )
                    }
                }
            }
        }

        if (showBlockDialog) {
            BlockUserConfirmationDialog(
                creatorName = creatorName,
                creatorHandle = creatorHandle,
                onConfirm = {
                    showBlockDialog = false
                    onBlockUser()
                },
                onDismiss = { showBlockDialog = false }
            )
        }
    }
}

@Composable
fun BlockUserConfirmationDialog(
    creatorName: String,
    creatorHandle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PulseSurface,
        icon = {
            Icon(
                imageVector = Icons.Filled.Block,
                contentDescription = null,
                tint = PulseCrimson,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Block $creatorName?",
                fontWeight = FontWeight.Bold,
                color = PulseTextPrimary
            )
        },
        text = {
            Text(
                text = "$creatorHandle's videos and live streams will no longer appear in your For You, Following, or Discover feeds. You can unblock them anytime from your Profile.",
                style = MaterialTheme.typography.bodyMedium,
                color = PulseTextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PulseCrimson),
                modifier = Modifier.testTag("confirm_block_user_button")
            ) {
                Text("Block User", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PulseTextSecondary)
            }
        }
    )
}

@Composable
private fun CreatorStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PulseTextPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = PulseTextSecondary
        )
    }
}

@Composable
private fun CreatorVideoGridTile(
    scoredVideo: ScoredVideo,
    onClick: () -> Unit
) {
    val video = scoredVideo.video
    val accent = Color(video.accentHex)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.45f), PulseSurfaceElevated)
                )
            )
            .border(1.dp, PulseOutline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag("creator_video_tile_${video.id}")
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Play",
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(28.dp)
        )
        Text(
            text = video.title,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
