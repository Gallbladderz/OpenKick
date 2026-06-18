package com.gallbladderz.openkick.features.categories

data class CategoryUiModel(
    val id: String,
    val name: String,
    val slug: String,
    val viewers: Int,
    val bannerUrl: String
)