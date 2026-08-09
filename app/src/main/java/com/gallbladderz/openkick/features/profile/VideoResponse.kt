/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VideoItemDto(
    val id: Int? = null,
    val video: VideoInnerDto? = null,
    @SerialName("session_title") val sessionTitle: String? = null,
    val title: String? = null,
    val duration: Long = 0L,
    val is_live: Boolean = false,
    val viewer_count: Int = 0,
    val thumbnail: VideoThumbnailItemDto? = null,
    val channel: VideoChannelDto? = null,
    val start_time: String? = null,
    val end_time: String? = null,
    val language: String? = null
)

@Serializable
data class VideoThumbnailItemDto(
    val src: String? = null,
    val srcSet: String? = null
)

@Serializable
data class VideoChannelDto(
    val id: Int = 0,
    val slug: String = "",
    val username: String? = null
)