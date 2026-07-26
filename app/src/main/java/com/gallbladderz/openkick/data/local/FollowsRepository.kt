package com.gallbladderz.openkick.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FollowsRepository(private val dao: FollowsDao) {

    fun getFollowedCategoriesSlugs(): Flow<List<String>> {
        return dao.getAllFollows()
            .map { list -> list.filter { it.type == FollowType.CATEGORY }.map { it.slug } }
            .flowOn(Dispatchers.IO)
    }

    fun getFollowedStreamersSlugs(): Flow<List<String>> {
        return dao.getAllFollows()
            .map { list -> list.filter { it.type == FollowType.STREAMER }.map { it.slug } }
            .flowOn(Dispatchers.IO)
    }

    fun isCategoryFollowed(slug: String): Flow<Boolean> = dao.isFollowed(slug, FollowType.CATEGORY).flowOn(Dispatchers.IO)

    suspend fun toggleCategoryFollow(slug: String, isCurrentlyFollowed: Boolean) = withContext(Dispatchers.IO) {
        if (isCurrentlyFollowed) {
            dao.delete(slug, FollowType.CATEGORY)
        } else {
            dao.insert(FollowedEntity(slug, FollowType.CATEGORY))
        }
    }

    fun isStreamerFollowed(slug: String): Flow<Boolean> = dao.isFollowed(slug, FollowType.STREAMER).flowOn(Dispatchers.IO)

    suspend fun toggleStreamerFollow(slug: String, isCurrentlyFollowed: Boolean) = withContext(Dispatchers.IO) {
        if (isCurrentlyFollowed) {
            dao.delete(slug, FollowType.STREAMER)
        } else {
            dao.insert(FollowedEntity(slug, FollowType.STREAMER))
        }
    }
}