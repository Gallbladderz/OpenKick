package com.gallbladderz.openkick.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowsDao {
    @Query("SELECT * FROM follows")
    fun getAllFollows(): Flow<List<FollowedEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM follows WHERE slug = :slug AND type = :type)")
    fun isFollowed(slug: String, type: FollowType): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(followedEntity: FollowedEntity)

    @Query("DELETE FROM follows WHERE slug = :slug AND type = :type")
    suspend fun delete(slug: String, type: FollowType)
}