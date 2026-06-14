package com.gallbladderz.openkick.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.koin.androidx.compose.koinViewModel
import com.gallbladderz.openkick.features.profile.MainViewModel

@Composable
fun OpenKickNavHost(viewModel: MainViewModel = koinViewModel()) {
    val navController = rememberNavController()
    val hideCategories by viewModel.hideCategories.collectAsStateWithLifecycle()

    val bottomNavItems = remember(hideCategories) {
        val items = mutableListOf(
            BottomNavItem("Главная", HOME_ROUTE, Icons.Filled.Home, Icons.Outlined.Home)
        )
        if (!hideCategories) {
            items.add(BottomNavItem("Категории", CATEGORIES_ROUTE, Icons.Filled.List, Icons.Filled.List))
        }
        items.add(BottomNavItem("Фолловеры", FOLLOWERS_ROUTE, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder))
        items.add(BottomNavItem("Профиль", PROFILE_ROUTE, Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle))
        items
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HOME_ROUTE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(HOME_ROUTE) { DummyScreen("Главная: Лента стримов") }
            composable(CATEGORIES_ROUTE) { DummyScreen("Категории") }
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
        }
    }
}

@Composable
fun DummyScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title)
    }
}