package com.gallbladderz.openkick.features.home

import com.gallbladderz.openkick.core.domain.toDomainError
import com.gallbladderz.openkick.core.network.KickApiService
import com.gallbladderz.openkick.features.player.ClipRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HomeRepository(
    private val apiService: KickApiService,
    private val clipRepository: ClipRepository
) {

    suspend fun fetchLivestreams(
        cursor: String? = null,
        sort: String = "viewer_count_desc",
        languages: Set<String> = emptySet()
    ): Result<Pair<List<StreamUiModel>, String?>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getHomeLivestreams(
                cursor = cursor,
                sort = sort,
                languages = if (languages.isEmpty()) null else languages.toList()
            )

            val uiModels = response.data?.livestreams?.mapNotNull { it.toDomain() } ?: emptyList()

            val nextCursor = response.data?.pagination?.nextCursor

            Result.success(Pair(uiModels, if (nextCursor.isNullOrBlank()) null else nextCursor))
        } catch (e: Exception) {
            Result.failure(e.toDomainError())
        }
    }

    suspend fun fetchTopClips(cursor: String? = null): Result<Pair<List<ClipUiModel>, String?>> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getTopClips(cursor = cursor)
                val uiModels = response.actualClips.map { it.toUiModel() }
                val nextCursor = response.actualCursor

                clipRepository.cacheClips(uiModels)

                Result.success(Pair(uiModels, if (nextCursor.isNullOrBlank()) null else nextCursor))
            } catch (e: Exception) {
                Result.failure(e.toDomainError())
            }
        }
}

fun HomeLivestreamItem.toDomain(): StreamUiModel {
    val stream = this.actualStream
    return StreamUiModel(
        id = stream.id ?: "0",
        streamerName = stream.channel?.slug ?: stream.channel?.username ?: "Unknown",
        title = stream.sessionTitle,
        viewers = stream.viewerCount,
        category = stream.category?.name ?: "No Category",
        categorySlug = stream.category?.slug ?: "",
        thumbnailUrl = stream.thumbnail?.finalUrl ?: ""
    )
}