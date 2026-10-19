/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.following


import com.gallbladderz.openkick.core.auth.AppMode
import com.gallbladderz.openkick.core.auth.SessionManager
import com.gallbladderz.openkick.core.network.api.FollowingApi
import com.gallbladderz.openkick.core.network.dto.following.RemoteFollowedChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import java.util.concurrent.atomic.AtomicLong

class RemoteFollowingDataSource(
    private val api: FollowingApi,
    private val session: SessionManager
) {
    private val loaded = MutableStateFlow<Pair<Long, List<RemoteFollowedChannel>>?>(null)
    private val requestId = AtomicLong()
    val channels: Flow<List<RemoteFollowedChannel>> = combine(
        session.mode, session.sessionGeneration, loaded
    ) { mode, generation, result ->
        visibleChannels(mode, generation, result)
    }

    fun currentChannels(): List<RemoteFollowedChannel> =
        visibleChannels(session.mode.value, session.sessionGeneration.value, loaded.value)

    suspend fun refresh() {
        val generation = session.sessionGeneration.value
        check(session.mode.value == AppMode.AUTHENTICATED) { "Sign in to load subscriptions" }
        val request = requestId.incrementAndGet()
        loaded.value = null

        val allChannels = mutableListOf<RemoteFollowedChannel>()
        var cursor: Int? = null
        do {
            val response = api.followedChannels(cursor)
            allChannels.addAll(response.channels)
            cursor = response.nextCursor
        } while (cursor != null)

        val result = allChannels
            .filter { it.slug.isNotBlank() }
            .distinctBy { it.slug.lowercase() }

        check(session.mode.value == AppMode.AUTHENTICATED && session.sessionGeneration.value == generation && requestId.get() == request) {
            "Session changed while loading subscriptions"
        }
        loaded.value = generation to result
    }

    suspend fun follow(slug: String) {
        mutate { api.follow(slug) }
    }

    suspend fun unfollow(slug: String) {
        mutate { api.unfollow(slug) }
    }

    private suspend fun mutate(action: suspend () -> Unit) {
        val generation = session.sessionGeneration.value
        check(session.mode.value == AppMode.AUTHENTICATED) { "Sign in to change subscriptions" }
        action()
        check(session.mode.value == AppMode.AUTHENTICATED && session.sessionGeneration.value == generation) {
            "Session changed while updating subscriptions"
        }
        refresh()
    }
}

internal fun visibleChannels(
    mode: AppMode,
    generation: Long,
    loaded: Pair<Long, List<RemoteFollowedChannel>>?
): List<RemoteFollowedChannel> =
    if (mode == AppMode.AUTHENTICATED && loaded?.first == generation) loaded.second else emptyList()
