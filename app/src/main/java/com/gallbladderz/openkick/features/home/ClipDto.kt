package com.gallbladderz.openkick.features.home

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClipResponse(
    val clips: List<ClipDto>
)

@Serializable
data class ClipDto(
    val id: String,
    val title: String,
    @SerialName("clip_url") val clipUrl: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String,
    val views: Int,
    val duration: Int,
    @SerialName("created_at") val createdAt: String
)