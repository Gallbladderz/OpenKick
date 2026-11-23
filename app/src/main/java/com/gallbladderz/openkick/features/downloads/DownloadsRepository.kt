/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.downloads

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import androidx.work.workDataOf
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.data.local.DownloadDao
import com.gallbladderz.openkick.core.data.local.DownloadEntity
import com.gallbladderz.openkick.core.data.local.DownloadMediaType
import com.gallbladderz.openkick.core.data.local.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal data class DownloadSource(
    val mediaType: DownloadMediaType,
    val id: String,
    val title: String,
    val channel: String,
    val thumbnail: String,
    val durationSec: Long? = null,
    val channelAvatarUrl: String? = null
)

internal class DownloadsRepository(private val context: Context, private val dao: DownloadDao) {
    val downloads = dao.observeAll()
    private val changes = Mutex()
    private val workManager get() = WorkManager.getInstance(context)

    suspend fun enqueue(
        source: DownloadSource,
        quality: DownloadQuality? = null,
        startSec: Long? = null,
        endSec: Long? = null
    ): DownloadStatus = withContext(NonCancellable + Dispatchers.IO) {
        changes.withLock {
            require((startSec == null && endSec == null) ||
                (startSec != null && endSec != null && startSec >= 0 && endSec > startSec))
            val id = UUID.randomUUID().toString()
            val request = request(id)
            val entry = dao.insertOrGet(
                DownloadEntity(
                    id = id, mediaType = source.mediaType, sourceId = source.id,
                    title = source.title, channelName = source.channel, thumbnailUrl = source.thumbnail,
                    qualityLabel = quality?.label?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.download_quality_original),
                    qualityKey = quality?.key,
                    startSec = startSec, endSec = endSec, workId = request.id.toString(),
                    durationSec = source.durationSec,
                    channelAvatarUrl = source.channelAvatarUrl
                )
            )
            if (entry.id == id) submit(request, id)
            entry.status
        }
    }

    suspend fun retry(id: String) = withContext(NonCancellable + Dispatchers.IO) {
        changes.withLock {
            val request = request(id)
            if (dao.retry(id, request.id.toString()) != 0) submit(request, id)
        }
    }

    suspend fun cancel(id: String) = withContext(NonCancellable + Dispatchers.IO) {
        changes.withLock {
            val entry = dao.get(id) ?: return@withLock
            if (dao.updateActive(id, entry.workId, DownloadStatus.CANCELLED, entry.progress) != 0) {
                workManager.cancelWorkById(UUID.fromString(entry.workId)).result.get()
            }
        }
    }

    suspend fun reconcile() = withContext(Dispatchers.IO) {
        changes.withLock {
            for (entry in dao.getActive()) {
                val work = workManager.getWorkInfoById(UUID.fromString(entry.workId)).get()
                if (work == null || work.state.isFinished) {
                    val cancelled = work?.state == WorkInfo.State.CANCELLED
                    dao.updateActive(entry.id, entry.workId,
                        if (cancelled) DownloadStatus.CANCELLED else DownloadStatus.FAILED,
                        entry.progress,
                        if (cancelled) null else context.getString(R.string.download_error))
                    downloadDirectory(context, entry).deleteRecursively()
                }
            }
        }
    }

    suspend fun delete(id: String) = withContext(NonCancellable + Dispatchers.IO) {
        changes.withLock {
            val entry = dao.get(id) ?: return@withLock
            if (entry.status == DownloadStatus.QUEUED || entry.status == DownloadStatus.DOWNLOADING) return@withLock
            val directory = downloadDirectory(context, entry).parentFile ?: return@withLock
            if (directory.exists() && !directory.deleteRecursively()) throw IOException()
            File(context.cacheDir, "download-shares/$id.zip").delete()
            dao.deleteInactive(id)
        }
    }

    suspend fun share(entry: DownloadEntity): Intent = withContext(Dispatchers.IO) {
        val file = entry.existingFile() ?: throw IOException()
        val hls = file.extension == "m3u8"
        val shareFile = if (hls) archive(entry, file) else file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.downloads", shareFile)
        Intent(Intent.ACTION_SEND).apply {
            type = if (hls) "application/zip" else "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(entry.title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private suspend fun archive(entry: DownloadEntity, file: File): File {
        val directory = File(context.cacheDir, "download-shares").apply { mkdirs() }
        val destination = File(directory, "${entry.id}.zip")
        val temporary = File.createTempFile("archive-", ".part", directory)
        try {
            ZipOutputStream(temporary.outputStream().buffered()).use { zip ->
                zip.setLevel(Deflater.NO_COMPRESSION)
                for (asset in file.parentFile?.listFiles().orEmpty().sortedBy { it.name }) {
                    currentCoroutineContext().ensureActive()
                    zip.putNextEntry(ZipEntry(asset.name))
                    asset.inputStream().buffered().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val size = input.read(buffer)
                            if (size == -1) break
                            zip.write(buffer, 0, size)
                        }
                    }
                    zip.closeEntry()
                }
            }
            if (!temporary.renameTo(destination)) throw IOException()
            return destination
        } finally {
            temporary.delete()
        }
    }

    private fun request(id: String) = OneTimeWorkRequestBuilder<DownloadWorker>()
        .setInputData(workDataOf(DownloadWorker.DOWNLOAD_ID to id))
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()

    private suspend fun submit(request: OneTimeWorkRequest, id: String) {
        try {
            workManager.enqueue(request).result.get()
        } catch (e: Exception) {
            dao.updateActive(id, request.id.toString(), DownloadStatus.FAILED, 0,
                context.getString(R.string.download_error))
            throw e
        }
    }
}

internal fun DownloadEntity.existingFile(): File? =
    filePath?.let(::File)?.takeIf { status == DownloadStatus.COMPLETED && it.isFile && it.length() > 0 }

internal fun downloadDirectory(context: Context, entry: DownloadEntity): File =
    File(context.filesDir, "downloads/${UUID.fromString(entry.id)}/${UUID.fromString(entry.workId)}")
