/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.chat

import com.gallbladderz.openkick.core.network.api.SevenTvApi
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SevenTvRepositoryTest {
    @Test
    fun cachesGlobalAndMissingChannelEntriesByOwnerId() = runBlocking {
        val api = FakeSevenTvApi()
        val repository = SevenTvRepository(api)

        repeat(2) {
            assertTrue(repository.globalEmotes().isEmpty())
            assertTrue(repository.channelEmotes(8).isEmpty())
            assertTrue(repository.channelEmotes(9).isEmpty())
        }

        assertEquals(1, api.globalRequests)
        assertEquals(listOf(8L, 9L), api.ownerIds)
    }

    @Test
    fun retriesTransientNetworkFailures() = runBlocking {
        val api = FakeSevenTvApi()
        val repository = SevenTvRepository(api)
        api.networkError = true
        assertTrue(repository.channelEmotes(8).isEmpty())
        api.networkError = false
        assertTrue(repository.channelEmotes(8).isEmpty())
        assertTrue(repository.channelEmotes(8).isEmpty())
        assertEquals(listOf(8L, 8L), api.ownerIds)
    }

    private class FakeSevenTvApi : SevenTvApi {
        var globalRequests = 0
        val ownerIds = mutableListOf<Long>()
        var networkError = false

        override suspend fun global(): Response<SevenTvEmoteSetDto> {
            globalRequests++
            return Response.success(SevenTvEmoteSetDto())
        }

        override suspend fun channel(kickUserId: Long): Response<SevenTvConnectionDto> {
            ownerIds += kickUserId
            if (networkError) throw IOException("offline")
            return Response.error(404, "".toResponseBody())
        }
    }
}
