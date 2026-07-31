package com.gallbladderz.openkick.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "follows")
data class FollowedEntity(
    @PrimaryKey val slug: String,
    val type: FollowType,
    val isLive: Boolean = false,
    val username: String = "",
    val avatarUrl: String = "",
    val streamTitle: String = "",
    val viewers: Int = 0,
    val categoryName: String = "",
    val bannerUrl: String = ""
)

enum class FollowType {
    STREAMER, CATEGORY
}