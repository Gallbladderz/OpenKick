/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val mediaType: DownloadMediaType,
    val sourceId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val qualityLabel: String,
    val qualityKey: String?,
    val startSec: Long?,
    val endSec: Long?,
    val filePath: String? = null,
    val fileSizeBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val workId: String,
    val durationSec: Long? = null,
    val channelAvatarUrl: String? = null
)

enum class DownloadMediaType { CLIP, VOD }
enum class DownloadStatus { QUEUED, DOWNLOADING, COMPLETED, FAILED, CANCELLED }
