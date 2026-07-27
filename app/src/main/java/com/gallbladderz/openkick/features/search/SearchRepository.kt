package com.gallbladderz.openkick.features.search

import com.gallbladderz.openkick.core.domain.DomainError
import com.gallbladderz.openkick.core.domain.toDomainError
import com.gallbladderz.openkick.core.network.KickApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

data class SearchResultData(
    val channels: List<SearchUiModel>,
    val streams: List<SearchStreamUiModel>,
    val categories: List<SearchCategoryUiModel>
)

class SearchRepository(private val apiService: KickApiService) {
    fun searchStreamer(query: String): Flow<Result<SearchResultData>> = flow {
        try {
            val response = apiService.searchChannels(query)
            val channels = response.data?.channels?.filter { it.slug.isNotBlank() }?.map { it.toDomain() } ?: emptyList()
            val streams = response.data?.livestreams?.filter { it.slug.isNotBlank() }?.map { it.toDomain() } ?: emptyList()
            val categories = response.data?.categories?.filter { it.slug.isNotBlank() }?.map { it.toDomain() } ?: emptyList()

            if (channels.isEmpty() && streams.isEmpty() && categories.isEmpty()) {
                emit(Result.failure(DomainError.ApiError("Ничего не найдено")))
            } else {
                emit(Result.success(SearchResultData(channels, streams, categories)))
            }
        } catch (e: Exception) {
            emit(Result.failure(e.toDomainError()))
        }
    }.flowOn(Dispatchers.IO)
}

fun SearchChannelDto.toDomain(): SearchUiModel {
    return SearchUiModel(
        username = this.slug,
        profilePic = this.profilePic?.replace("\\/", "/") ?: "",
        isLive = this.isLive
    )
}

fun SearchLivestreamDto.toDomain(): SearchStreamUiModel {
    return SearchStreamUiModel(
        slug = this.slug,
        title = this.title,
        thumbnailUrl = this.thumbnail?.finalUrl?.replace("\\/", "/") ?: ""
    )
}

fun SearchCategoryDto.toDomain(): SearchCategoryUiModel {
    return SearchCategoryUiModel(
        name = this.name,
        slug = this.slug,
        thumbnailUrl = this.thumbnail?.finalUrl?.replace("\\/", "/") ?: ""
    )
}
