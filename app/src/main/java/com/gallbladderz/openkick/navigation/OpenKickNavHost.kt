package com.gallbladderz.openkick.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import org.koin.androidx.compose.koinViewModel

@Composable
fun OpenKickNavHost(viewModel: MainViewModel = koinViewModel()) {
    val navController = rememberNavController()
    val hideCategories by viewModel.hideCategories.collectAsStateWithLifecycle()

    val bottomNavItems = remember(hideCategories) {
        val items = mutableListOf(
            BottomNavItem(R.string.home, HomeRoute, Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavItem(R.string.search, SearchRoute, Icons.Filled.Search, Icons.Outlined.Search)
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
                    }
                )
            }

            composable<SearchRoute> {
                SearchScreen(
                    onChannelClick = { streamerName ->
                        navController.navigate(PlayerRoute(streamerName))
                    }
                )
            }

            composable<CategoriesRoute> { CategoriesScreen() }

            composable<FollowersRoute> { DummyScreen(stringResource(R.string.followers)) }

            composable<ProfileRoute> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.settings))
                    Button(onClick = { viewModel.toggleCategories(!hideCategories) }) {
                        Text(if (hideCategories) stringResource(R.string.show_categories) else stringResource(R.string.hide_categories))
                    }
                }
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

@Composable
fun DummyScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title)
    }
}
