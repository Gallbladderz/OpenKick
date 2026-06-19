package com.gallbladderz.openkick.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FollowedEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun followsDao(): FollowsDao
}