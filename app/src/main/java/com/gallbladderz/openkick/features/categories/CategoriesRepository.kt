package com.gallbladderz.openkick.features.categories

import com.gallbladderz.openkick.core.network.KickApiConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class CategoriesRepository(private val client: OkHttpClient) {
    fun fetchCategories(page: Int = 1): Flow<Result<String>> = flow {
        val request = Request.Builder()

            .url("${KickApiConstants.KICK_API_BASE_URL}/subcategories?limit=50&page=$page")
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                emit(Result.failure(Exception("Categories API error: ${response.code}")))
                return@flow
            }

            emit(Result.success(responseBody))
        } catch (e: IOException) {
            emit(Result.failure(Exception("Network died: ${e.message}", e)))
        }
    }
}