/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.following


import com.gallbladderz.openkick.core.auth.AppMode
import com.gallbladderz.openkick.core.auth.SessionManager
import com.gallbladderz.openkick.core.network.api.FollowingApi
import com.gallbladderz.openkick.core.network.dto.following.RemoteFollowedChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RemoteFollowingDataSource(
    private val api: FollowingApi,
    private val session: SessionManager
) {
    private val loaded = MutableStateFlow<Pair<Long, List<RemoteFollowedChannel>>?>(null)
    private val requestMutex = Mutex()
    val channels: Flow<List<RemoteFollowedChannel>> = combine(
        session.mode, session.sessionGeneration, loaded
    ) { mode, generation, result ->
        visibleChannels(mode, generation, result)
    }

    fun currentChannels(): List<RemoteFollowedChannel> =
        visibleChannels(session.mode.value, session.sessionGeneration.value, loaded.value)

    suspend fun refresh(generation: Long = session.sessionGeneration.value) {
        requestMutex.withLock { refreshUnderLock(generation) }
    }

    private suspend fun refreshUnderLock(generation: Long): List<RemoteFollowedChannel> {
        val allChannels = mutableListOf<RemoteFollowedChannel>()
        var cursor: Int? = null
        do {
            checkSession(generation)
            val response = api.followedChannels(cursor)
            allChannels.addAll(response.channels)
            cursor = response.nextCursor
        } while (cursor != null)

        val result = allChannels
            .filter { it.slug.isNotBlank() }
            .distinctBy { it.slug.lowercase() }

        checkSession(generation)
        loaded.value = generation to result
        return result
    }

    internal suspend fun followForMigration(channel: RemoteFollowedChannel, generation: Long) {
        requestMutex.withLock {
            checkSession(generation)
            if (currentChannels().any { it.slug.equals(channel.slug, ignoreCase = true) }) return@withLock

            confirmChannelFollow(
                slug = channel.slug,
                follow = { api.follow(channel.slug) },
                reload = { refreshUnderLock(generation) }
            )
            checkSession(generation)
            val current = currentChannels()
            if (current.none { it.slug.equals(channel.slug, ignoreCase = true) }) {
                loaded.value = generation to (current + channel)
            }
        }
    }

    suspend fun follow(slug: String) {
        mutate { api.follow(slug) }
    }

    suspend fun unfollow(slug: String) {
        mutate { api.unfollow(slug) }
    }

    private suspend fun mutate(action: suspend () -> Unit) {
        val generation = session.sessionGeneration.value
        requestMutex.withLock {
            checkSession(generation)
            action()
            refreshUnderLock(generation)
        }
    }

    private fun checkSession(generation: Long) {
        check(session.mode.value == AppMode.AUTHENTICATED && session.sessionGeneration.value == generation) {
            "Session changed while updating subscriptions"
        }
    }
}

internal suspend fun confirmChannelFollow(
    slug: String,
    follow: suspend () -> Unit,
    reload: suspend () -> List<RemoteFollowedChannel>
) {
    try {
        follow()
    } catch (e: CancellationException) {
        throw e
    } catch (error: Exception) {
        val channels = try {
            reload()
        } catch (e: CancellationException) {
            throw e
        } catch (refreshError: Exception) {
            if (error !== refreshError) error.addSuppressed(refreshError)
            throw error
        }
        if (channels.none { it.slug.equals(slug, ignoreCase = true) }) throw error
    }
}

internal fun visibleChannels(
    mode: AppMode,
    generation: Long,
    loaded: Pair<Long, List<RemoteFollowedChannel>>?
): List<RemoteFollowedChannel> =
    if (mode == AppMode.AUTHENTICATED && loaded?.first == generation) loaded.second else emptyList()
