package com.gallbladderz.openkick.features.search

import com.gallbladderz.openkick.core.domain.DomainError
import com.gallbladderz.openkick.core.domain.toDomainError
import com.gallbladderz.openkick.core.network.KickApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class SearchRepository(private val apiService: KickApiService) {
    fun searchStreamer(query: String): Flow<Result<List<SearchUiModel>>> = flow {
        try {
            val response = apiService.searchChannels(query)
            val channels = response.data?.channels?.map { it.toDomain() } ?: emptyList()

            if (channels.isEmpty()) {
                emit(Result.failure(DomainError.ApiError("Ничего не найдено")))
            } else {
                emit(Result.success(channels))
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
        isLive = this.isActuallyLive
    )
}