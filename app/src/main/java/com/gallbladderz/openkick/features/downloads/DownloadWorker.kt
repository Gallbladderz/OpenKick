/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.downloads

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gallbladderz.openkick.MainActivity
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.data.local.DownloadDao
import com.gallbladderz.openkick.core.data.local.DownloadMediaType
import com.gallbladderz.openkick.core.data.local.DownloadStatus
import com.gallbladderz.openkick.core.data.repository.ClipRepository
import com.gallbladderz.openkick.features.profile.StreamerProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException

@UnstableApi
internal class DownloadWorker(
    context: Context,
    parameters: WorkerParameters,
    private val dao: DownloadDao,
    private val engine: DownloadEngine,
    private val clips: ClipRepository,
    private val videos: StreamerProfileRepository
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val entryId = inputData.getString(DOWNLOAD_ID) ?: return@withContext Result.failure()
        val entry = dao.get(entryId) ?: return@withContext Result.failure()
        val workId = id.toString()
        if (dao.updateActive(entryId, workId, DownloadStatus.DOWNLOADING, 0) == 0) {
            return@withContext Result.success()
        }
        val directory = downloadDirectory(applicationContext, entry)
        try {
            directory.deleteRecursively()
            if (!directory.mkdirs()) throw IOException()
            setForeground(notification(entry.title, 0))
            val url = when (entry.mediaType) {
                DownloadMediaType.CLIP -> clips.fetchClip(entry.sourceId).getOrThrow().videoUrl
                DownloadMediaType.VOD -> videos.fetchVideoPlayback(entry.sourceId, entry.title).getOrThrow().videoUrl
            }
            ensureActive()
            var lastProgress = 0
            val file = engine.download(url, entry, directory) { progress ->
                if (progress == lastProgress) return@download
                if (dao.updateActive(entryId, workId, DownloadStatus.DOWNLOADING, progress) == 0) {
                    throw CancellationException()
                }
                lastProgress = progress
                setForeground(notification(entry.title, progress))
            }
            val size = directory.listFiles().orEmpty().sumOf { it.length() }
            dao.complete(entryId, workId, file.absolutePath, size)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = when (e.message) {
                "range" -> R.string.download_range_error
                "quality" -> R.string.download_quality_unavailable
                "unsupported" -> R.string.download_unsupported
                else -> R.string.download_error
            }
            dao.updateActive(entryId, workId, DownloadStatus.FAILED, dao.get(entryId)?.progress ?: 0,
                applicationContext.getString(message))
            Result.failure()
        } finally {
            withContext(NonCancellable) {
                val persisted = dao.get(entryId)
                if (persisted?.workId != workId || persisted.status != DownloadStatus.COMPLETED) {
                    directory.deleteRecursively()
                }
            }
        }
    }

    private fun notification(title: String, progress: Int): ForegroundInfo {
        val context = applicationContext
        val channel = "downloads"
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(channel, context.getString(R.string.downloads), NotificationManager.IMPORTANCE_LOW)
        )
        val openApp = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val content = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.download_in_progress))
            .setProgress(100, progress, progress == 0)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(openApp)
            .addAction(0, context.getString(R.string.download_cancel),
                WorkManager.getInstance(context).createCancelPendingIntent(id))
            .build()
        val notificationId = (id.hashCode() and Int.MAX_VALUE).coerceAtLeast(1)
        return if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(notificationId, content, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, content)
        }
    }

    companion object {
        const val DOWNLOAD_ID = "downloadId"
    }
}
