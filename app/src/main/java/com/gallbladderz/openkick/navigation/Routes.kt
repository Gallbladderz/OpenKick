package com.gallbladderz.openkick.navigation

import androidx.compose.ui.graphics.vector.ImageVector

// Routes
const val HOME_ROUTE = "home"
const val CATEGORIES_ROUTE = "categories"
const val FOLLOWERS_ROUTE = "followers"
const val PROFILE_ROUTE = "profile"
const val PLAYER_ROUTE = "player/{streamerName}"
const val SEARCH_ROUTE = "search"

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)