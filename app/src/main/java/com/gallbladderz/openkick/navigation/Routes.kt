package com.gallbladderz.openkick.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object SearchRoute

@Serializable
object CategoriesRoute

@Serializable
object FollowersRoute

@Serializable
object ProfileRoute

@Serializable
object AllFollowsRoute

@Serializable
data class CategoryDetailsRoute(val slug: String)

@Serializable
data class PlayerRoute(val streamerName: String)

data class BottomNavItem(
    @StringRes val titleResId: Int,
    val route: Any,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)