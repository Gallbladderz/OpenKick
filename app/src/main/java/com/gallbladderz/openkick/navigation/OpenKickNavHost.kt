package com.gallbladderz.openkick.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.gallbladderz.openkick.features.categories.CategoryDetailsScreen
import com.gallbladderz.openkick.features.following.AllFollowsScreen
import com.gallbladderz.openkick.features.following.FollowingScreen
import com.gallbladderz.openkick.features.home.HomeScreen
import com.gallbladderz.openkick.features.player.PlayerScreen
import com.gallbladderz.openkick.features.profile.LanguageSettingsScreen
import com.gallbladderz.openkick.features.profile.SettingsScreen
import com.gallbladderz.openkick.features.profile.StreamerProfileScreen
import com.gallbladderz.openkick.features.search.SearchScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable


@Serializable
object MainTabsRoute


private enum class MainTab(val titleResId: Int, val icon: ImageVector, val selectedIcon: ImageVector) {
    HOME(R.string.home, Icons.Outlined.Home, Icons.Filled.Home),
    FOLLOWERS(R.string.followers, Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
    PROFILE(R.string.profile, Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
}

@Composable
fun OpenKickNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MainTabsRoute
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
                            HomeScreen(
                                onStreamClick = { streamerName -> navController.navigate(PlayerRoute(streamerName)) },
                                onCategoryClick = { slug -> navController.navigate(CategoryDetailsRoute(slug)) },
                                onSearchClick = { navController.navigate(SearchRoute) }
                            )
                        }
                        MainTab.FOLLOWERS -> {
                            FollowingScreen(
                                onManageClick = { navController.navigate(AllFollowsRoute) },
                                onStreamerClick = { slug -> navController.navigate(PlayerRoute(slug)) },
                                onCategoryClick = { slug -> navController.navigate(CategoryDetailsRoute(slug)) }
                            )
                        }
                        MainTab.PROFILE -> {
                            SettingsScreen(
                                onLanguageSettingsClick = {
                                    navController.navigate(LanguageSettingsRoute)
                                }
                            )
                        }
                    }
                }
            }
        }



        composable<CategoryDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CategoryDetailsRoute>()
            CategoryDetailsScreen(
                slug = route.slug,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<SearchRoute> {
            SearchScreen(
                onChannelClick = { streamerName, isLive ->
                    if (isLive) {
                        // Стример онлайн — закидываем в плеер смотреть стрим
                        navController.navigate(PlayerRoute(streamerName))
                    } else {
                        // Стример оффлайн — отправляем в профиль изучать его биографию
                        navController.navigate(StreamerProfileRoute(slug = streamerName))
                    }
                }
            )
        }

        composable<LanguageSettingsRoute> {
            LanguageSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable<AllFollowsRoute> {
            AllFollowsScreen(
                onBackClick = { navController.popBackStack() },
                onStreamerClick = { slug -> navController.navigate(PlayerRoute(slug)) }
            )
        }

        composable<StreamerProfileRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<StreamerProfileRoute>()
            StreamerProfileScreen(
                slug = route.slug,
                onBackClick = { navController.popBackStack() },
                onVideoClick = { /* Сюда потом прикрутим плеер для VOD-ов */ }
            )
        }

        composable<PlayerRoute> { backStackEntry ->
            val playerRoute = backStackEntry.toRoute<PlayerRoute>()
            PlayerScreen(
                streamerName = playerRoute.streamerName,
                onBackClick = { navController.popBackStack() },
                onAvatarClick = { slug -> navController.navigate(StreamerProfileRoute(slug)) }
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
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = currentPage == tab.ordinal
                val title = stringResource(id = tab.titleResId)

                val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                Row(
                    modifier = Modifier
                        .height(48.dp)
                        .animateContentSize()
                        .then(if (isSelected) Modifier.weight(1f) else Modifier.width(64.dp))
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab.ordinal) }
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                        contentDescription = title,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )

                    AnimatedVisibility(visible = isSelected) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            modifier = Modifier.padding(start = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}