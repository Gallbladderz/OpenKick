package com.gallbladderz.openkick.features.home

data class StreamUiModel(
    val id: String,
    val streamerName: String,
    val title: String,
    val viewers: Int,
    val category: String,
    val thumbnailUrl: String
)
