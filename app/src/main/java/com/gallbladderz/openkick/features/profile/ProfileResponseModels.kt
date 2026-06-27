package com.gallbladderz.openkick.features.profile

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChannelV1Response(
    val id: Int? = null,
    val slug: String? = null,
    val user: ChannelV1User? = null,
    @SerialName("banner_image") val bannerImage: JsonElement? = null, 
    val followersCount: Int = 0,
    val livestream: ChannelV1Livestream? = null
)

@Serializable
data class ChannelV1User(
    val username: String? = null,
    @SerialName("profile_pic") val profilePic: String? = null,
    val bio: String? = null
)

@Serializable
data class ChannelV1Livestream(
    @SerialName("session_title") val sessionTitle: String? = null,
    @SerialName("viewer_count") val viewerCount: Int = 0,
    val category: ChannelV1Category? = null,
    val thumbnail: ChannelV1Thumbnail? = null
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
data class VideoDto(
    val id: String? = null,
    @SerialName("is_live") val isLive: Boolean = false,
    @SerialName("session_title") val sessionTitle: String? = null,
    val views: Int = 0,
    val source: String? = null,
    val duration: Long = 0L,
    val thumbnail: VideoThumbnailDto? = null,
    val video: VideoInnerDto? = null
) {
    
    val actualId: String get() = video?.uuid ?: id ?: ""
}

@Serializable
data class VideoThumbnailDto(
    val src: String? = null
)

@Serializable
data class VideoInnerDto(
    val uuid: String? = null
)