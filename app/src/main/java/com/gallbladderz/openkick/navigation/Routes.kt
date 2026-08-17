/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
object MainTabsRoute

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
data class StreamerProfileRoute(val slug: String)

data class BottomNavItem(
    @StringRes val titleResId: Int,
    val route: Any,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

@Serializable
data class ClipPlayerRoute(
    val clipId: String
)

@Serializable
data class VodPlayerRoute(
    val videoId: String
)
