package com.gallbladderz.openkick.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FollowsRepository(private val dao: FollowsDao) {

    fun getFollowedCategoriesSlugs(): Flow<List<String>> {
        return dao.getAllFollows()
            .map { list -> list.filter { it.type == FollowType.CATEGORY }.map { it.slug } }
    }

    fun getFollowedStreamersSlugs(): Flow<List<String>> {
        return dao.getAllFollows()
            .map { list -> list.filter { it.type == FollowType.STREAMER }.map { it.slug } }
    }

    fun isCategoryFollowed(slug: String): Flow<Boolean> = dao.isFollowed(slug, FollowType.CATEGORY)

    suspend fun toggleCategoryFollow(slug: String, isCurrentlyFollowed: Boolean) {
        if (isCurrentlyFollowed) {
            dao.delete(slug, FollowType.CATEGORY)
        } else {
            dao.insert(FollowedEntity(slug, FollowType.CATEGORY))
        }
    }

    fun isStreamerFollowed(slug: String): Flow<Boolean> = dao.isFollowed(slug, FollowType.STREAMER)

    suspend fun toggleStreamerFollow(slug: String, isCurrentlyFollowed: Boolean) {
        if (isCurrentlyFollowed) {
            dao.delete(slug, FollowType.STREAMER)
        } else {
            dao.insert(FollowedEntity(slug, FollowType.STREAMER))
        }
    }
}