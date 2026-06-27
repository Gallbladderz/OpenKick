package com.gallbladderz.openkick.features.categories

import kotlinx.serialization.Serializable

@Serializable
data class CategoriesResponse(
    val data: List<CategoryDto> = emptyList()
)

@Serializable
data class CategoryDto(
    val id: Int? = null,
    val name: String = "Untitled",
    val slug: String = "",
    val viewers: Int = 0,
    val tags: List<String> = emptyList(),
    val banner: CategoryBannerDto? = null
)