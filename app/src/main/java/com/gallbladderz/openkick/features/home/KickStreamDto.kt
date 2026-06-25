package com.gallbladderz.openkick.features.home

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class KickStreamDto(
    val id: Int? = null,
    @SerialName("session_title") val sessionTitle: String = "Untitled",
    val viewers: Int = 0,
    val channel: KickChannelDto? = null,
    val category: KickCategoryDto? = null,
    val thumbnail: KickThumbnailDto? = null
)

@Serializable
data class KickChannelDto(
    val slug: String = "unknown"
)

@Serializable
data class KickCategoryDto(
    val name: String = "Just Chatting"
)

@Serializable
data class KickThumbnailDto(
    val url: String = ""
)