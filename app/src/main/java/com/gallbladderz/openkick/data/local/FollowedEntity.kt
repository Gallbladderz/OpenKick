package com.gallbladderz.openkick.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "follows")
data class FollowedEntity(
    @PrimaryKey val slug: String,
    val type: FollowType
)

enum class FollowType {
    STREAMER, CATEGORY
}