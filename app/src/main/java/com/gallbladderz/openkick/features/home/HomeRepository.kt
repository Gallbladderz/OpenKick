package com.gallbladderz.openkick.features.home

import com.gallbladderz.openkick.core.network.KickApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeRepository(private val apiService: KickApiService) {

    suspend fun fetchLivestreams(
        cursor: String? = null,
        languages: Set<String> = emptySet()
    ): Result<Pair<List<StreamUiModel>, String?>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getHomeLivestreams(
                cursor = cursor,
                languages = if (languages.isEmpty()) null else languages.toList()
            )

            val uiModels = response.data?.livestreams?.mapNotNull { item ->
                val stream = item.actualStream
                StreamUiModel(
                    id = stream.id ?: "0",
                    streamerName = stream.channel?.slug ?: stream.channel?.username ?: "Unknown",
                    title = stream.sessionTitle,
                    viewers = stream.viewerCount,
                    category = stream.category?.name ?: "No Category",
                    thumbnailUrl = stream.thumbnail?.finalUrl ?: ""
                )
            } ?: emptyList()

            val nextCursor = response.data?.pagination?.nextCursor

            Result.success(Pair(uiModels, if (nextCursor.isNullOrBlank()) null else nextCursor))
        } catch (e: Exception) {
            Result.failure(Exception("Network dropped (Streams): ${e.message}", e))
        }
    }

    suspend fun fetchTopClips(cursor: String? = null): Result<Pair<List<ClipUiModel>, String?>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTopClips(cursor = cursor)
            val uiModels = response.actualClips.map { it.toUiModel() }
            val nextCursor = response.actualCursor

            Result.success(Pair(uiModels, if (nextCursor.isNullOrBlank()) null else nextCursor))
        } catch (e: Exception) {
            Result.failure(Exception("Network error (Clips): ${e.message}", e))
        }
    }
}