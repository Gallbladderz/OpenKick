/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.navigation

import kotlinx.serialization.Serializable

@Serializable
object MainTabsRoute

@Serializable
object SearchRoute

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
object DownloadsRoute

@Serializable
object LicensesRoute

sealed interface KickLinkDestination

@Serializable
data class CategoryDetailsRoute(val slug: String) : KickLinkDestination

@Serializable
data class StreamerProfileRoute(val slug: String) : KickLinkDestination

@Serializable
data class ClipPlayerRoute(
    val clipId: String
) : KickLinkDestination

@Serializable
data class VodPlayerRoute(
    val videoId: String,
    val channelSlug: String? = null
) : KickLinkDestination
