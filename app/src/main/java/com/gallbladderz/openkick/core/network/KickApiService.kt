package com.gallbladderz.openkick.core.network

import com.gallbladderz.openkick.features.categories.CategoryDetailsResponse
import com.gallbladderz.openkick.features.home.HomeLivestreamsResponse
import com.gallbladderz.openkick.features.home.TopClipsResponse
import com.gallbladderz.openkick.features.profile.VideoItemDto
import com.gallbladderz.openkick.features.search.SearchResponse
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface KickApiService {


    @GET("api/v1/subcategories")
    suspend fun getCategories(
        @Query("limit") limit: Int = 50,
        @Query("page") page: Int = 1
    ): com.gallbladderz.openkick.features.categories.CategoriesResponse

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


    @GET("api/v2/channels/{slug}")
    suspend fun getChannelStreamInfo(
        @Path("slug") slug: String
    ): com.gallbladderz.openkick.features.player.models.ChannelStreamInfoResponse

    @GET("api/v1/channels/{slug}/links")
    suspend fun getChannelLinks(
        @Path("slug") slug: String
    ): List<com.gallbladderz.openkick.features.player.models.ChannelLinkDto>


    @GET("api/v1/channels/{slug}")
    suspend fun getChannelV1(
        @Path("slug") slug: String
    ): com.gallbladderz.openkick.features.profile.ChannelV1Response

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
        @Query("subcategory") subcategorySlug: String,
        @Query("limit") limit: Int = 20,
        @Query("sort") sort: String = "viewer_count_desc",
        @Query("after") cursor: String? = null
    ): HomeLivestreamsResponse

    @GET("api/v1/video/{videoId}")
    suspend fun getVideoPlayback(
        @Path("videoId") videoId: String
    ): JsonElement
}

