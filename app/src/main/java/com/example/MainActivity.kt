package com.example

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.AppRoute
import com.example.ui.screens.AlgorithmLabScreen
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.PulseCrimson
import com.example.ui.theme.PulseCyan
import com.example.ui.theme.PulseObsidian
import com.example.ui.theme.PulseSurface
import com.example.ui.theme.PulseTextPrimary
import com.example.ui.theme.PulseTextSecondary
import com.example.ui.theme.ReelPulseTheme
import com.example.ui.viewmodel.ReelPulseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            ReelPulseTheme {
                ReelPulseApp()
            }
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
fun ReelPulseApp(
    viewModel: ReelPulseViewModel = viewModel(factory = ReelPulseViewModel.Factory)
) {
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
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = PulseObsidian,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!useNavigationRail) {
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
                                    viewModel.updateSearchQuery(tag)
                                }
                                navigateTopLevel(AppRoute.Discover)
                            },
                            onNavigateToAlgorithmLab = {
                                navigateTopLevel(AppRoute.Algorithm)
                            }
                        )
                    }

                    composable<AppRoute.Discover> {
                        DiscoverScreen(
                            uiState = uiState,
                            onSearchChange = viewModel::updateSearchQuery,
                            onCategorySelect = viewModel::selectCategoryFilter,
                            onVideoClick = { videoId ->
                                viewModel.requestJumpToVideo(videoId)
                                navigateTopLevel(AppRoute.Feed)
                            }
                        )
                    }

                    composable<AppRoute.Create> {
                        CreateScreen(
                            onPublish = { title, caption, category, tagsCsv, soundTitle, videoUrl ->
                                viewModel.publishVideo(
                                    title = title,
                                    caption = caption,
                                    category = category,
                                    tagsCsv = tagsCsv,
                                    soundTitle = soundTitle,
                                    videoUrl = videoUrl,
                                    onComplete = { navigateTopLevel(AppRoute.Feed) }
                                )
                            }
                        )
                    }

                    composable<AppRoute.Algorithm> {
                        AlgorithmLabScreen(
                            uiState = uiState,
                            onExplorationRateChange = viewModel::setExplorationRate,
                            onResetSignals = viewModel::resetPersonalizationSignals
                        )
                    }

                    composable<AppRoute.Profile> {
                        ProfileScreen(
                            uiState = uiState,
                            onEditProfile = viewModel::updateProfile,
                            onVideoClick = { videoId ->
                                viewModel.requestJumpToVideo(videoId)
                                navigateTopLevel(AppRoute.Feed)
                            },
                            onCreateClick = {
                                navigateTopLevel(AppRoute.Create)
                            }
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
