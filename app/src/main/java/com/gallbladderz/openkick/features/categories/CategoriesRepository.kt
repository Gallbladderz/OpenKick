package com.gallbladderz.openkick.features.categories

import com.gallbladderz.openkick.core.network.KickApiService
import com.gallbladderz.openkick.features.home.ClipUiModel
import com.gallbladderz.openkick.features.home.toUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class CategoriesRepository(private val apiService: KickApiService) {

    fun fetchCategories(page: Int = 1): Flow<Result<List<CategoryUiModel>>> = flow {
        try {
            val response = apiService.getCategories(limit = 50, page = page)

            val uiModels = response.data.map { dto ->
                var bannerUrl = dto.banner?.finalUrl ?: ""
                bannerUrl = bannerUrl.replace("\\/", "/")
                if (bannerUrl.contains(" ")) {
                    bannerUrl = bannerUrl.split(",").firstOrNull()?.trim()?.substringBefore(" ") ?: bannerUrl
                }
                if (bannerUrl.startsWith("/")) bannerUrl = "https://kick.com$bannerUrl"

                CategoryUiModel(
                    id = dto.id?.toString() ?: "0",
                    name = dto.name,
                    slug = dto.slug,
                    viewers = dto.viewers,
                    bannerUrl = bannerUrl,
                    tags = dto.tags
                )
            }
            emit(Result.success(uiModels))
        } catch (e: Exception) {
            emit(Result.failure(Exception("Network died: ${e.message}", e)))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun fetchCategoryDetails(slug: String): CategoryDetailsResponse = withContext(Dispatchers.IO) {
        apiService.getCategoryDetails(slug)
    }

    suspend fun fetchCategoryClips(slug: String): List<ClipUiModel> = withContext(Dispatchers.IO) {
        apiService.getCategoryClips(slug).clips.map { it.toUiModel() }
    }
}