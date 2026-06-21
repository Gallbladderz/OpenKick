package com.gallbladderz.openkick.features.home

data class ClipUiModel(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val views: Int,
    val durationFormatted: String
)


fun ClipDto.toUiModel(): ClipUiModel {
    val minutes = duration / 60
    val seconds = duration % 60
    val durationStr = String.format("%02d:%02d", minutes, seconds)

    return ClipUiModel(
        id = id,
        title = title,
        thumbnailUrl = thumbnailUrl,
        videoUrl = clipUrl,
        views = views,
        durationFormatted = durationStr
    )
}