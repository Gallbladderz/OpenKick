package com.gallbladderz.openkick.features.home

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClipResponse(
    val clips: List<ClipDto> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
    val cursor: String? = null
)

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
    val user: ClipUserDto? = null
)

@Serializable
data class ClipChannelDto(
    val slug: String? = null,
    val username: String? = null,
    @SerialName("profile_pic") val profilePic: String? = null,
    @SerialName("profilepic") val profilePicLegacy: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val user: ClipUserDto? = null
) {
    val displayName: String get() = slug ?: username ?: user?.displayName ?: ""
    val avatar: String get() = profilePic ?: profilePicLegacy ?: avatarUrl ?: user?.avatar ?: ""
}

@Serializable
data class ClipUserDto(
    val username: String? = null,
    val slug: String? = null,
    @SerialName("profile_pic") val profilePic: String? = null,
    @SerialName("profilepic") val profilePicLegacy: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
) {
    val displayName: String get() = slug ?: username ?: ""
    val avatar: String get() = profilePic ?: profilePicLegacy ?: avatarUrl ?: ""
}
