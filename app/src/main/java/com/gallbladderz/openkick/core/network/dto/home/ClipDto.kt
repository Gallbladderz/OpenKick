/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.network.dto.home

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClipDto(
    val id: String,
    val title: String,
    @SerialName("clip_url") val clipUrl: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String,
    val views: Int,
    val duration: Int,
    @SerialName("created_at") val createdAt: String,
    val channel: ClipChannelDto? = null,
    val creator: ClipChannelDto? = null,
    val user: ClipUserDto? = null,
    val category: ClipCategoryDto? = null,
    @SerialName("channel_id") val channelId: Long? = null,
    @SerialName("started_at") val startedAt: String? = null
)

@Serializable
data class ClipCategoryDto(val name: String? = null)

@Serializable
data class ClipChannelDto(
    val slug: String? = null,
    val username: String? = null,
    @SerialName("profile_picture") val profilePicture: String? = null,
    @SerialName("profile_pic") val profilePic: String? = null,
    @SerialName("profilepic") val profilePicLegacy: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val user: ClipUserDto? = null,
    val id: Long? = null
) {
    val displayName: String get() = slug ?: username ?: user?.displayName ?: ""
    val avatar: String get() = sequenceOf(profilePicture, profilePic, profilePicLegacy, avatarUrl, user?.avatar)
        .filterNotNull().firstOrNull { it.isNotBlank() }.orEmpty()
}

@Serializable
data class ClipUserDto(
    val username: String? = null,
    val slug: String? = null,
    @SerialName("profile_picture") val profilePicture: String? = null,
    @SerialName("profile_pic") val profilePic: String? = null,
    @SerialName("profilepic") val profilePicLegacy: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
) {
    val displayName: String get() = slug ?: username ?: ""
    val avatar: String get() = sequenceOf(profilePicture, profilePic, profilePicLegacy, avatarUrl)
        .filterNotNull().firstOrNull { it.isNotBlank() }.orEmpty()
}
