package com.gallbladderz.openkick.features.categories

import com.gallbladderz.openkick.core.domain.toDomainError
import com.gallbladderz.openkick.core.network.KickApiService
import com.gallbladderz.openkick.features.home.ClipUiModel
import com.gallbladderz.openkick.features.home.StreamUiModel
import com.gallbladderz.openkick.features.home.toDomain
import com.gallbladderz.openkick.features.home.toUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

data class CategoryDetailsUiModel(
    val name: String,
    val viewers: Int,
    val tags: List<String>,
    val bannerUrl: String
)

class CategoriesRepository(private val apiService: KickApiService) {

    fun fetchCategories(page: Int = 1): Flow<Result<List<CategoryUiModel>>> = flow {
        try {
            val response = apiService.getCategories(limit = 50, page = page)
            val uiModels = response.data.map { it.toDomain() }
            emit(Result.success(uiModels))
        } catch (e: Exception) {
            emit(Result.failure(e.toDomainError()))
        }
    }.flowOn(Dispatchers.IO)

    suspend fun fetchCategoryDetails(slug: String): Result<CategoryDetailsUiModel> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCategoryDetails(slug)
                Result.success(response.toDomain())
            } catch (e: Exception) {
                Result.failure(e.toDomainError())
            }
        }

    suspend fun fetchCategoryClips(
        slug: String,
        cursor: String? = null
    ): Result<Pair<List<ClipUiModel>, String?>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCategoryClips(slug = slug, cursor = cursor)
                val uiModels = response.actualClips.map { it.toUiModel() }
                val nextCursor = response.actualCursor
                Result.success(Pair(uiModels, if (nextCursor.isNullOrBlank()) null else nextCursor))
            } catch (e: Exception) {
                Result.failure(e.toDomainError())
            }
        }

    suspend fun fetchCategoryStreams(
        slug: String,
        cursor: String? = null,
        sort: String = "viewer_count_desc",
        languages: List<String>? = null
    ): Result<Pair<List<StreamUiModel>, String?>> =
        withContext(Dispatchers.IO) {
            try {
                val response =
                    apiService.getCategoryLivestreams(subcategorySlug = slug, cursor = cursor, sort = sort, languages = languages)
                val streams =
                    response.data?.livestreams?.mapNotNull { it.toDomain() } ?: emptyList()
                val nextCursor = response.data?.pagination?.nextCursor
                Result.success(Pair(streams, if (nextCursor.isNullOrBlank()) null else nextCursor))
            } catch (e: Exception) {
                Result.failure(e.toDomainError())
            }
        }
}

fun CategoryDto.toDomain(): CategoryUiModel {
    var bannerUrl = this.banner?.finalUrl ?: ""
    bannerUrl = bannerUrl.replace("\\/", "/")
    if (bannerUrl.contains(" ")) {
        bannerUrl = bannerUrl.split(",").firstOrNull()?.trim()?.substringBefore(" ") ?: bannerUrl
    }
    if (bannerUrl.startsWith("/")) bannerUrl = "https://kick.com$bannerUrl"

    return CategoryUiModel(
        id = this.id?.toString() ?: "0",
        name = this.name,
        slug = this.slug,
        viewers = this.viewers,
        bannerUrl = bannerUrl,
        tags = this.tags
    )
}

fun CategoryDetailsResponse.toDomain(): CategoryDetailsUiModel {
    var bannerUrl = this.banner?.srcset ?: ""
    if (bannerUrl.contains(" ")) {
        bannerUrl = bannerUrl.split(",").firstOrNull()?.trim()?.substringBefore(" ") ?: bannerUrl
    }

    return CategoryDetailsUiModel(
        name = this.name,
        viewers = this.viewers,
        tags = this.tags,
        bannerUrl = bannerUrl
    )
}