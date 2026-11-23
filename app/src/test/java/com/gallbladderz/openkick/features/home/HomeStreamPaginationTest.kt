/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.home

import com.gallbladderz.openkick.core.domain.model.StreamUiModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeStreamPaginationTest {
    @Test
    fun fillsFeedPastShortFirstPageAndFullyHiddenSecondPage() = runBlocking {
        val requested = mutableListOf<String?>()
        val pages = mapOf(
            null to (listOf(stream("first"), stream("casino", "slots")) to "second"),
            "second" to (listOf(stream("more-casino", "slots")) to "third"),
            "third" to (listOf(stream("last")) to "fourth")
        )

        val (streams, cursor) = loadVisibleStreamPages(null, setOf("slots"), minimumStreams = 2) {
            requested.add(it)
            Result.success(pages.getValue(it))
        }.getOrThrow()

        assertEquals(listOf(null, "second", "third"), requested)
        assertEquals(listOf("first", "last"), streams.map { it.id })
        assertEquals("fourth", cursor)
    }

    @Test
    fun skipsPreviouslyDisplayedStreamsAndStopsAtEndOrRepeatedCursor() = runBlocking {
        for (endCursor in listOf(null, "third")) {
            val pages = mapOf<String?, Pair<List<StreamUiModel>, String?>>(
                "second" to (listOf(stream("already-shown")) to "third"),
                "third" to (listOf(stream("casino", "slots")) to endCursor)
            )

            val (streams, cursor) = loadVisibleStreamPages(
                cursor = "second",
                hiddenCategories = setOf("slots"),
                minimumStreams = 1,
                existingIds = setOf("already-shown")
            ) { Result.success(pages.getValue(it)) }.getOrThrow()

            assertEquals(emptyList<StreamUiModel>(), streams)
            assertNull(cursor)
        }
    }

    @Test
    fun keepsPartialResultsAndFailedCursorForRetry() = runBlocking {
        val (streams, cursor) = loadVisibleStreamPages(null, setOf("slots"), minimumStreams = 2) {
            if (it == null) Result.success(listOf(stream("first")) to "retry")
            else Result.failure(IllegalStateException("Network error"))
        }.getOrThrow()

        assertEquals(listOf("first"), streams.map { it.id })
        assertEquals("retry", cursor)
    }

    private fun stream(id: String, category: String = "just-chatting") = StreamUiModel(
        id = id,
        streamerName = id,
        title = id,
        viewers = 0,
        category = category,
        categorySlug = category,
        thumbnailUrl = ""
    )
}
