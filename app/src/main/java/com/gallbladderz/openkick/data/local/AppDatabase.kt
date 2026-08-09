/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FollowedEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun followsDao(): FollowsDao
}