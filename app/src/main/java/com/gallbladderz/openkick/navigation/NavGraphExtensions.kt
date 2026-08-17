/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.gallbladderz.openkick.LocalBottomBarOffset
import com.gallbladderz.openkick.features.player.LocalGlobalPlayerController
import com.gallbladderz.openkick.features.search.SearchScreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

fun NavGraphBuilder.mainTabsScreen(navController: NavController) {
    composable<MainTabsRoute> {
        val pagerState = rememberPagerState(pageCount = { MainTab.entries.size })
        val coroutineScope = rememberCoroutineScope()
        val bottomBarOffset = LocalBottomBarOffset.current


        Scaffold { innerPadding ->
            val globalPlayerController = LocalGlobalPlayerController.current


            Box(modifier = Modifier.fillMaxSize()) {


                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()


                        .padding(top = innerPadding.calculateTopPadding())
                ) { page ->
                    when (MainTab.entries[page]) {
                        MainTab.HOME -> {
                            com.gallbladderz.openkick.features.home.HomeRoute(
                                onStreamClick = { streamerName ->
                                    globalPlayerController.expandPlayer(streamerName)
                                },
                                onCategoryClick = { slug ->
                                    navController.navigate(CategoryDetailsRoute(slug))
                                },
                                onClipClick = { clip ->
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
                                onStreamClick = { slug -> globalPlayerController.expandPlayer(slug) },
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


                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset { IntOffset(0, bottomBarOffset().roundToInt()) }
                ) {
                    OpenKickBottomBar(
                        currentPage = pagerState.currentPage,
                        onTabSelected = { tabOrdinal ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(tabOrdinal)
                            }
                        }
                    )
                }
            }
        }
    }
}


fun NavGraphBuilder.categoryDetailsScreen(navController: NavController) {
    composable<CategoryDetailsRoute> { backStackEntry ->
        val globalPlayerController = LocalGlobalPlayerController.current
        val route = backStackEntry.toRoute<CategoryDetailsRoute>()
        com.gallbladderz.openkick.features.categories.CategoryDetailsRoute(
            slug = route.slug,
            onBackClick = { navController.popBackStack() },
            onStreamClick = { streamerName ->
                globalPlayerController.expandPlayer(streamerName)
            },
            onClipClick = { clip ->
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
        val globalPlayerController = LocalGlobalPlayerController.current
        SearchScreen(
            onBackClick = { navController.popBackStack() },
            onChannelClick = { streamerName, isLive ->
                if (isLive) {
                    globalPlayerController.expandPlayer(streamerName)
                } else {
                    navController.navigate(StreamerProfileRoute(slug = streamerName))
                }
            },
            onCategoryClick = { slug -> navController.navigate(CategoryDetailsRoute(slug)) },
            onStreamClick = { slug -> globalPlayerController.expandPlayer(slug) }
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
        val globalPlayerController = LocalGlobalPlayerController.current
        com.gallbladderz.openkick.features.following.AllFollowsRoute(
            onBackClick = { navController.popBackStack() },
            onStreamClick = { slug -> globalPlayerController.expandPlayer(slug) },
            onProfileClick = { slug -> navController.navigate(StreamerProfileRoute(slug)) }
        )
    }
}

fun NavGraphBuilder.streamerProfileScreen(navController: NavController) {
    composable<StreamerProfileRoute> { backStackEntry ->
        val globalPlayerController = LocalGlobalPlayerController.current
        val route = backStackEntry.toRoute<StreamerProfileRoute>()
        com.gallbladderz.openkick.features.profile.StreamerProfileRoute(
            slug = route.slug,
            onBackClick = { navController.popBackStack() },
            onStreamClick = { slug -> globalPlayerController.expandPlayer(slug) },
            onVideoClick = { videoId: String ->
                navController.navigate(
                    VodPlayerRoute(
                        videoId = videoId
                    )
                )
            },
            onClipClick = { clip ->
                navController.navigate(
                    ClipPlayerRoute(
                        clipId = clip.id
                    )
                )
            }
        )
    }
}

fun NavGraphBuilder.vodPlayerScreen(navController: NavController) {
    composable<VodPlayerRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<VodPlayerRoute>()
        com.gallbladderz.openkick.features.player.vod.VodPlayerRoute(
            videoId = route.videoId,
            onBackClick = { navController.popBackStack() },
            onStreamerClick = { slug -> navController.navigate(StreamerProfileRoute(slug)) }
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
