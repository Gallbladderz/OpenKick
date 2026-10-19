/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.network.dto.player

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChannelStreamInfoResponse(
    val playback_url: String? = null,
    val livestream: LivestreamDto? = null,
    val user: UserDto? = null,
    val chatroom: ChatroomDto? = null,
    val chatroom_id: Int? = null,
    val subscriber_badges: List<SubscriberBadgeDto> = emptyList(),
    val recent_categories: List<CategoryDto> = emptyList(),
    val verified: Boolean = false
)

@Serializable
data class SubscriberBadgeDto(
    val id: Int? = null,
    val months: Int? = null,
    @SerialName("badge_image") val badgeImage: BadgeImageDto? = null
)

@Serializable
data class BadgeImageDto(
    val src: String? = null,
    val srcset: String? = null
)

@Serializable
data class LivestreamDto(
    val id: JsonElement? = null,
    val slug: String? = null,
    val uuid: String? = null,
    val playback_url: String? = null,
    val viewer_count: Int = 0,
    val session_title: String = "Stream",
    val category: CategoryDto? = null
) {
    val parsedId: String?
        get() = (id as? kotlinx.serialization.json.JsonPrimitive)?.content
}

@Serializable
data class UserDto(
    val profile_pic: String? = null
)

@Serializable
data class ChatroomDto(
    val id: Int? = null,
    val chat_mode: String? = null,
    val slow_mode: JsonElement? = null,
    val message_interval: JsonElement? = null,
    val subscribers_mode: JsonElement? = null,
    val followers_mode: JsonElement? = null,
    val enabled: JsonElement? = null,
    val is_enabled: JsonElement? = null,
    val is_subscriber: JsonElement? = null,
    val user_is_subscriber: JsonElement? = null,
    val subscribed: JsonElement? = null
)

@Serializable
data class ChannelLinkDto(
    val id: Int = 0,
    val description: String = "",
    val link: String = "",
    val title: String = "",
    val image: ImageDto? = null
)

@Serializable
data class ImageDto(
    val url: String = ""
)

@Serializable
data class CategoryDto(
    val id: Int = 0,
    val name: String = "",
    val slug: String = ""
)
