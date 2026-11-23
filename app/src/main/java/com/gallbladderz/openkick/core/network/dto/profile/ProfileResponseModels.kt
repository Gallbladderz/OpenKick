/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.network.dto.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

@Serializable
data class ChannelV1Response(
    val id: Int? = null,
    val slug: String? = null,
    val user: ChannelV1User? = null,
    @SerialName("banner_image") val bannerImage: JsonElement? = null,
    val followersCount: Int = 0,
    val livestream: ChannelV1Livestream? = null,
    val verified: JsonElement? = null,
    @SerialName("previous_livestreams") val previousLivestreams: List<PreviousLivestreamDto>? = null
) {
    val isVerified: Boolean
        get() = verified is JsonObject || (verified as? JsonPrimitive)?.booleanOrNull == true
}

@Serializable
data class PreviousLivestreamDto(
    @SerialName("start_time") val startedAt: String? = null,
    @SerialName("end_time") val endedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("is_live") val isLive: Boolean = false,
    val duration: Long? = null
)

@Serializable
data class ChannelV1User(
    val username: String? = null,
    @SerialName("profile_pic") val profilePic: String? = null,
    val bio: String? = null,
    val youtube: String? = null,
    val instagram: String? = null,
    val twitter: String? = null,
    val tiktok: String? = null,
    val discord: String? = null,
    val facebook: String? = null,
    val website: String? = null
)

@Serializable
data class ChannelV1Livestream(
    @SerialName("session_title") val sessionTitle: String? = null,
    @SerialName("viewer_count") val viewerCount: Int? = null,
    val category: ChannelV1Category? = null,
    val thumbnail: ChannelV1Thumbnail? = null,
    val categories: List<ChannelV1Category>? = null
)

@Serializable
data class ChannelV1Category(
    val name: String? = null
)

@Serializable
data class ChannelV1Thumbnail(
    val url: String? = null,
    val src: String? = null
) {
    val finalUrl: String get() = url ?: src ?: ""
}

@Serializable
data class VideoThumbnailDto(
    val src: String? = null
)

@Serializable
data class VideoInnerDto(
    val uuid: String? = null
)

@Serializable
data class ChannelInfoV2Response(val verified: Boolean = false)
