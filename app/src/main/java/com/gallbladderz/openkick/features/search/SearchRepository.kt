package com.gallbladderz.openkick.features.search

import com.gallbladderz.openkick.core.network.KickApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class SearchRepository(private val apiService: KickApiService) {
    fun searchStreamer(query: String): Flow<Result<List<SearchUiModel>>> = flow {
        try {
            val response = apiService.searchChannels(query)
            val channels = response.data?.channels?.map { dto ->
                SearchUiModel(
                    username = dto.slug,
                    profilePic = dto.profilePic?.replace("\\/", "/") ?: "",
                    isLive = dto.isActuallyLive
                )
            } ?: emptyList()

            if (channels.isEmpty()) {
                emit(Result.failure(Exception("Ничего не найдено")))
            } else {
                emit(Result.success(channels))
            }
        } catch (e: Exception) {
            emit(Result.failure(Exception("Ошибка сети: ${e.message}", e)))
        }
    }.flowOn(Dispatchers.IO)
}