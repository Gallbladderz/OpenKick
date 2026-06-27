package com.gallbladderz.openkick.features.player.models

import kotlinx.serialization.Serializable

@Serializable
data class ChannelStreamInfoResponse(
    val playback_url: String? = null,
    val livestream: LivestreamDto? = null,
    val user: UserDto? = null,
    val chatroom: ChatroomDto? = null,
    val chatroom_id: Int? = null 
)

@Serializable
data class LivestreamDto(
    val playback_url: String? = null,
    val viewer_count: Int = 0,
    val session_title: String = "Stream"
)

@Serializable
data class UserDto(
    val profile_pic: String? = null
)

@Serializable
data class ChatroomDto(
    val id: Int? = null 
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