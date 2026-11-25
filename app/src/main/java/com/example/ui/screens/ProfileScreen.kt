package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.remote.AccountVisibility
import com.example.data.remote.BlockedUser
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
import com.example.ui.viewmodel.CreatorSummary
import com.example.ui.viewmodel.ReelPulseUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ReelPulseUiState,
    onEditProfile: (String, String, String?, String) -> Unit,
    onAccountVisibilityChange: (AccountVisibility) -> Unit,
    onVideoClick: (String) -> Unit,
    onCreateClick: () -> Unit,
    onBlockUser: (CreatorSummary) -> Unit = {},
    onUnblockUser: (String) -> Unit = {},
    onOpenCreatorProfile: (CreatorSummary) -> Unit = {},
    onLogoutClick: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showBlockedUsersSheet by remember { mutableStateOf(false) }

    val firebaseEmail = uiState.userAccount?.email ?: Firebase.auth.currentUser?.email
    val mobileNumber = uiState.userAccount?.mobileNumber
    val currentVisibility = uiState.accountVisibility

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
            // Top bar with Account Visibility Pill, Settings button, and Logout button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isPersonal = currentVisibility == AccountVisibility.PERSONAL
                Surface(
                    onClick = { showSettingsSheet = true },
                    shape = RoundedCornerShape(50),
                    color = if (isPersonal) PulseCrimson.copy(alpha = 0.16f) else PulseCyan.copy(alpha = 0.14f),
                    modifier = Modifier
                        .height(40.dp)
                        .border(
                            1.dp,
                            if (isPersonal) PulseCrimson.copy(alpha = 0.45f) else PulseCyan.copy(alpha = 0.45f),
                            RoundedCornerShape(50)
                        )
                        .testTag("account_visibility_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isPersonal) Icons.Filled.Lock else Icons.Filled.Public,
                            contentDescription = null,
                            tint = if (isPersonal) PulseCrimson else PulseCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isPersonal) "Personal" else "Public",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPersonal) PulseCrimson else PulseCyan
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("account_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Account Settings",
                            tint = PulseTextPrimary
                        )
                    }

                    OutlinedButton(
                        onClick = onLogoutClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log Out",
                            tint = PulseCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Log Out", color = PulseCrimson)
                    }
                }
            }

            if (uiState.accountErrorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PulseCrimson.copy(alpha = 0.16f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uiState.accountErrorMessage,
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

            // Profile Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CreatorAvatar(
                    avatarKey = "local_user",
                    displayName = uiState.userProfile.displayName,
                    size = 78.dp,
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

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!firebaseEmail.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PulseSurfaceElevated
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Email,
                                    contentDescription = null,
                                    tint = PulseCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = firebaseEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PulseTextSecondary
                                )
                            }
                        }
                    }
                    if (!mobileNumber.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PulseSurfaceElevated
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = null,
                                    tint = PulseCrimson,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = mobileNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PulseTextSecondary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStat(count = uiState.followedCreatorIds.size, label = "Following")
                    ProfileStat(count = uiState.likedIds.size, label = "Liked")
                    ProfileStat(count = uiState.watchHistory.size, label = "Watched")
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showEditDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
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
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("New Short")
                    }
                    OutlinedButton(
                        onClick = { showBlockedUsersSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("manage_blocked_users_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = null,
                            tint = if (uiState.blockedUsers.isNotEmpty()) PulseCrimson else PulseTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = if (uiState.blockedUsers.isNotEmpty()) {
                                "Blocked (${uiState.blockedUsers.size})"
                            } else {
                                "Block / Safety"
                            },
                            color = PulseTextPrimary
                        )
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
                initialMobile = mobileNumber.orEmpty(),
                initialBio = uiState.userProfile.bio,
                onSave = { name, handle, mobile, bio ->
                    onEditProfile(name, handle, mobile.takeIf { it.isNotBlank() }, bio)
                    showEditDialog = false
                },
                onDismiss = { showEditDialog = false }
            )
        }

        if (showSettingsSheet) {
            AccountSettingsBottomSheet(
                currentVisibility = currentVisibility,
                blockedCount = uiState.blockedUsers.size,
                isUpdating = uiState.isUpdatingVisibility,
                onSelectVisibility = { selected ->
                    if (selected != currentVisibility) {
                        onAccountVisibilityChange(selected)
                    }
                },
                onOpenBlockedUsers = {
                    showSettingsSheet = false
                    showBlockedUsersSheet = true
                },
                onDismiss = { showSettingsSheet = false }
            )
        }

        if (showBlockedUsersSheet) {
            BlockedUsersBottomSheet(
                blockedUsers = uiState.blockedUsers,
                availableCreators = uiState.knownCreators.filter { it.creatorId !in uiState.blockedCreatorIds },
                isUpdating = uiState.isUpdatingBlockState,
                onBlockUser = onBlockUser,
                onUnblockUser = onUnblockUser,
                onOpenCreatorProfile = { creator ->
                    showBlockedUsersSheet = false
                    onOpenCreatorProfile(creator)
                },
                onDismiss = { showBlockedUsersSheet = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSettingsBottomSheet(
    currentVisibility: AccountVisibility,
    blockedCount: Int,
    isUpdating: Boolean,
    onSelectVisibility: (AccountVisibility) -> Unit,
    onOpenBlockedUsers: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PulseSurface,
        contentColor = PulseTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Account Privacy",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PulseTextPrimary
                    )
                    Text(
                        text = "Choose whether your account is personal or public",
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseTextSecondary
                    )
                }
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PulseCyan,
                        strokeWidth = 2.dp
                    )
                }
            }

            VisibilityOptionCard(
                title = "Public Account",
                description = "Anyone on ReelPulse can view your profile, discover your posted shorts, and follow you.",
                icon = Icons.Filled.Public,
                accentColor = PulseCyan,
                selected = currentVisibility == AccountVisibility.PUBLIC,
                onClick = { onSelectVisibility(AccountVisibility.PUBLIC) },
                testTag = "visibility_public_option"
            )

            VisibilityOptionCard(
                title = "Personal Account",
                description = "Keep your account personal and private. Profile details and newly posted clips stay limited to you.",
                icon = Icons.Filled.Lock,
                accentColor = PulseCrimson,
                selected = currentVisibility == AccountVisibility.PERSONAL,
                onClick = { onSelectVisibility(AccountVisibility.PERSONAL) },
                testTag = "visibility_personal_option"
            )

            Surface(
                onClick = onOpenBlockedUsers,
                shape = RoundedCornerShape(16.dp),
                color = PulseSurfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PulseOutline, RoundedCornerShape(16.dp))
                    .testTag("settings_blocked_users_row")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PulseCrimson.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = null,
                            tint = PulseCrimson,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.blocked_users_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PulseTextPrimary
                        )
                        Text(
                            text = if (blockedCount == 0) {
                                "No users blocked • Tap to block or manage accounts"
                            } else {
                                "$blockedCount blocked • Tap to unblock or manage"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = PulseTextSecondary
                        )
                    }
                }
            }

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PulseSurfaceElevated),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Done", color = PulseTextPrimary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockedUsersBottomSheet(
    blockedUsers: List<BlockedUser>,
    availableCreators: List<CreatorSummary>,
    isUpdating: Boolean,
    onBlockUser: (CreatorSummary) -> Unit,
    onUnblockUser: (String) -> Unit,
    onOpenCreatorProfile: (CreatorSummary) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pendingBlockCreator by remember { mutableStateOf<CreatorSummary?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PulseSurface,
        contentColor = PulseTextPrimary
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("blocked_users_sheet"),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.blocked_users_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PulseTextPrimary
                        )
                        Text(
                            text = "Blocked users' videos and live streams are hidden from your feed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PulseTextSecondary
                        )
                    }
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PulseCrimson,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            if (blockedUsers.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PulseSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PulseOutline, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = PulseCyan
                            )
                            Text(
                                text = "You haven't blocked any users yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PulseTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(blockedUsers, key = { it.blockedCreatorId }) { blocked ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PulseCrimson.copy(alpha = 0.10f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PulseCrimson.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CreatorAvatar(
                                avatarKey = blocked.blockedCreatorAvatarKey,
                                displayName = blocked.blockedCreatorName,
                                size = 44.dp,
                                borderColor = PulseCrimson
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = blocked.blockedCreatorName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTextPrimary
                                )
                                Text(
                                    text = blocked.blockedCreatorHandle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PulseTextSecondary
                                )
                            }
                            OutlinedButton(
                                onClick = { onUnblockUser(blocked.blockedCreatorId) },
                                enabled = !isUpdating,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("unblock_user_${blocked.blockedCreatorId}")
                            ) {
                                Text(
                                    text = stringResource(R.string.unblock_user),
                                    color = PulseCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            if (availableCreators.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Creators on ReelPulse",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PulseTextPrimary
                    )
                }
                items(availableCreators, key = { it.creatorId }) { creator ->
                    Surface(
                        onClick = { onOpenCreatorProfile(creator) },
                        shape = RoundedCornerShape(16.dp),
                        color = PulseSurfaceElevated,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PulseOutline, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CreatorAvatar(
                                avatarKey = creator.creatorAvatarKey,
                                displayName = creator.creatorName,
                                size = 44.dp,
                                borderColor = PulseCyan
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = creator.creatorName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PulseTextPrimary
                                )
                                Text(
                                    text = "${creator.creatorHandle} • ${creator.videoCount} shorts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PulseTextSecondary
                                )
                            }
                            OutlinedButton(
                                onClick = { pendingBlockCreator = creator },
                                enabled = !isUpdating,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("block_user_${creator.creatorId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Block,
                                    contentDescription = null,
                                    tint = PulseCrimson,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(
                                    text = "Block",
                                    color = PulseCrimson,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pendingBlockCreator?.let { target ->
        BlockUserConfirmationDialog(
            creatorName = target.creatorName,
            creatorHandle = target.creatorHandle,
            onConfirm = {
                onBlockUser(target)
                pendingBlockCreator = null
            },
            onDismiss = { pendingBlockCreator = null }
        )
    }
}

@Composable
private fun VisibilityOptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accentColor.copy(alpha = 0.12f) else PulseSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) accentColor else PulseOutline,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PulseTextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = PulseTextSecondary
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
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
    initialMobile: String,
    initialBio: String,
    onSave: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var handle by remember { mutableStateOf(initialHandle) }
    var mobile by remember { mutableStateOf(initialMobile) }
    var bio by remember { mutableStateOf(initialBio) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PulseSurface,
        title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorText = null },
                    label = { Text("Display Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it; errorText = null },
                    label = { Text("Handle") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it; errorText = null },
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    maxLines = 3
                )
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = PulseCrimson
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedMobile = mobile.trim()
                    if (name.trim().length < 2) {
                        errorText = "Name must be at least 2 characters."
                    } else if (trimmedMobile.isNotEmpty() && trimmedMobile.count { it.isDigit() } < 7) {
                        errorText = "Enter a valid mobile number (at least 7 digits) or leave blank."
                    } else {
                        onSave(name, handle, trimmedMobile, bio)
                    }
                },
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
