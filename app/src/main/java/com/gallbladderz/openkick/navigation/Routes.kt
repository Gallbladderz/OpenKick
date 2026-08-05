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
object LanguageSettingsRoute

@Serializable
object NotificationSettingsRoute

@Serializable
object ContentSettingsRoute

@Serializable
object AllFollowsRoute

@Serializable
object AboutAppRoute

@Serializable
object LicensesRoute

@Serializable
data class CategoryDetailsRoute(val slug: String)
@Serializable
data class PlayerRoute(val streamerName: String)

@Serializable
data class StreamerProfileRoute(val slug: String)

data class BottomNavItem(
    @StringRes val titleResId: Int,
    val route: Any,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

@Serializable
data class ClipPlayerRoute(
    val videoUrl: String,
    val title: String,
    val streamerName: String,
    val streamerAvatarUrl: String,
    val views: Int,
    val durationFormatted: String
)
