/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [FollowedEntity::class, DownloadEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun followsDao(): FollowsDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS downloads (
                        id TEXT NOT NULL PRIMARY KEY, mediaType TEXT NOT NULL, sourceId TEXT NOT NULL,
                        title TEXT NOT NULL, channelName TEXT NOT NULL, thumbnailUrl TEXT NOT NULL,
                        qualityLabel TEXT NOT NULL, qualityKey TEXT, startSec INTEGER, endSec INTEGER,
                        filePath TEXT, fileSizeBytes INTEGER NOT NULL, status TEXT NOT NULL,
                        progress INTEGER NOT NULL, errorMessage TEXT, createdAt INTEGER NOT NULL,
                        workId TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloads ADD COLUMN durationSec INTEGER")
                db.execSQL("ALTER TABLE downloads ADD COLUMN channelAvatarUrl TEXT")
            }
        }
    }
}
