package com.gallbladderz.openkick.core.network

import com.gallbladderz.openkick.features.categories.CategoryDetailsResponse
import com.gallbladderz.openkick.features.home.ClipResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface KickApiService {

    // Для списка всех категорий оставляем сырой ответ,
    // чтобы не сломать твою старую CategoriesViewModel
    @GET("api/v1/subcategories")
    suspend fun getCategoriesRaw(
        @Query("limit") limit: Int = 50,
        @Query("page") page: Int = 1
    ): ResponseBody

    // Детали конкретной категории (уже с красивым маппингом)
    @GET("api/v1/subcategories/{slug}")
    suspend fun getCategoryDetails(
        @Path("slug") slug: String
    ): CategoryDetailsResponse

    // Клипы категории
    @GET("api/v2/categories/{slug}/clips")
    suspend fun getCategoryClips(
        @Path("slug") slug: String,
        @Query("sort") sort: String = "view",
        @Query("time") time: String = "week"
    ): ClipResponse
}