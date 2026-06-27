package com.gallbladderz.openkick.features.home

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HomeLivestreamsResponse(
    val data: HomeLivestreamsData? = null
)

@Serializable
data class HomeLivestreamsData(
    val livestreams: List<HomeLivestreamItem> = emptyList(),
    val pagination: PaginationDto? = null
)

@Serializable
data class PaginationDto(
    @SerialName("next_cursor") val nextCursor: String? = null,
    val cursor: String? = null
)


@Serializable
data class HomeLivestreamItem(
    val livestream: KickStreamItemDto? = null,
    val id: String? = null,
    @SerialName("session_title") val sessionTitle: String? = null,
    val title: String? = null,
    @SerialName("viewer_count") val viewerCount: Int? = null,
    val viewers: Int? = null,
    val channel: KickChannelDto? = null,
    val category: KickCategoryDto? = null,
    val thumbnail: KickThumbnailFallback? = null
) {
    val actualStream: KickStreamItemDto
        get() = livestream ?: KickStreamItemDto(
            id = id,
            sessionTitle = sessionTitle ?: title ?: "Untitled",
            viewerCount = viewerCount ?: viewers ?: 0,
            channel = channel,
            category = category,
            thumbnail = thumbnail
        )
}

@Serializable
data class KickStreamItemDto(
    val id: String? = null,
    @SerialName("session_title") val sessionTitle: String = "Untitled",
    @SerialName("viewer_count") val viewerCount: Int = 0,
    val channel: KickChannelDto? = null,
    val category: KickCategoryDto? = null,
    val thumbnail: KickThumbnailFallback? = null
)

@Serializable
data class KickChannelDto(
    val slug: String? = null,
    val username: String? = null
)

@Serializable
data class KickCategoryDto(
    val name: String = "Just Chatting"
)

@Serializable
data class KickThumbnailFallback(
    val url: String? = null,
    val src: String? = null
) {
    val finalUrl: String get() = url ?: src ?: ""
}

@Serializable
data class TopClipsResponse(
    val clips: List<ClipDto> = emptyList(),
    val data: List<ClipDto> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
    val cursor: String? = null,
    val pagination: PaginationDto? = null
) {
    
    val actualClips: List<ClipDto> get() = clips.ifEmpty { data }
    val actualCursor: String? get() = nextCursor ?: cursor ?: pagination?.nextCursor
}