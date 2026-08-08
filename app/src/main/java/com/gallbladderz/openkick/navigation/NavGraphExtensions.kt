package com.gallbladderz.openkick.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.gallbladderz.openkick.features.search.SearchScreen
import kotlinx.coroutines.launch

fun NavGraphBuilder.mainTabsScreen(navController: NavController) {
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
                                navController.navigate(PlayerRoute(streamerName))
                            },
                            onCategoryClick = { slug ->
                                navController.navigate(CategoryDetailsRoute(slug))
                            },
                            onClipClick = { clip ->
                                com.gallbladderz.openkick.features.player.ActiveClipHolder.activeClip = clip
                                navController.navigate(
                                    ClipPlayerRoute(
                                        clipId = clip.id
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
                                navController.navigate(StreamerProfileRoute(slug))
                            },
                            onCategoryClick = { slug ->
                                navController.navigate(CategoryDetailsRoute(slug))
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
}

fun NavGraphBuilder.categoryDetailsScreen(navController: NavController) {
    composable<CategoryDetailsRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<CategoryDetailsRoute>()
        com.gallbladderz.openkick.features.categories.CategoryDetailsRoute(
            slug = route.slug,
            onBackClick = { navController.popBackStack() },
            onStreamClick = { streamerName ->
                navController.navigate(PlayerRoute(streamerName))
            },
            onClipClick = { clip ->
                com.gallbladderz.openkick.features.player.ActiveClipHolder.activeClip = clip
                navController.navigate(
                    ClipPlayerRoute(
                        clipId = clip.id
                    )
                )
            }
        )
    }
}

fun NavGraphBuilder.clipPlayerScreen(navController: NavController) {
    composable<ClipPlayerRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ClipPlayerRoute>()
        com.gallbladderz.openkick.features.player.ClipPlayerRoute(
            clipId = route.clipId,
            onBackClick = { navController.popBackStack() },
            onStreamerClick = { slug -> navController.navigate(StreamerProfileRoute(slug)) }
        )
    }
}

fun NavGraphBuilder.searchScreen(navController: NavController) {
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
}

fun NavGraphBuilder.languageSettingsScreen(navController: NavController) {
    composable<LanguageSettingsRoute> {
        com.gallbladderz.openkick.features.profile.LanguageSettingsRoute(
            onBackClick = { navController.popBackStack() }
        )
    }
}

fun NavGraphBuilder.notificationSettingsScreen(navController: NavController) {
    composable<NotificationSettingsRoute> {
        com.gallbladderz.openkick.features.profile.NotificationSettingsScreen(
            onBackClick = { navController.popBackStack() }
        )
    }
}

fun NavGraphBuilder.allFollowsScreen(navController: NavController) {
    composable<AllFollowsRoute> {
        com.gallbladderz.openkick.features.following.AllFollowsRoute(
            onBackClick = { navController.popBackStack() },
            onStreamClick = { slug -> navController.navigate(PlayerRoute(slug)) },
            onProfileClick = { slug -> navController.navigate(StreamerProfileRoute(slug)) }
        )
    }
}

fun NavGraphBuilder.streamerProfileScreen(navController: NavController) {
    composable<StreamerProfileRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<StreamerProfileRoute>()
        com.gallbladderz.openkick.features.profile.StreamerProfileRoute(
            slug = route.slug,
            onBackClick = { navController.popBackStack() },
onStreamClick = { slug -> navController.navigate(PlayerRoute(slug)) },
            onVideoClick = { clipId ->
                navController.navigate(
                    ClipPlayerRoute(
                        clipId = clipId
                    )
                )
            },
            onClipClick = { clip ->
                com.gallbladderz.openkick.features.player.ActiveClipHolder.activeClip = clip
                navController.navigate(
                    ClipPlayerRoute(
                        clipId = clip.id
                    )
                )
            }
        )
    }
}

fun NavGraphBuilder.playerScreen(navController: NavController) {
    composable<PlayerRoute> { backStackEntry ->
        val playerRoute = backStackEntry.toRoute<PlayerRoute>()
        com.gallbladderz.openkick.features.player.PlayerRoute(
            streamerName = playerRoute.streamerName,
            onBackClick = { navController.popBackStack() },
            onAvatarClick = { slug -> navController.navigate(StreamerProfileRoute(slug)) },
            onCategoryClick = { slug -> navController.navigate(CategoryDetailsRoute(slug)) }
        )
    }
}

fun NavGraphBuilder.contentSettingsScreen(navController: NavController) {
    composable<ContentSettingsRoute> {
        com.gallbladderz.openkick.features.profile.ContentSettingsRoute(
            onBackClick = { navController.popBackStack() }
        )
    }
}

fun NavGraphBuilder.themeSettingsScreen(navController: NavController) {
    composable<ThemeSettingsRoute> {
        com.gallbladderz.openkick.features.profile.ThemeSettingsRoute(
            onBackClick = { navController.popBackStack() }
        )
    }
}

fun NavGraphBuilder.aboutAppScreen(navController: NavController) {
    composable<AboutAppRoute> {
        com.gallbladderz.openkick.features.profile.AboutAppRoute(
            onBackClick = { navController.popBackStack() },
            onLicensesClick = { navController.navigate(LicensesRoute) }
        )
    }
}

fun NavGraphBuilder.licensesScreen(navController: NavController) {
    composable<LicensesRoute> {
        com.gallbladderz.openkick.features.profile.LicensesScreen(
            onBackClick = { navController.popBackStack() }
        )
    }
}
