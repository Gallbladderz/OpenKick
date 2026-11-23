/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.network.dto.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class VideoItemDto(
    val id: Int? = null,
    val video: VideoInnerDto? = null,
    @SerialName("session_title") val sessionTitle: String? = null,
    val title: String? = null,
    val duration: Long = 0L,
    val is_live: Boolean = false,
    val viewer_count: Int = 0,
    val thumbnail: JsonElement? = null,
    val channel: VideoChannelDto? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val language: String? = null,
    val source: String? = null,
    val categories: List<VideoCategoryDto>? = null,
    val category: VideoCategoryDto? = null,
    @SerialName("channel_id") val channelId: Long? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class VideoCategoryDto(val name: String? = null)

@Serializable
data class VideoChannelDto(
    val id: Int = 0,
    val slug: String = "",
    val username: String? = null,
    val user: VideoChannelUserDto? = null,
    @SerialName("profile_picture") val profilePicture: String? = null,
    @SerialName("profile_pic") val profilePic: String? = null
) {
    val avatar: String get() =
        sequenceOf(profilePicture, profilePic, user?.profilePic, user?.profilepic, user?.profilePicture)
            .filterNotNull().firstOrNull { it.isNotBlank() }.orEmpty()
}

@Serializable
data class VideoChannelUserDto(
    val username: String? = null,
    val profilepic: String? = null,
    @SerialName("profile_pic") val profilePic: String? = null,
    @SerialName("profile_picture") val profilePicture: String? = null
)

@Serializable
data class VideoPlaybackResponse(
    val source: String? = null,
    @SerialName("playback_url") val playbackUrl: String? = null,
    @SerialName("video_url") val videoUrl: String? = null,
    val url: String? = null,
    val views: Int? = null,
    val livestream: VideoItemDto? = null
)

@Serializable
data class WebVideoPlaybackResponse(val data: WebVideoPlaybackDto)

@Serializable
data class WebVideoPlaybackDto(
    @SerialName("recording_url") val recordingUrl: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("viewer_count") val viewerCount: Int = 0,
    val title: String? = null,
    val duration: Long = 0L,
    val channel: VideoChannelDto? = null,
    val category: VideoCategoryDto? = null,
    val thumbnail: JsonElement? = null
)
