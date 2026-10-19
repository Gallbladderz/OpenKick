/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.network.api

import com.gallbladderz.openkick.core.network.dto.categories.CategoriesResponse
import com.gallbladderz.openkick.core.network.dto.categories.CategoryDetailsResponse
import com.gallbladderz.openkick.core.network.dto.home.HomeLivestreamsResponse
import com.gallbladderz.openkick.core.network.dto.home.SingleClipResponse
import com.gallbladderz.openkick.core.network.dto.home.TopClipsResponse
import com.gallbladderz.openkick.core.network.dto.player.ChannelLinkDto
import com.gallbladderz.openkick.core.network.dto.player.ChannelStreamInfoResponse
import com.gallbladderz.openkick.core.network.dto.player.ChatroomDto
import com.gallbladderz.openkick.core.network.dto.profile.ChannelInfoV2Response
import com.gallbladderz.openkick.core.network.dto.profile.ChannelV1Response
import com.gallbladderz.openkick.core.network.dto.profile.VideoItemDto
import com.gallbladderz.openkick.core.network.dto.search.SearchResponse
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface KickApiService {

    @GET("api/v1/subcategories")
    suspend fun getCategories(
        @Query("limit") limit: Int = 50,
        @Query("page") page: Int = 1
    ): CategoriesResponse

    @GET("api/v1/subcategories/{slug}")
    suspend fun getCategoryDetails(
        @Path("slug") slug: String
    ): CategoryDetailsResponse

    @GET("api/v2/categories/{slug}/clips")
    suspend fun getCategoryClips(
        @Path("slug") slug: String,
        @Query("sort") sort: String = "view",
        @Query("time") time: String = "week",
        @Query("cursor") cursor: String? = null
    ): TopClipsResponse

    @GET("https://search.kick.com/api/v1/search/enriched")
    suspend fun searchChannels(
        @Query("query") query: String
    ): SearchResponse

    @GET("https://mobile.kick.com/api/v1/livestreams")
    suspend fun getHomeLivestreams(
        @Query("limit") limit: Int = 24,
        @Query("sort") sort: String = "viewer_count_desc",
        @Query("after") cursor: String? = null,
        @Query("language") languages: List<String>? = null
    ): HomeLivestreamsResponse

    @GET("api/v2/clips")
    suspend fun getTopClips(
        @Query("sort") sort: String = "view",
        @Query("time") time: String = "week",
        @Query("cursor") cursor: String? = null
    ): TopClipsResponse

    @GET("api/v2/clips/{clipId}")
    suspend fun getClip(
        @Path("clipId") clipId: String
    ): SingleClipResponse

    @GET("api/v2/channels/{slug}")
    suspend fun getChannelStreamInfo(
        @Path("slug") slug: String
    ): ChannelStreamInfoResponse

    @GET("api/v2/channels/{slug}/chatroom")
    suspend fun getChannelChatroom(
        @Path("slug") slug: String
    ): ChatroomDto

    @GET("api/v2/channels/{slug}/me")
    suspend fun getChannelMe(
        @Path("slug") slug: String
    ): kotlinx.serialization.json.JsonElement

    @GET("api/v1/channels/{slug}/links")
    suspend fun getChannelLinks(
        @Path("slug") slug: String
    ): List<ChannelLinkDto>

    @GET("api/v1/channels/{slug}")
    suspend fun getChannelV1(
        @Path("slug") slug: String
    ): ChannelV1Response

    @GET("api/v2/channels/{slug}/info")
    suspend fun getChannelInfoV2(
        @Path("slug") slug: String
    ): ChannelInfoV2Response

    @GET("api/v2/channels/{slug}/videos")
    suspend fun getChannelVideos(
        @Path("slug") slug: String
    ): List<VideoItemDto>

    @GET("api/v2/channels/{slug}/clips")
    suspend fun getChannelClips(
        @Path("slug") slug: String,
        @Query("cursor") cursor: String? = null
    ): TopClipsResponse

    @GET("https://mobile.kick.com/api/v1/livestreams")
    suspend fun getCategoryLivestreams(
        @Query("category_id") categoryId: Int,
        @Query("limit") limit: Int = 20,
        @Query("sort") sort: String = "viewer_count_desc",
        @Query("after") cursor: String? = null,
        @Query("language") languages: List<String>? = null
    ): HomeLivestreamsResponse

    @GET("api/v1/video/{videoId}")
    suspend fun getVideoPlayback(
        @Path("videoId") videoId: String
    ): JsonElement

}

