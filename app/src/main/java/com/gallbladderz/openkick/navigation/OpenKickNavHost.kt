package com.gallbladderz.openkick.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.search.SearchScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable


@Serializable
object MainTabsRoute


private enum class MainTab(
    val titleResId: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    HOME(R.string.home, Icons.Outlined.Home, Icons.Filled.Home),
    FOLLOWERS(R.string.followers, Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
    SETTINGS(R.string.settings_tab, Icons.Outlined.Settings, Icons.Filled.Settings)
}

@Composable
fun OpenKickNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MainTabsRoute,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)
            )
        }
    ) {

        composable<MainTabsRoute> {
            val pagerState = rememberPagerState(pageCount = { MainTab.entries.size })
            val coroutineScope = rememberCoroutineScope()

            Scaffold(
                bottomBar = {
                    OpenKickBottomBar(
                        currentPage = pagerState.currentPage,
                        onTabSelected = { tabOrdinal ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(tabOrdinal)
                            }
                        }
                    )
                }
            ) { innerPadding ->

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) { page ->
                    when (MainTab.entries[page]) {
                        MainTab.HOME -> {
                            com.gallbladderz.openkick.features.home.HomeRoute(
                                onStreamClick = { streamerName ->
                                    navController.navigate(
                                        PlayerRoute(
                                            streamerName
                                        )
                                    )
                                },
                                onCategoryClick = { slug ->
                                    navController.navigate(
                                        CategoryDetailsRoute(slug)
                                    )
                                },
                                onClipClick = { clip ->
                                    navController.navigate(
                                        ClipPlayerRoute(
                                            videoUrl = clip.videoUrl,
                                            title = clip.title,
                                            streamerName = clip.streamerName,
                                            streamerAvatarUrl = clip.streamerAvatarUrl,
                                            views = clip.views,
                                            durationFormatted = clip.durationFormatted
                                        )
                                    )
                                },
                                onSearchClick = { navController.navigate(SearchRoute) }
                            )
                        }

                        MainTab.FOLLOWERS -> {
                            com.gallbladderz.openkick.features.following.FollowingRoute(
                                onManageClick = { navController.navigate(AllFollowsRoute) },
                                onStreamClick = { slug -> navController.navigate(PlayerRoute(slug)) },
                                onProfileClick = { slug ->
                                    navController.navigate(
                                        StreamerProfileRoute(slug)
                                    )
                                },
                                onCategoryClick = { slug ->
                                    navController.navigate(
                                        CategoryDetailsRoute(slug)
                                    )
                                }
                            )
                        }

                        MainTab.SETTINGS -> {
                            com.gallbladderz.openkick.features.profile.SettingsRoute(
                                onLanguageSettingsClick = {
                                    navController.navigate(LanguageSettingsRoute)
                                },
                                onNotificationSettingsClick = {
                                    navController.navigate(NotificationSettingsRoute)
                                },
                                onContentSettingsClick = {
                                    navController.navigate(ContentSettingsRoute)
                                },
                                onThemeSettingsClick = {
                                    navController.navigate(ThemeSettingsRoute)
                                },
                                onAboutAppClick = {
                                    navController.navigate(AboutAppRoute)
                                }
                            )
                        }
                    }
                }
            }
        }



        composable<CategoryDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CategoryDetailsRoute>()
            com.gallbladderz.openkick.features.categories.CategoryDetailsRoute(
                slug = route.slug,
                onBackClick = { navController.popBackStack() },
                onStreamClick = { streamerName ->
                    navController.navigate(PlayerRoute(streamerName))
                },
                onClipClick = { clip ->
                    navController.navigate(
                        ClipPlayerRoute(
                            videoUrl = clip.videoUrl,
                            title = clip.title,
                            streamerName = clip.streamerName,
                            streamerAvatarUrl = clip.streamerAvatarUrl,
                            views = clip.views,
                            durationFormatted = clip.durationFormatted
                        )
                    )
                }
            )
        }

        composable<ClipPlayerRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ClipPlayerRoute>()

            com.gallbladderz.openkick.features.player.ClipPlayerRoute(
                videoUrl = route.videoUrl,
                title = route.title,
                streamerName = route.streamerName,
                streamerAvatarUrl = route.streamerAvatarUrl,
                views = route.views,
                durationFormatted = route.durationFormatted,
                onBackClick = { navController.popBackStack() },
                onStreamerClick = { slug -> navController.navigate(StreamerProfileRoute(slug)) }
            )
        }

        composable<SearchRoute> {
            SearchScreen(
                onBackClick = { navController.popBackStack() },
                onChannelClick = { streamerName, isLive ->
                    if (isLive) {

                        navController.navigate(PlayerRoute(streamerName))
                    } else {

                        navController.navigate(StreamerProfileRoute(slug = streamerName))
                    }
                },
                onCategoryClick = { slug -> navController.navigate(CategoryDetailsRoute(slug)) },
                onStreamClick = { slug -> navController.navigate(PlayerRoute(slug)) }
            )
        }

        composable<LanguageSettingsRoute> {
            com.gallbladderz.openkick.features.profile.LanguageSettingsRoute(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<NotificationSettingsRoute> {
            com.gallbladderz.openkick.features.profile.NotificationSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<AllFollowsRoute> {
            com.gallbladderz.openkick.features.following.AllFollowsRoute(
                onBackClick = { navController.popBackStack() },
                onStreamClick = { slug -> navController.navigate(PlayerRoute(slug)) },
                onProfileClick = { slug -> navController.navigate(StreamerProfileRoute(slug)) }
            )
        }

        composable<StreamerProfileRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<StreamerProfileRoute>()
            com.gallbladderz.openkick.features.profile.StreamerProfileRoute(
                slug = route.slug,
                onBackClick = { navController.popBackStack() },
                onStreamClick = { slug -> navController.navigate(PlayerRoute(slug)) },
                onVideoClick = { video, profile ->
                    navController.navigate(
                        ClipPlayerRoute(
                            videoUrl = video.videoUrl,
                            title = video.title,
                            streamerName = profile.slug,
                            streamerAvatarUrl = profile.avatarUrl,
                            views = video.views,
                            durationFormatted = video.durationFormatted
                        )
                    )
                },
                onClipClick = { clip ->
                    navController.navigate(
                        ClipPlayerRoute(
                            videoUrl = clip.videoUrl,
                            title = clip.title,
                            streamerName = clip.streamerName,
                            streamerAvatarUrl = clip.streamerAvatarUrl,
                            views = clip.views,
                            durationFormatted = clip.durationFormatted
                        )
                    )
                }
            )
        }

        composable<PlayerRoute> { backStackEntry ->
            val playerRoute = backStackEntry.toRoute<PlayerRoute>()
            com.gallbladderz.openkick.features.player.PlayerRoute(
                streamerName = playerRoute.streamerName,
                onBackClick = { navController.popBackStack() },
                onAvatarClick = { slug -> navController.navigate(StreamerProfileRoute(slug)) },
                onCategoryClick = { slug -> navController.navigate(CategoryDetailsRoute(slug)) }
            )
        }

        composable<ContentSettingsRoute> {
            com.gallbladderz.openkick.features.profile.ContentSettingsRoute(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<ThemeSettingsRoute> {
            com.gallbladderz.openkick.features.profile.ThemeSettingsRoute(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<AboutAppRoute> {
            com.gallbladderz.openkick.features.profile.AboutAppRoute(
                onBackClick = { navController.popBackStack() },
                onLicensesClick = { navController.navigate(LicensesRoute) }
            )
        }

        composable<LicensesRoute> {
            com.gallbladderz.openkick.features.profile.LicensesRoute(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun OpenKickBottomBar(
    currentPage: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(70.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = currentPage == tab.ordinal
                val title = stringResource(id = tab.titleResId)


                val contentColor =
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab.ordinal) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                        contentDescription = title,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}