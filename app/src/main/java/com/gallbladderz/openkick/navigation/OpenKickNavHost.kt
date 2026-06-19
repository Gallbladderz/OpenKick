package com.gallbladderz.openkick.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.categories.CategoriesScreen
import com.gallbladderz.openkick.features.home.HomeScreen
import com.gallbladderz.openkick.features.player.PlayerScreen
import com.gallbladderz.openkick.features.profile.MainViewModel
import com.gallbladderz.openkick.features.search.SearchScreen
import com.gallbladderz.openkick.features.categories.CategoryDetailsScreen
import com.gallbladderz.openkick.features.profile.SettingsScreen
import org.koin.androidx.compose.koinViewModel
import com.gallbladderz.openkick.features.following.FollowingScreen

@Composable
fun OpenKickNavHost(viewModel: MainViewModel = koinViewModel()) {
    val navController = rememberNavController()
    val hideCategories by viewModel.hideCategories.collectAsStateWithLifecycle()

    val bottomNavItems = remember(hideCategories) {
        val items = mutableListOf(
            BottomNavItem(R.string.home, HomeRoute, Icons.Filled.Home, Icons.Outlined.Home),
        )
        if (!hideCategories) {
            items.add(BottomNavItem(R.string.categories, CategoriesRoute, Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Filled.List))
        }
        items.add(BottomNavItem(R.string.followers, FollowersRoute, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder))
        items.add(BottomNavItem(R.string.profile, ProfileRoute, Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle))
        items
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isPlayerRoute = currentDestination?.route?.contains("PlayerRoute") == true
    val showBottomBar = !isPlayerRoute

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val routeName = item.route::class.qualifiedName ?: ""
                        val isSelected = currentDestination?.hierarchy?.any { it.route?.contains(routeName) == true } == true
                        val title = stringResource(id = item.titleResId)
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.icon else item.selectedIcon,
                                    contentDescription = title
                                )
                            },
                            label = { Text(title) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onStreamClick = { streamerName ->
                        navController.navigate(PlayerRoute(streamerName))
                    },
                    onSearchClick = {
                        navController.navigate(SearchRoute)
                    }
                )
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
                    onChannelClick = { streamerName ->
                        navController.navigate(PlayerRoute(streamerName))
                    }
                )
            }

            composable<CategoriesRoute> {
                CategoriesScreen(
                    onCategoryClick = { slug ->
                        navController.navigate(CategoryDetailsRoute(slug))
                    }
                )
            }

            composable<FollowersRoute> {
                FollowingScreen(
                    onManageClick = {
                        // TODO: позже тут будет переход на новый экран управления
                        // navController.navigate(ManageFollowsRoute)
                    }
                )
            }

            composable<ProfileRoute> {
                SettingsScreen()
            }

            composable<PlayerRoute> { backStackEntry ->
                val playerRoute = backStackEntry.toRoute<PlayerRoute>()
                PlayerScreen(
                    streamerName = playerRoute.streamerName,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
