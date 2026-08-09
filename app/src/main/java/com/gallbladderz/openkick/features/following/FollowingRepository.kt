/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.following

import com.gallbladderz.openkick.core.domain.toDomainError
import com.gallbladderz.openkick.core.network.KickApiService
import com.gallbladderz.openkick.features.categories.CategoryDetailsResponse
import com.gallbladderz.openkick.features.profile.ChannelV1Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FollowingRepository(private val apiService: KickApiService) {

    suspend fun fetchChannelDetails(slug: String): Result<FollowedStreamerUi> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getChannelV1(slug)
                Result.success(response.toFollowedStreamerDomain(slug))
            } catch (e: Exception) {
                Result.failure(e.toDomainError())
            }
        }

    suspend fun fetchCategoryDetails(slug: String): Result<FollowedCategoryUi> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCategoryDetails(slug)
                Result.success(response.toFollowedCategoryDomain(slug))
            } catch (e: Exception) {
                Result.failure(e.toDomainError())
            }
        }
}

fun ChannelV1Response.toFollowedStreamerDomain(slugFallback: String): FollowedStreamerUi {
    val username = this.user?.username ?: this.slug ?: slugFallback
    val avatarUrl = this.user?.profilePic ?: ""

    return if (this.livestream != null) {
        val title = this.livestream.sessionTitle ?: "Untitled"
        val viewers = this.livestream.viewerCount
        val categoryName = this.livestream.category?.name ?: ""
        val streamThumbnailUrl = this.livestream.thumbnail?.finalUrl ?: ""

        FollowedStreamerUi(
            slugFallback,
            username,
            avatarUrl,
            true,
            title,
            viewers,
            categoryName,
            streamThumbnailUrl
        )
    } else {
        FollowedStreamerUi(slugFallback, username, avatarUrl, false)
    }
}

fun CategoryDetailsResponse.toFollowedCategoryDomain(slugFallback: String): FollowedCategoryUi {
    var bannerUrl = this.banner?.finalUrl ?: ""
    bannerUrl = bannerUrl.replace("\\/", "/")
    if (bannerUrl.contains(" ")) {
        bannerUrl = bannerUrl.split(",").firstOrNull()?.trim()?.substringBefore(" ") ?: bannerUrl
    }

    return FollowedCategoryUi(slugFallback, this.name, bannerUrl, this.viewers)
}