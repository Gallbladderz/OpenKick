/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.following


import com.gallbladderz.openkick.core.data.local.FollowType
import com.gallbladderz.openkick.core.data.local.FollowedEntity
import com.gallbladderz.openkick.core.data.local.FollowsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalFollowingDataSource(private val dao: FollowsDao) {
    fun entities(): Flow<List<FollowedEntity>> = dao.getAllFollows()
    fun streamers(): Flow<List<FollowedEntity>> = entities().map { list ->
        list.filter { it.type == FollowType.STREAMER }
    }

    fun categories(): Flow<List<FollowedEntity>> = entities().map { list ->
        list.filter { it.type == FollowType.CATEGORY }
    }

    suspend fun save(entity: FollowedEntity) = dao.insert(entity)
    suspend fun delete(slug: String, type: FollowType) = dao.delete(slug, type)
}
