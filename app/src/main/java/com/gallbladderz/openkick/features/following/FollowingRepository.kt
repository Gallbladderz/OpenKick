package com.gallbladderz.openkick.features.following

import android.util.Log
import com.gallbladderz.openkick.core.network.KickApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FollowingRepository(private val apiService: KickApiService) {

    suspend fun fetchChannelDetails(slug: String): FollowedStreamerUi? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getChannelV1(slug)

            val username = response.user?.username ?: response.slug ?: slug
            val avatarUrl = response.user?.profilePic ?: ""

            if (response.livestream != null) {
                val title = response.livestream.sessionTitle ?: "Untitled"
                val viewers = response.livestream.viewerCount
                val categoryName = response.livestream.category?.name ?: ""
                val streamThumbnailUrl = response.livestream.thumbnail?.finalUrl ?: ""

                FollowedStreamerUi(slug, username, avatarUrl, true, title, viewers, categoryName, streamThumbnailUrl)
            } else {
                FollowedStreamerUi(slug, username, avatarUrl, false)
            }
        } catch (e: Exception) {
            Log.e("FollowingRepo", "Error loading channel $slug: ${e.message}")
            null
        }
    }

    suspend fun fetchCategoryDetails(slug: String): FollowedCategoryUi? = withContext(Dispatchers.IO) {
        try {
            
            val response = apiService.getCategoryDetails(slug)
            val name = response.name
            val viewers = response.viewers

            var bannerUrl = response.banner?.finalUrl ?: ""
            bannerUrl = bannerUrl.replace("\\/", "/")
            if (bannerUrl.contains(" ")) {
                bannerUrl = bannerUrl.split(",").firstOrNull()?.trim()?.substringBefore(" ") ?: bannerUrl
            }

            FollowedCategoryUi(slug, name, bannerUrl, viewers)
        } catch (e: Exception) {
            Log.e("FollowingRepo", "Error loading category $slug: ${e.message}")
            null
        }
    }
}