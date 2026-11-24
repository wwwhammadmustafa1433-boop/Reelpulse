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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseObsidian
import com.example.ui.theme.PulseOutline
import com.example.ui.theme.PulseSurface
import com.example.ui.theme.PulseSurfaceElevated
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.theme.PulseTextSecondary
import com.example.ui.viewmodel.ReelPulseUiState

@Composable
fun ProfileScreen(
    uiState: ReelPulseUiState,
    onEditProfile: (String, String, String) -> Unit,
    onVideoClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }

    val myPosts = uiState.allScoredVideos.filter { it.video.isUserCreated }
    val likedVideos = uiState.allScoredVideos.filter { it.isLiked }
    val savedVideos = uiState.allScoredVideos.filter { it.isSaved }
    val watchedIdsOrdered = uiState.watchHistory.map { it.videoId }.distinct()
    val videoMap = uiState.allScoredVideos.associateBy { it.video.id }
    val historyVideos = watchedIdsOrdered.mapNotNull { videoMap[it] }

    val displayedVideos = when (selectedTabIndex) {
        0 -> myPosts
        1 -> likedVideos
        2 -> savedVideos
        else -> historyVideos
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
                .widthIn(max = 820.dp)
        ) {
            // Profile Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CreatorAvatar(
                    avatarKey = "local_user",
                    displayName = uiState.userProfile.displayName,
                    size = 82.dp,
                    borderColor = PulseCrimson
                )
                Text(
                    text = uiState.userProfile.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PulseTextPrimary
                )
                Text(
                    text = uiState.userProfile.handle,
                    style = MaterialTheme.typography.labelLarge,
                    color = PulseCyan
                )
                if (uiState.userProfile.bio.isNotBlank()) {
                    Text(
                        text = uiState.userProfile.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStat(count = uiState.followedCreatorIds.size, label = "Following")
                    ProfileStat(count = uiState.likedIds.size, label = "Liked")
                    ProfileStat(count = uiState.watchHistory.size, label = "Watched")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(R.string.profile_edit))
                    }
                    Button(
                        onClick = onCreateClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PulseCrimson),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("New Short")
                    }
                }
            }

            // Tabs: Posts | Liked | Saved | History
            val tabs = listOf(
                "Posts" to Icons.Filled.VideoLibrary,
                "Liked" to Icons.Filled.Favorite,
                "Saved" to Icons.Filled.Bookmark,
                "History" to Icons.Filled.History
            )
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = PulseObsidian,
                contentColor = PulseTextPrimary,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = PulseCrimson
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, (label, icon) ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp)) },
                        text = { Text(label, style = MaterialTheme.typography.labelMedium) },
                        selectedContentColor = PulseTextPrimary,
                        unselectedContentColor = PulseTextSecondary
                    )
                }
            }

            if (displayedVideos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when (selectedTabIndex) {
                            0 -> "No shorts posted yet"
                            1 -> "No liked shorts yet"
                            2 -> "No saved shorts yet"
                            else -> "No watch history yet"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = PulseTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when (selectedTabIndex) {
                            0 -> "Tap New Short to share a video clip."
                            1 -> "Double-tap any video in For You to like it."
                            2 -> "Bookmark clips to find them here anytime."
                            else -> "Watch clips in For You to build your history."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 118.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedVideos, key = { "${selectedTabIndex}_${it.video.id}" }) { item ->
                        ProfileVideoTile(
                            scoredVideo = item,
                            onClick = { onVideoClick(item.video.id) }
                        )
                    }
                }
            }
        }

        if (showEditDialog) {
            EditProfileDialog(
                initialName = uiState.userProfile.displayName,
                initialHandle = uiState.userProfile.handle,
                initialBio = uiState.userProfile.bio,
                onSave = { name, handle, bio ->
                    onEditProfile(name, handle, bio)
                    showEditDialog = false
                },
                onDismiss = { showEditDialog = false }
            )
        }
    }
}

@Composable
private fun ProfileStat(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
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
private fun ProfileVideoTile(
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

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialHandle: String,
    initialBio: String,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var handle by remember { mutableStateOf(initialHandle) }
    var bio by remember { mutableStateOf(initialBio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PulseSurface,
        title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Handle") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, handle, bio) },
                colors = ButtonDefaults.buttonColors(containerColor = PulseCrimson)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
