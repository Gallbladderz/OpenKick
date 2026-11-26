/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    abstract fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    abstract suspend fun get(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING')")
    abstract suspend fun getActive(): List<DownloadEntity>

    @Query("""
        SELECT * FROM downloads WHERE mediaType = :type AND sourceId = :sourceId
          AND qualityKey IS :qualityKey AND startSec IS :startSec AND endSec IS :endSec LIMIT 1
    """)
    protected abstract suspend fun find(
        type: DownloadMediaType, sourceId: String, qualityKey: String?, startSec: Long?, endSec: Long?
    ): DownloadEntity?

    @Insert
    protected abstract suspend fun insert(download: DownloadEntity)

    @Transaction
    open suspend fun insertOrGet(download: DownloadEntity): DownloadEntity {
        return find(download.mediaType, download.sourceId, download.qualityKey, download.startSec, download.endSec)
            ?: download.also { insert(it) }
    }

    @Query("""
        UPDATE downloads SET status = 'QUEUED', workId = :workId, progress = 0, errorMessage = NULL
        WHERE id = :id AND status IN ('FAILED', 'CANCELLED')
    """)
    abstract suspend fun retry(id: String, workId: String): Int

    @Query("""
        UPDATE downloads SET status = :status, progress = :progress, errorMessage = :error
        WHERE id = :id AND workId = :workId AND status IN ('QUEUED', 'DOWNLOADING')
    """)
    abstract suspend fun updateActive(
        id: String, workId: String, status: DownloadStatus, progress: Int, error: String? = null
    ): Int

    @Query("""
        UPDATE downloads SET channelId = :channelId, chatStartedAt = :chatStartedAt
        WHERE id = :id
    """)
    abstract suspend fun updateChatSource(id: String, channelId: Long?, chatStartedAt: String?): Int

    @Query("""
        UPDATE downloads SET status = 'COMPLETED', progress = 100, filePath = :path, fileSizeBytes = :bytes
        WHERE id = :id AND workId = :workId AND status = 'DOWNLOADING'
    """)
    abstract suspend fun complete(id: String, workId: String, path: String, bytes: Long): Int

    @Query("DELETE FROM downloads WHERE id = :id AND status NOT IN ('QUEUED', 'DOWNLOADING')")
    abstract suspend fun deleteInactive(id: String)
}
