package com.gallbladderz.openkick.features.search

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement

@Serializable
data class SearchResponse(
    val data: SearchData? = null
)

@Serializable
data class SearchData(
    val channels: List<SearchChannelDto> = emptyList()
)

@Serializable
data class SearchChannelDto(
    val slug: String,
    @SerialName("profile_pic")
    val profilePic: String? = null,
    @SerialName("is_live")
    val isLive: Boolean = false,
    
    val livestream: JsonElement? = null
) {
    val isActuallyLive: Boolean
        get() = isLive || livestream != null
}