package com.gallbladderz.openkick.features.player

import com.gallbladderz.openkick.core.network.KickApiConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class PlayerRepository(private val client: OkHttpClient) {
    fun fetchStreamInfo(streamerName: String): Flow<Result<String>> = flow {
        val request = Request.Builder()
            .url("${KickApiConstants.KICK_API_V2_BASE_URL}/channels/$streamerName")
            .build()
        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                emit(Result.failure(Exception("Kick отбил запрос: код ${response.code}")))
                return@flow
            }
            emit(Result.success(responseBody))
        } catch (e: IOException) {
            emit(Result.failure(e))
        }
    }
}
