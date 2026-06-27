package com.gallbladderz.openkick.features.categories

import kotlinx.serialization.Serializable

@Serializable
data class CategoryDetailsResponse(
    val name: String = "Category",
    val viewers: Int = 0,
    val tags: List<String> = emptyList(),
    val banner: CategoryBannerDto? = null
)

@Serializable
data class CategoryBannerDto(
    val srcset: String? = null,
    val responsive: String? = null
) {
    
    val finalUrl: String
        get() = responsive ?: srcset ?: ""
}