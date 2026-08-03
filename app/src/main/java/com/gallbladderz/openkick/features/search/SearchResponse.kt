package com.gallbladderz.openkick.features.search

import com.gallbladderz.openkick.features.home.KickThumbnailFallback
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val data: SearchData? = null
)

@Serializable
data class SearchData(
    val channels: List<SearchChannelDto> = emptyList(),
    val categories: List<SearchCategoryDto> = emptyList(),
    val livestreams: List<SearchLivestreamDto> = emptyList()
)

@Serializable
data class SearchChannelDto(
    val slug: String = "",
    @SerialName("profile_picture")
    val profilePic: String? = null,
    @SerialName("is_live")
    val isLive: Boolean = false
)

@Serializable
data class SearchCategoryDto(
    val name: String = "",
    val slug: String = "",
    @SerialName("viewer_count")
    val viewerCount: Int = 0,
    val tags: List<String> = emptyList(),
    val thumbnail: KickThumbnailFallback? = null
)

@Serializable
data class SearchLivestreamDto(
    val slug: String = "",
    val title: String = "",
    val session_title: String? = null,
    @SerialName("viewer_count")
    val viewerCount: Int = 0,
    val category: SearchCategoryDto? = null,
    val channel: SearchChannelDto? = null,
    val thumbnail: KickThumbnailFallback? = null
)