/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.following


import com.gallbladderz.openkick.core.auth.AppMode
import com.gallbladderz.openkick.core.auth.SessionManager
import com.gallbladderz.openkick.core.network.api.CategoryFollowingApi
import com.gallbladderz.openkick.core.network.dto.following.RemoteFollowedCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicLong

class RemoteCategoryFollowingDataSource(
    private val api: CategoryFollowingApi,
    private val session: SessionManager
) {
    private val loaded = MutableStateFlow<Pair<Long, List<RemoteFollowedCategory>>?>(null)
    private val requestId = AtomicLong()
    private val requestMutex = Mutex()

    val categories: Flow<List<RemoteFollowedCategory>> = combine(
        session.mode, session.sessionGeneration, loaded
    ) { mode, generation, result ->
        visibleCategories(mode, generation, result)
    }

    fun currentCategories(): List<RemoteFollowedCategory> =
        visibleCategories(session.mode.value, session.sessionGeneration.value, loaded.value)

    suspend fun refresh() = requestMutex.withLock { refreshUnderLock() }

    private suspend fun refreshUnderLock() {
        val generation = session.sessionGeneration.value
        check(session.mode.value == AppMode.AUTHENTICATED) { "Sign in to load followed categories" }
        val request = requestId.incrementAndGet()
        val result = api.followedCategories()
            .filter { it.slug.isNotBlank() }
            .distinctBy { it.slug.lowercase() }
        check(
            session.mode.value == AppMode.AUTHENTICATED &&
                    session.sessionGeneration.value == generation && requestId.get() == request
        ) { "Session changed while loading followed categories" }
        loaded.value = generation to result
    }

    suspend fun setFollowed(slug: String, shouldFollow: Boolean) = requestMutex.withLock {
        val generation = session.sessionGeneration.value
        check(session.mode.value == AppMode.AUTHENTICATED) { "Sign in to follow categories" }
        if (loaded.value?.first != generation) refreshUnderLock()
        if (currentCategories().any { it.slug.equals(slug, ignoreCase = true) } == shouldFollow) {
            return@withLock
        }
        val response = api.toggleFollow(slug)
        response.body()?.close()
        if (!response.isSuccessful) {
            response.errorBody()?.close()
            error("Could not update category: HTTP ${response.code()}")
        }
        check(session.mode.value == AppMode.AUTHENTICATED && session.sessionGeneration.value == generation) {
            "Session changed while updating a category"
        }

        val current = currentCategories()
        val updated = if (shouldFollow) {
            if (current.any { it.slug.equals(slug, ignoreCase = true) }) current
            else current + RemoteFollowedCategory(slug = slug)
        } else {
            current.filterNot { it.slug.equals(slug, ignoreCase = true) }
        }
        loaded.value = generation to updated
    }
}

internal fun visibleCategories(
    mode: AppMode,
    generation: Long,
    loaded: Pair<Long, List<RemoteFollowedCategory>>?
): List<RemoteFollowedCategory> =
    if (mode == AppMode.AUTHENTICATED && loaded?.first == generation) loaded.second else emptyList()
