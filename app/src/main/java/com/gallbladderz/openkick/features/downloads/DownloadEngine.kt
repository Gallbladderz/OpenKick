/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.downloads

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.UriUtil
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceInputStream
import androidx.media3.datasource.DataSpec
import androidx.media3.exoplayer.hls.playlist.HlsMediaPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsMultivariantPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylist
import androidx.media3.exoplayer.hls.playlist.HlsPlaylistParser
import com.gallbladderz.openkick.core.data.local.DownloadEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.ceil
import kotlin.math.roundToInt

internal data class DownloadQuality(val key: String, val label: String, val height: Int, val fps: Int)

@UnstableApi
internal class DownloadEngine(private val factory: DataSource.Factory) {
    suspend fun qualities(url: String): List<DownloadQuality> = withContext(Dispatchers.IO) {
        val parsed = if (url.isHls()) playlist(Uri.parse(url)) else null
        if (parsed is HlsMultivariantPlaylist) parsed.orderedVariants().map { it.format.quality() }
        else emptyList()
    }

    suspend fun download(
        url: String,
        entry: DownloadEntity,
        directory: File,
        progress: suspend (Int) -> Unit
    ): File {
        if (!url.isHls()) {
            check(entry.startSec == null && entry.endSec == null) { "range" }
            return File(directory, "video.mp4").also { file ->
                copy(Uri.parse(url), file, onProgress = progress)
            }
        }
        val top = playlist(Uri.parse(url))
        val master = top as? HlsMultivariantPlaylist
        val variants = master?.orderedVariants().orEmpty()
        val variant = if (entry.qualityKey == null) variants.firstOrNull()
        else variants.firstOrNull { it.format.quality().key == entry.qualityKey }
            ?: throw IOException("quality")
        val video = when {
            top is HlsMediaPlaylist -> top
            variant != null -> playlist(variant.url, master) as? HlsMediaPlaylist
            else -> null
        } ?: throw IOException("playlist")
        val audioRendition = master?.audios?.filter { it.groupId == variant?.audioGroupId }
            ?.let { renditions ->
                renditions.firstOrNull { it.format.selectionFlags and C.SELECTION_FLAG_DEFAULT != 0 }
                    ?: renditions.firstOrNull()
            }
        val audio = audioRendition?.url?.let { playlist(it, master) as? HlsMediaPlaylist
            ?: throw IOException("playlist") }
        val videoIndices = video.selected(entry)
        val audioIndices = audio?.let {
            downloadSegmentIndices(it.segments.map { segment -> segment.durationUs }, entry.startSec, entry.endSec)
        }.orEmpty()
        var finished = 0L
        val count = videoIndices.size.toLong() + audioIndices.size
        val advance: suspend () -> Unit = { progress((++finished * 100 / count).toInt().coerceAtMost(99)) }
        writePlaylist(video, videoIndices, directory, "video", advance)
        if (audio == null || audioIndices.isEmpty()) return File(directory, "video.m3u8")
        writePlaylist(audio, audioIndices, directory, "audio", advance)
        val bandwidth = (variant?.format?.peakBitrate ?: 0).coerceAtLeast(1)
        return File(directory, "index.m3u8").apply {
            writeText(
                "#EXTM3U\n#EXT-X-VERSION:7\n" +
                    "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",NAME=\"Audio\",DEFAULT=YES,AUTOSELECT=YES,URI=\"audio.m3u8\"\n" +
                    "#EXT-X-STREAM-INF:BANDWIDTH=$bandwidth,AUDIO=\"audio\"\nvideo.m3u8\n"
            )
        }
    }

    private fun HlsMediaPlaylist.selected(entry: DownloadEntity): List<Int> {
        if (entry.endSec != null && entry.endSec > ceil(durationUs / 1_000_000.0).toLong()) {
            throw IOException("range")
        }
        return downloadSegmentIndices(segments.map { it.durationUs }, entry.startSec, entry.endSec)
            .takeIf { it.isNotEmpty() } ?: throw IOException("range")
    }

    private suspend fun writePlaylist(
        playlist: HlsMediaPlaylist, indices: List<Int>, directory: File, prefix: String,
        advance: suspend () -> Unit
    ) {
        val text = StringBuilder("#EXTM3U\n#EXT-X-VERSION:7\n#EXT-X-PLAYLIST-TYPE:VOD\n")
        val target = ceil(indices.maxOf { playlist.segments[it].durationUs } / 1_000_000.0).toLong()
        text.append("#EXT-X-TARGETDURATION:$target\n")
        val first = playlist.segments[indices.first()]
        text.append("#EXT-X-DISCONTINUITY-SEQUENCE:${playlist.discontinuitySequence + first.relativeDiscontinuitySequence}\n")
        var discontinuity = first.relativeDiscontinuitySequence
        var currentInit: HlsMediaPlaylist.Segment? = null
        val initFiles = mutableMapOf<HlsMediaPlaylist.Segment, String>()
        val keys = mutableMapOf<Uri, ByteArray>()
        for (index in indices) {
            val segment = playlist.segments[index]
            if (segment.drmInitData != null || segment.hasGapTag) throw IOException("unsupported")
            if (segment.relativeDiscontinuitySequence != discontinuity) {
                text.append("#EXT-X-DISCONTINUITY\n")
                discontinuity = segment.relativeDiscontinuitySequence
            }
            val init = segment.initializationSegment
            if (init != null && init != currentInit) {
                val name = initFiles[init] ?: "$prefix-init-${initFiles.size}.mp4".also {
                    copySegment(playlist, init, File(directory, it), keys)
                    initFiles[init] = it
                }
                text.append("#EXT-X-MAP:URI=\"$name\"\n")
            }
            currentInit = init
            val extension = if (init != null) "m4s" else "ts"
            val name = "$prefix-$index.$extension"
            copySegment(playlist, segment, File(directory, name), keys)
            text.append("#EXTINF:${segment.durationUs / 1_000_000.0},\n$name\n")
            advance()
        }
        File(directory, "$prefix.m3u8").writeText(text.append("#EXT-X-ENDLIST\n").toString())
    }

    private suspend fun copySegment(
        playlist: HlsMediaPlaylist, segment: HlsMediaPlaylist.Segment, file: File,
        keys: MutableMap<Uri, ByteArray>
    ) {
        val cipher = segment.fullSegmentEncryptionKeyUri?.let { key ->
            val keyUri = UriUtil.resolveToUri(playlist.baseUri, key)
            val bytes = keys[keyUri] ?: open(keyUri).use { stream ->
                val keyBytes = ByteArray(16)
                var read = 0
                while (read < keyBytes.size) {
                    val count = stream.read(keyBytes, read, keyBytes.size - read)
                    if (count < 0) throw IOException("unsupported")
                    read += count
                }
                if (stream.read() != -1) throw IOException("unsupported")
                keyBytes
            }.also { keys[keyUri] = it }
            val iv = segment.encryptionIV ?: throw IOException("unsupported")
            val encoded = BigInteger(iv.removePrefix("0x").removePrefix("0X"), 16).toByteArray()
            val normalized = ByteArray(16)
            val length = minOf(encoded.size, normalized.size)
            encoded.copyInto(normalized, 16 - length, encoded.size - length)
            Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(bytes, "AES"), IvParameterSpec(normalized))
            }
        }
        copy(UriUtil.resolveToUri(playlist.baseUri, segment.url), file,
            segment.byteRangeOffset, segment.byteRangeLength, cipher)
    }

    private fun playlist(uri: Uri, master: HlsMultivariantPlaylist? = null): HlsPlaylist {
        val source = factory.createDataSource()
        return DataSourceInputStream(source, DataSpec(uri)).use { input ->
            input.open()
            val parser = if (master == null) HlsPlaylistParser() else HlsPlaylistParser(master, null)
            parser.parse(source.uri ?: uri, input)
        }
    }

    private fun open(uri: Uri): InputStream =
        DataSourceInputStream(factory.createDataSource(), DataSpec(uri))

    private suspend fun copy(
        uri: Uri, file: File, position: Long = 0, length: Long = C.LENGTH_UNSET.toLong(),
        cipher: Cipher? = null, onProgress: suspend (Int) -> Unit = {}
    ) {
        val context = currentCoroutineContext()
        context.ensureActive()
        val source = factory.createDataSource()
        val spec = DataSpec.Builder().setUri(uri).setPosition(position).setLength(length).build()
        try {
            val total = source.open(spec)
            val input = object : InputStream() {
                override fun read(): Int {
                    val single = ByteArray(1)
                    return if (read(single, 0, 1) == -1) -1 else single[0].toInt() and 0xff
                }
                override fun read(bytes: ByteArray, offset: Int, count: Int): Int {
                    context.ensureActive()
                    return source.read(bytes, offset, count)
                }
            }
            val payload = if (cipher == null) input else CipherInputStream(input, cipher)
            file.outputStream().buffered().use { output ->
                val buffer = ByteArray(64 * 1024)
                var copied = 0L
                var last = 0
                while (true) {
                    context.ensureActive()
                    val count = payload.read(buffer)
                    if (count == -1) break
                    output.write(buffer, 0, count)
                    copied += count
                    if (total > 0 && cipher == null) {
                        val percent = (copied.toDouble() * 100 / total).toInt().coerceIn(0, 99)
                        if (percent > last) {
                            last = percent
                            onProgress(percent)
                        }
                    }
                }
                if (copied == 0L) throw IOException("empty")
            }
        } finally {
            source.close()
        }
    }
}

private fun String.isHls() = Uri.parse(this).path.orEmpty().endsWith(".m3u8", ignoreCase = true)

@UnstableApi
private fun HlsMultivariantPlaylist.orderedVariants() = variants
    .filter { it.format.height > 0 }.ifEmpty { variants }
    .sortedWith(compareByDescending<HlsMultivariantPlaylist.Variant> { it.format.height }
        .thenByDescending { it.format.frameRate }.thenByDescending { it.format.peakBitrate })
    .distinctBy { it.format.quality().key }

@UnstableApi
private fun Format.quality(): DownloadQuality {
    val fps = if (frameRate.isFinite() && frameRate > 0) frameRate.roundToInt() else 0
    val label = if (height > 0) "${height}p" + fps.takeIf { it > 0 }?.toString().orEmpty() else ""
    return DownloadQuality("$height:$fps:$averageBitrate:$peakBitrate", label, height, fps)
}
