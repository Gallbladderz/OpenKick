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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gallbladderz.openkick.MainActivity
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.data.local.DownloadDao
import com.gallbladderz.openkick.core.data.local.DownloadEntity
import com.gallbladderz.openkick.core.data.local.DownloadMediaType
import com.gallbladderz.openkick.core.data.local.DownloadStatus
import com.gallbladderz.openkick.core.data.repository.ClipRepository
import com.gallbladderz.openkick.core.domain.chat.ChatReplaySource
import com.gallbladderz.openkick.core.network.api.KickApiService
import com.gallbladderz.openkick.features.profile.StreamerProfileRepository
import java.io.File
import java.io.IOException
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

@UnstableApi
internal class DownloadWorker(
    context: Context,
    parameters: WorkerParameters,
    private val dao: DownloadDao,
    private val engine: DownloadEngine,
    private val clips: ClipRepository,
    private val videos: StreamerProfileRepository,
    private val api: KickApiService
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
            val metadata = when (entry.mediaType) {
                DownloadMediaType.CLIP -> clips.fetchClip(entry.sourceId).getOrThrow().let {
                    DownloadMetadata(it.videoUrl, it.chatReplay, null, null)
                }
                DownloadMediaType.VOD -> videos.fetchVideoPlayback(entry.sourceId, entry.title).getOrThrow().let {
                    DownloadMetadata(it.videoUrl, it.chatReplay, it.broadcastEndedAt, it.durationFormatted)
                }
            }
            // Persist the chat source even if the chat download below fails, so the
            // offline player can fall back to the online chat replay for this video.
            metadata.chatReplay?.let { source ->
                dao.updateChatSource(entryId, source.channelId, source.startedAt.toString())
            }
            ensureActive()
            var lastProgress = 0
            val file = engine.download(metadata.videoUrl, entry, directory) { progress ->
                if (progress == lastProgress) return@download
                if (dao.updateActive(entryId, workId, DownloadStatus.DOWNLOADING, progress) == 0) {
                    throw CancellationException()
                }
                lastProgress = progress
                setForeground(notification(entry.title, progress))
            }
            // Chat replay is best-effort: a failure here must never fail the video.
            downloadChatReplayBestEffort(entry, metadata, directory)
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

    /**
     * Download the chat replay for the downloaded range into per-hour chunk
     * files (`chat-0000.json`, …) next to the video. Best-effort: any failure
     * is logged and the video download still succeeds — chunks already
     * written are kept, and the player falls back to the online chat replay
     * when possible. A full 10h chat is ~2,400 windows / ~7,200 API calls,
     * so it takes a while and may hit Kick rate limits; that only loses the
     * remaining chunks, never the video.
     */
    private suspend fun downloadChatReplayBestEffort(
        entry: DownloadEntity,
        metadata: DownloadMetadata,
        directory: File
    ) {
        val source = metadata.chatReplay ?: return
        try {
            val broadcastEndSec = metadata.broadcastEndedAt?.let {
                Duration.between(source.startedAt, it).seconds
            }
            val range = offlineChatRangeMs(
                entry.startSec, entry.endSec,
                entry.durationSec ?: broadcastEndSec,
                metadata.durationFormatted
            ) ?: run {
                Log.i("DownloadWorker", "Skipping chat replay for ${entry.id}: unknown download end")
                return
            }
            val result = downloadChatReplayChunked(source, range.first, range.second, directory) { timestamp ->
                api.getChatReplay(source.channelId, timestamp.toString()).data
            }
            if (result.failedChunk != null) {
                Log.w(
                    "DownloadWorker",
                    "Chat replay download stopped at chunk ${result.failedChunk} for ${entry.id} " +
                        "(${result.totalMessages} messages in ${result.chunksWritten.size} chunks kept)"
                )
            } else if (result.totalMessages > 0) {
                Log.i(
                    "DownloadWorker",
                    "Chat replay saved for ${entry.id}: ${result.totalMessages} messages " +
                        "in ${result.chunksWritten.size} chunks"
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("DownloadWorker", "Chat replay download failed for ${entry.id}", e)
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

private data class DownloadMetadata(
    val videoUrl: String,
    val chatReplay: ChatReplaySource?,
    val broadcastEndedAt: Instant?,
    /** Duration formatted as "MM:SS"/"H:MM:SS"; only a last-resort chat-range fallback. */
    val durationFormatted: String?
)
