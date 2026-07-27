package com.gallbladderz.openkick.features.search

data class SearchUiModel(
    val username: String,
    val profilePic: String,
    val isLive: Boolean
)

data class SearchCategoryUiModel(
    val name: String,
    val slug: String,
    val thumbnailUrl: String
)

data class SearchStreamUiModel(
    val slug: String,
    val title: String,
    val thumbnailUrl: String
)
