package com.gallbladderz.openkick.features.search

import com.gallbladderz.openkick.core.network.KickApiConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class SearchRepository(private val client: OkHttpClient) {
    fun searchStreamer(query: String): Flow<Result<String>> = flow {
        val request = Request.Builder()
            .url("${KickApiConstants.KICK_SEARCH_API_BASE_URL}/search/enriched?query=${android.net.Uri.encode(query)}")
            .build()
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                emit(Result.failure(Exception("Поиск упал: ${response.code}")))
                return@flow
            }

            emit(Result.success(responseBody))
        } catch (e: IOException) {
            emit(Result.failure(Exception("Ошибка сети", e)))
        }
    }
}
