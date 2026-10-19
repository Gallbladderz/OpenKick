/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.api

import androidx.annotation.Keep
import com.gallbladderz.openkick.core.network.dto.following.RemoteFollowedCategory
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

@Keep
interface CategoryFollowingApi {
    @GET("api/v1/user/categories/top")
    suspend fun followedCategories(): List<RemoteFollowedCategory>

    @Headers("Content-Type: application/json")
    @POST("api/v1/subcategories/{slug}/toggle-follow")
    suspend fun toggleFollow(@Path("slug") slug: String): Response<ResponseBody>
}

