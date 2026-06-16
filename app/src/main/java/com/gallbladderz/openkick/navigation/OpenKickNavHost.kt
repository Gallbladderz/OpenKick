package com.gallbladderz.openkick.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import com.gallbladderz.openkick.features.categories.CategoriesScreen
import com.gallbladderz.openkick.features.home.HomeScreen
import com.gallbladderz.openkick.features.player.PlayerScreen
import com.gallbladderz.openkick.features.profile.MainViewModel
import com.gallbladderz.openkick.features.search.SearchScreen // <-- ИМПОРТ НАШЕГО НОВОГО ЭКРАНА
import org.koin.androidx.compose.koinViewModel

@Composable
fun OpenKickNavHost(viewModel: MainViewModel = koinViewModel()) {
    val navController = rememberNavController()
    val hideCategories by viewModel.hideCategories.collectAsStateWithLifecycle()

    val bottomNavItems = remember(hideCategories) {
        val items = mutableListOf(
            BottomNavItem("Главная", HOME_ROUTE, Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavItem("Поиск", SEARCH_ROUTE, Icons.Filled.Search, Icons.Outlined.Search)
        )
        if (!hideCategories) {
            items.add(BottomNavItem("Категории", CATEGORIES_ROUTE, Icons.Filled.List, Icons.Filled.List))
        }
        items.add(BottomNavItem("Фолловеры", FOLLOWERS_ROUTE, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder))
        items.add(BottomNavItem("Профиль", PROFILE_ROUTE, Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle))
        items
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showBottomBar = currentRoute?.startsWith("player") != true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.icon else item.selectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
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
            startDestination = HOME_ROUTE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(HOME_ROUTE) {
                HomeScreen(
                    onStreamClick = { streamerName ->
                        navController.navigate("player/$streamerName")
                    }
                )
            }

            composable(SEARCH_ROUTE) {
                SearchScreen(
                    onChannelClick = { streamerName ->
                        navController.navigate("player/$streamerName")
                    }
                )
            }

            composable(CATEGORIES_ROUTE) { CategoriesScreen() }
            composable(FOLLOWERS_ROUTE) { DummyScreen("Фолловеры") }
            composable(PROFILE_ROUTE) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Настройки")
                    Button(onClick = { viewModel.toggleCategories(!hideCategories) }) {
                        Text(if (hideCategories) "Вернуть Категории" else "Скрыть Категории")
                    }
                }
            }

            composable(PLAYER_ROUTE) { backStackEntry ->
                val streamerName = backStackEntry.arguments?.getString("streamerName") ?: ""
                PlayerScreen(
                    streamerName = streamerName,
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