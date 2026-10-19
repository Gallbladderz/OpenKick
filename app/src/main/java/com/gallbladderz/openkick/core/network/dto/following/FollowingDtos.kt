/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.dto.following

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class RemoteFollowedChannel(
    @JsonNames("slug", "channel_slug")
    val slug: String = "",
    @JsonNames("username", "user_username")
    val username: String = "",
    @SerialName("profile_pic")
    @JsonNames("profile_pic", "profile_picture")
    val profilePic: String = "",
    @JsonNames("is_live")
    val isLive: Boolean = false,
    @JsonNames("session_title")
    val sessionTitle: String = "",
    @JsonNames("viewer_count")
    val viewerCount: Int = 0,
    @JsonNames("category_name")
    val categoryName: String = ""
)

@Serializable
data class FollowedChannelsResponse(
    val channels: List<RemoteFollowedChannel> = emptyList(),
    val nextCursor: Int? = null
)

