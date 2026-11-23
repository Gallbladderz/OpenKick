/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.home

import com.gallbladderz.openkick.core.domain.model.StreamUiModel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend fun loadVisibleStreamPages(
    cursor: String?,
    hiddenCategories: Set<String>,
    minimumStreams: Int,
    existingIds: Set<String> = emptySet(),
    fetchPage: suspend (String?) -> Result<Pair<List<StreamUiModel>, String?>>
): Result<Pair<List<StreamUiModel>, String?>> {
    val streams = mutableListOf<StreamUiModel>()
    val seenIds = existingIds.toMutableSet()
    val visitedCursors = mutableSetOf<String?>()
    var nextCursor = cursor

    do {
        currentCoroutineContext().ensureActive()
        val page = fetchPage(nextCursor).getOrElse { error ->
            if (visitedCursors.isEmpty()) return Result.failure(error)
            return Result.success(streams to nextCursor)
        }
        visitedCursors.add(nextCursor)
        val (pageStreams, after) = page
        streams += pageStreams.filter { stream ->
            stream.categorySlug.lowercase() !in hiddenCategories && seenIds.add(stream.id)
        }
        nextCursor = after?.takeIf { it.isNotBlank() && it !in visitedCursors }
    } while (nextCursor != null && streams.size < minimumStreams)

    return Result.success(streams to nextCursor)
}
