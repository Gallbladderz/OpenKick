/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.following


import com.gallbladderz.openkick.core.auth.AppMode
import com.gallbladderz.openkick.core.auth.SessionManager
import com.gallbladderz.openkick.core.data.local.FollowType
import com.gallbladderz.openkick.core.data.local.FollowedEntity
import com.gallbladderz.openkick.core.domain.toDomainError
import com.gallbladderz.openkick.core.network.api.KickApiService
import com.gallbladderz.openkick.core.network.dto.categories.CategoryDetailsResponse
import com.gallbladderz.openkick.core.network.dto.profile.ChannelV1Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class FollowingRepository(
    private val apiService: KickApiService,
    private val local: LocalFollowingDataSource,
    private val remote: RemoteFollowingDataSource,
    private val remoteCategories: RemoteCategoryFollowingDataSource,
    private val session: SessionManager
) {
    private val refreshMutex = Mutex()
    private val categoryRefreshMutex = Mutex()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun followedEntities(): Flow<List<FollowedEntity>> = session.mode.flatMapLatest { mode ->
        if (mode == AppMode.AUTHENTICATED) {
            combine(remoteCategories.categories, remote.channels) { categories, channels ->
                categories.map { category ->
                    FollowedEntity(
                        slug = category.slug,
                        type = FollowType.CATEGORY,
                        categoryName = category.name
                    )
                } + channels.map {
                    FollowedEntity(
                        slug = it.slug,
                        type = FollowType.STREAMER,
                        isLive = it.isLive,
                        username = it.username.ifBlank { it.slug },
                        avatarUrl = it.profilePic,
                        streamTitle = it.sessionTitle,
                        viewers = it.viewerCount,
                        categoryName = it.categoryName
                    )
                }
            }
        } else local.entities()
    }

    fun isStreamerFollowed(slug: String): Flow<Boolean> = followedEntities().map { entities ->
        entities.any { it.type == FollowType.STREAMER && it.slug.equals(slug, ignoreCase = true) }
    }

    fun followedCategorySlugs(): Flow<Set<String>> = followedEntities().map { entities ->
        entities.filter { it.type == FollowType.CATEGORY }.map { it.slug.lowercase() }.toSet()
    }

    suspend fun toggleCategoryFollow(slug: String, currentlyFollowed: Boolean) {
        if (session.mode.value == AppMode.AUTHENTICATED) {
            remoteCategories.setFollowed(slug, !currentlyFollowed)
        } else if (currentlyFollowed) local.delete(slug, FollowType.CATEGORY)
        else local.save(FollowedEntity(slug, FollowType.CATEGORY))
    }

    suspend fun toggleStreamerFollow(slug: String, currentlyFollowed: Boolean) {
        if (session.mode.value == AppMode.AUTHENTICATED) {
            if (currentlyFollowed) remote.unfollow(slug) else remote.follow(slug)
        } else if (currentlyFollowed) local.delete(slug, FollowType.STREAMER)
        else local.save(FollowedEntity(slug, FollowType.STREAMER))
    }

    suspend fun refreshFollowing() = refreshMutex.withLock {
        if (session.mode.value != AppMode.AUTHENTICATED) return@withLock
        remote.refresh()

        try {
            refreshCategories()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {

        }
    }

    suspend fun refreshCategories() = categoryRefreshMutex.withLock {
        if (session.mode.value != AppMode.AUTHENTICATED) return@withLock
        val generation = session.sessionGeneration.value
        remoteCategories.refresh()

        for (category in local.categories().first()) {
            check(session.mode.value == AppMode.AUTHENTICATED && session.sessionGeneration.value == generation) {
                "Session changed while moving followed categories"
            }
            remoteCategories.setFollowed(category.slug, true)
            check(session.mode.value == AppMode.AUTHENTICATED && session.sessionGeneration.value == generation) {
                "Session changed while moving followed categories"
            }
            local.delete(category.slug, FollowType.CATEGORY)
        }
    }

    fun localStreamers(): Flow<List<FollowedEntity>> = local.streamers()
    val isAuthenticated: Boolean get() = session.mode.value == AppMode.AUTHENTICATED

    suspend fun saveLocalSnapshot(entity: FollowedEntity) = local.save(entity)

    suspend fun migrateLocalToRemote() {
        check(session.mode.value == AppMode.AUTHENTICATED) { "Sign in before migrating follows" }
        remote.refresh()
        val generation = session.sessionGeneration.value
        val existing = remote.currentChannels().map { it.slug }.toSet()
        for (entity in local.streamers().first()) {
            check(session.mode.value == AppMode.AUTHENTICATED && session.sessionGeneration.value == generation) {
                "Session changed while migrating subscriptions"
            }
            if (entity.slug !in existing) remote.follow(entity.slug)
            check(session.mode.value == AppMode.AUTHENTICATED && session.sessionGeneration.value == generation) {
                "Session changed while migrating subscriptions"
            }
            local.delete(entity.slug, FollowType.STREAMER)
        }
    }

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
