package com.gallbladderz.openkick.features.categories

import com.gallbladderz.openkick.core.network.KickApiService
import com.gallbladderz.openkick.features.home.ClipUiModel
import com.gallbladderz.openkick.features.home.toUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class CategoriesRepository(private val apiService: KickApiService) {

    // Старый метод списка категорий, переведенный на Retrofit
    fun fetchCategories(page: Int = 1): Flow<Result<String>> = flow {
        try {
            val response = apiService.getCategoriesRaw(limit = 50, page = page)
            // Достаем строку из сырого ответа, чтобы CategoriesViewModel не подавилась
            emit(Result.success(response.string()))
        } catch (e: Exception) {
            emit(Result.failure(Exception("Network died: ${e.message}", e)))
        }
    }

    // Новые модные методы через саспенды
    suspend fun fetchCategoryDetails(slug: String): CategoryDetailsResponse = withContext(Dispatchers.IO) {
        apiService.getCategoryDetails(slug)
    }

    suspend fun fetchCategoryClips(slug: String): List<ClipUiModel> = withContext(Dispatchers.IO) {
        apiService.getCategoryClips(slug).clips.map { it.toUiModel() }
    }
}