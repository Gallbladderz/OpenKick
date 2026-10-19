/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.api

import com.gallbladderz.openkick.core.network.dto.following.FollowedChannelsResponse
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FollowingApi {
    @GET("api/v2/channels/followed")
    suspend fun followedChannels(
        @Query("cursor") cursor: Int? = null
    ): FollowedChannelsResponse

    @POST("api/v2/channels/{slug}/follow")
    suspend fun follow(@Path("slug") slug: String)

    @DELETE("api/v2/channels/{slug}/follow")
    suspend fun unfollow(@Path("slug") slug: String)
}

