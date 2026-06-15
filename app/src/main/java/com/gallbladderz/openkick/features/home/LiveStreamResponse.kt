package com.gallbladderz.openkick.features.home

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class LiveStreamResponse(
    val data: List<LiveStream> = emptyList()
)

@Serializable
data class LiveStream(
    @SerialName("session_title")
    val sessionTitle: String = "",
    val viewers: Int = 0,
    val thumbnail: Thumbnail? = null,
    val channel: Channel? = null,
    val categories: List<Category> = emptyList()
)

@Serializable
data class Thumbnail(
    val url: String = ""
)

@Serializable
data class Channel(
    val user: User? = null
)

@Serializable
data class User(
    val username: String = "",
    @SerialName("profile_pic")
    val profilePic: String? = null
)

@Serializable
data class Category(
    val name: String = ""
)
