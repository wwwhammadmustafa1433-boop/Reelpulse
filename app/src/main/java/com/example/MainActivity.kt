package com.example

import android.app.Application
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ai.configureAppCheck
import com.example.ui.auth.AuthFlowHost
import com.example.ui.auth.PendingSignUpProfile
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.navigation.AppRoute
import com.example.ui.screens.AlgorithmLabScreen
import com.example.ui.screens.CameraCaptureScreen
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.CreatorProfileScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.GeminiChatScreen
import com.example.ui.screens.LiveScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseObsidian
import com.example.ui.theme.PulseSurface
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.theme.PulseTextSecondary
import com.example.ui.theme.ReelPulseTheme
import com.example.ui.viewmodel.GeminiChatViewModel
import com.example.ui.viewmodel.LiveStreamViewModel
import com.example.ui.viewmodel.ReelPulseViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            ReelPulseTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var pendingSignUp by remember { mutableStateOf<PendingSignUpProfile?>(null) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthFlowHost(
            onSignUpPrepared = { pending -> pendingSignUp = pending },
            onAuthSuccess = {}
        )
    } else {
        key(user.uid) {
            val viewModel: ReelPulseViewModel = viewModel(
                key = user.uid,
                factory = ReelPulseViewModel.provideFactory(
                    currentUserId = user.uid,
                    pendingSignUp = pendingSignUp
                )
            )
            ReelPulseAuthenticatedApp(
                currentUserId = user.uid,
                viewModel = viewModel,
                onLogout = {
                    pendingSignUp = null
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    }
}

private data class NavDestinationItem(
    val route: AppRoute,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
    val isCreateButton: Boolean = false
)

private val navItems = listOf(
    NavDestinationItem(
        route = AppRoute.Feed,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    ),
    NavDestinationItem(
        route = AppRoute.Discover,
        labelRes = R.string.nav_discover,
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_discover"
    ),
    NavDestinationItem(
        route = AppRoute.Create,
        labelRes = R.string.nav_create,
        selectedIcon = Icons.Filled.Add,
        unselectedIcon = Icons.Filled.Add,
        testTag = "nav_create",
        isCreateButton = true
    ),
    NavDestinationItem(
        route = AppRoute.Algorithm,
        labelRes = R.string.nav_algorithm,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_algorithm"
    ),
    NavDestinationItem(
        route = AppRoute.Profile,
        labelRes = R.string.nav_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_profile"
    )
)

@Composable
fun ReelPulseAuthenticatedApp(
    currentUserId: String,
    viewModel: ReelPulseViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val activeComments by viewModel.activeComments.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    fun navigateTopLevel(route: AppRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseObsidian)
    ) {
        val isFullScreenOverlay = currentDestination?.hasRoute<AppRoute.Camera>() == true ||
            currentDestination?.hasRoute<AppRoute.GeminiChat>() == true ||
            currentDestination?.hasRoute<AppRoute.Live>() == true ||
            currentDestination?.hasRoute<AppRoute.CreatorProfile>() == true
        val useNavigationRail = maxWidth >= 600.dp && !isFullScreenOverlay

        Scaffold(
            containerColor = PulseObsidian,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useNavigationRail && !isFullScreenOverlay) {
                    NavigationBar(
                        containerColor = PulseObsidian.copy(alpha = 0.96f),
                        contentColor = PulseTextPrimary,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            val label = stringResource(item.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    if (item.isCreateButton) {
                                        CreatePlusBadge()
                                    } else {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = label
                                        )
                                    }
                                },
                                label = {
                                    if (!item.isCreateButton) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = if (item.route == AppRoute.Algorithm) PulseCyan else PulseTextPrimary,
                                    selectedTextColor = if (item.route == AppRoute.Algorithm) PulseCyan else PulseTextPrimary,
                                    unselectedIconColor = PulseTextSecondary,
                                    unselectedTextColor = PulseTextSecondary,
                                    indicatorColor = PulseSurface
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = PulseSurface,
                        contentColor = PulseTextPrimary,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            val label = stringResource(item.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(item.route) },
                                icon = {
                                    if (item.isCreateButton) {
                                        CreatePlusBadge()
                                    } else {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = label
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = PulseCrimson,
                                    selectedTextColor = PulseTextPrimary,
                                    unselectedIconColor = PulseTextSecondary,
                                    unselectedTextColor = PulseTextSecondary,
                                    indicatorColor = PulseObsidian
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Feed,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<AppRoute.Feed> {
                        FeedScreen(
                            uiState = uiState,
                            activeComments = activeComments,
                            viewModel = viewModel,
                            onNavigateToDiscover = { tag ->
                                if (tag != null) {
                                    viewModel.openDiscoverWithTag(tag)
                                }
                                navigateTopLevel(AppRoute.Discover)
                            },
                            onNavigateToAlgorithmLab = {
                                navigateTopLevel(AppRoute.Algorithm)
                            },
                            onOpenGeminiChat = {
                                navController.navigate(AppRoute.GeminiChat) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenLive = {
                                navController.navigate(AppRoute.Live(startInBroadcastSetup = false)) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenCreatorProfile = { video ->
                                if (video.isUserCreated || video.creatorId == "local_user") {
                                    navigateTopLevel(AppRoute.Profile)
                                } else {
                                    navController.navigate(
                                        AppRoute.CreatorProfile(
                                            creatorId = video.creatorId,
                                            creatorName = video.creatorName,
                                            creatorHandle = video.creatorHandle,
                                            creatorAvatarKey = video.creatorAvatarKey
                                        )
                                    ) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }

                    composable<AppRoute.Discover> {
                        DiscoverScreen(
                            uiState = uiState,
                            onSearchChange = viewModel::updateSearchQuery,
                            onCategorySelect = viewModel::selectCategoryFilter,
                            onTrendingTagSelect = viewModel::selectTrendingTag,
                            onVideoClick = { videoId ->
                                viewModel.requestJumpToVideo(videoId)
                                navigateTopLevel(AppRoute.Feed)
                            },
                            onCreatorClick = { creator ->
                                navController.navigate(
                                    AppRoute.CreatorProfile(
                                        creatorId = creator.creatorId,
                                        creatorName = creator.creatorName,
                                        creatorHandle = creator.creatorHandle,
                                        creatorAvatarKey = creator.creatorAvatarKey
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            },
                            onCreateWithTag = { tag ->
                                viewModel.prepareDraftWithTrendingTag(tag)
                                navigateTopLevel(AppRoute.Create)
                            },
                            onOpenGeminiChat = {
                                navController.navigate(AppRoute.GeminiChat) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable<AppRoute.Create> {
                        val draft = uiState.createDraft
                        CreateScreen(
                            draftVideoUri = draft.videoUri,
                            draftFilterPreset = draft.filterPreset,
                            draftSourceType = draft.sourceType,
                            draftTagsCsv = draft.tagsCsv,
                            trendingTopics = uiState.trendingTopics,
                            accountVisibility = uiState.accountVisibility,
                            isPublishing = draft.isPublishing,
                            publishError = draft.publishError,
                            onOpenCamera = {
                                navController.navigate(AppRoute.Camera) {
                                    launchSingleTop = true
                                }
                            },
                            onGoLive = {
                                navController.navigate(
                                    AppRoute.Live(
                                        startInBroadcastSetup = true,
                                        initialFilterId = draft.filterPreset.id
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            },
                            onGalleryVideoPicked = { uri ->
                                viewModel.setDraftGalleryVideo(uri.toString())
                            },
                            onUseSampleClip = viewModel::setDraftSampleVideo,
                            onClearVideo = viewModel::clearDraftVideo,
                            onFilterSelected = viewModel::setDraftFilter,
                            onTagsChange = viewModel::setDraftTags,
                            onToggleTrendingTag = viewModel::toggleDraftTrendingTag,
                            onPublish = { title, caption, category, tagsCsv, soundTitle ->
                                viewModel.publishDraftVideo(
                                    title = title,
                                    caption = caption,
                                    category = category,
                                    tagsCsv = tagsCsv,
                                    soundTitle = soundTitle,
                                    onComplete = { navigateTopLevel(AppRoute.Feed) }
                                )
                            },
                            onDismissPublishError = viewModel::dismissPublishError
                        )
                    }

                    composable<AppRoute.Camera> {
                        CameraCaptureScreen(
                            initialFilter = uiState.createDraft.filterPreset,
                            onVideoRecorded = { videoUri, filterPreset, durationMs ->
                                viewModel.setDraftRecordedVideo(
                                    videoUri = videoUri,
                                    filterPreset = filterPreset,
                                    durationMs = durationMs
                                )
                                navController.popBackStack()
                            },
                            onGallerySelected = { uri, filterPreset ->
                                viewModel.setDraftGalleryVideo(
                                    videoUri = uri.toString(),
                                    filterPreset = filterPreset
                                )
                                navController.popBackStack()
                            },
                            onSwitchToLive = { filterPreset ->
                                navController.navigate(
                                    AppRoute.Live(
                                        startInBroadcastSetup = true,
                                        initialFilterId = filterPreset.id
                                    )
                                ) {
                                    popUpTo<AppRoute.Camera> { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onClose = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable<AppRoute.Algorithm> {
                        AlgorithmLabScreen(
                            uiState = uiState,
                            onExplorationRateChange = viewModel::setExplorationRate,
                            onResetSignals = viewModel::resetPersonalizationSignals,
                            onOpenGeminiChat = {
                                navController.navigate(AppRoute.GeminiChat) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable<AppRoute.Profile> {
                        ProfileScreen(
                            uiState = uiState,
                            onEditProfile = viewModel::updateProfile,
                            onAccountVisibilityChange = viewModel::updateAccountVisibility,
                            onVideoClick = { videoId ->
                                viewModel.requestJumpToVideo(videoId)
                                navigateTopLevel(AppRoute.Feed)
                            },
                            onCreateClick = {
                                navigateTopLevel(AppRoute.Create)
                            },
                            onBlockUser = { creator ->
                                viewModel.blockUser(
                                    creatorId = creator.creatorId,
                                    creatorName = creator.creatorName,
                                    creatorHandle = creator.creatorHandle,
                                    creatorAvatarKey = creator.creatorAvatarKey
                                )
                            },
                            onUnblockUser = viewModel::unblockUser,
                            onOpenCreatorProfile = { creator ->
                                navController.navigate(
                                    AppRoute.CreatorProfile(
                                        creatorId = creator.creatorId,
                                        creatorName = creator.creatorName,
                                        creatorHandle = creator.creatorHandle,
                                        creatorAvatarKey = creator.creatorAvatarKey
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            },
                            onLogoutClick = onLogout,
                            onDismissError = viewModel::dismissAccountError
                        )
                    }

                    composable<AppRoute.GeminiChat> {
                        val geminiViewModel: GeminiChatViewModel = viewModel()
                        val chatState by geminiViewModel.screenState.collectAsStateWithLifecycle()
                        GeminiChatScreen(
                            state = chatState,
                            onSendMessage = geminiViewModel::sendMessage,
                            onRetry = geminiViewModel::retryLastPrompt,
                            onDismissError = geminiViewModel::dismissError,
                            onNewChat = geminiViewModel::startNewChat,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable<AppRoute.Live> { entry ->
                        val liveArgs = entry.toRoute<AppRoute.Live>()
                        val app = context.applicationContext as Application
                        val liveViewModel: LiveStreamViewModel = viewModel(
                            key = "${currentUserId}_${liveArgs.startInBroadcastSetup}_${liveArgs.initialFilterId}",
                            factory = LiveStreamViewModel.provideFactory(
                                application = app,
                                currentUserId = currentUserId,
                                initialStartInSetup = liveArgs.startInBroadcastSetup,
                                initialFilterId = liveArgs.initialFilterId
                            )
                        )
                        val liveState by liveViewModel.uiState.collectAsStateWithLifecycle()
                        LiveScreen(
                            uiState = liveState,
                            userDisplayName = uiState.userProfile.displayName,
                            userHandle = uiState.userProfile.handle,
                            accountVisibility = uiState.accountVisibility,
                            onOpenGoLiveSetup = liveViewModel::openGoLiveSetup,
                            onOpenHub = liveViewModel::openHub,
                            onUpdateDraftTitle = liveViewModel::updateDraftTitle,
                            onUpdateDraftCategory = liveViewModel::updateDraftCategory,
                            onSelectFilter = liveViewModel::selectFilter,
                            onStartBroadcast = {
                                liveViewModel.startBroadcast(
                                    hostName = uiState.userProfile.displayName,
                                    hostHandle = uiState.userProfile.handle,
                                    visibility = uiState.accountVisibility
                                )
                            },
                            onJoinStream = liveViewModel::joinStreamAsViewer,
                            onEndOrLeaveStream = {
                                liveViewModel.endOrLeaveStream(
                                    onExitToPrevious = { navController.popBackStack() }
                                )
                            },
                            onSendChatMessage = { text ->
                                liveViewModel.sendChatMessage(
                                    senderName = uiState.userProfile.displayName,
                                    senderHandle = uiState.userProfile.handle,
                                    text = text
                                )
                            },
                            onSendHeartReaction = liveViewModel::sendHeartReaction,
                            onDismissError = liveViewModel::dismissError,
                            onClose = { navController.popBackStack() }
                        )
                    }

                    composable<AppRoute.CreatorProfile> { entry ->
                        val args = entry.toRoute<AppRoute.CreatorProfile>()
                        val creatorVideos = uiState.allScoredVideos.filter {
                            it.video.creatorId == args.creatorId
                        }
                        CreatorProfileScreen(
                            creatorId = args.creatorId,
                            creatorName = args.creatorName,
                            creatorHandle = args.creatorHandle,
                            creatorAvatarKey = args.creatorAvatarKey,
                            creatorVideos = creatorVideos,
                            isFollowing = args.creatorId in uiState.followedCreatorIds,
                            isBlocked = args.creatorId in uiState.blockedCreatorIds,
                            isUpdatingBlock = uiState.isUpdatingBlockState,
                            errorMessage = uiState.accountErrorMessage,
                            onBack = { navController.popBackStack() },
                            onToggleFollow = {
                                viewModel.toggleFollowCreator(
                                    creatorId = args.creatorId,
                                    creatorName = args.creatorName
                                )
                            },
                            onBlockUser = {
                                viewModel.blockUser(
                                    creatorId = args.creatorId,
                                    creatorName = args.creatorName,
                                    creatorHandle = args.creatorHandle,
                                    creatorAvatarKey = args.creatorAvatarKey
                                )
                            },
                            onUnblockUser = {
                                viewModel.unblockUser(args.creatorId)
                            },
                            onVideoClick = { videoId ->
                                viewModel.requestJumpToVideo(videoId)
                                navigateTopLevel(AppRoute.Feed)
                            },
                            onDismissError = viewModel::dismissAccountError
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreatePlusBadge() {
    Box(
        modifier = Modifier
            .width(46.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(PulseCyan, PulseCrimson)
                )
            )
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.nav_create),
                tint = PulseObsidian,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
