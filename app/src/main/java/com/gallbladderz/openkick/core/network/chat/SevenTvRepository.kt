/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.chat

import com.gallbladderz.openkick.core.domain.chat.SevenTvEmote
import com.gallbladderz.openkick.core.network.api.SevenTvApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit

internal class SevenTvRepository(private val api: SevenTvApi = createSevenTvApi()) {
    private val globalLock = Mutex()
    private val channelLock = Mutex()
    private var globalCache: List<SevenTvEmote>? = null
    private val channelCache = LinkedHashMap<Long, List<SevenTvEmote>>(MAX_CHANNELS, 0.75f, true)

    suspend fun globalEmotes(): List<SevenTvEmote> = globalLock.withLock {
        globalCache ?: fetch { api.global().emotes { it } }
            ?.also { globalCache = it }.orEmpty()
    }

    suspend fun channelEmotes(kickUserId: Long): List<SevenTvEmote> = channelLock.withLock {
        channelCache[kickUserId] ?: fetch { api.channel(kickUserId).emotes { it.emoteSet } }
            ?.also {
                channelCache[kickUserId] = it
                if (channelCache.size > MAX_CHANNELS) channelCache.remove(channelCache.keys.first())
            }.orEmpty()
    }

    private suspend fun fetch(request: suspend () -> List<SevenTvEmote>?): List<SevenTvEmote>? = try {
        request()
    } catch (_: IOException) {
        currentCoroutineContext().ensureActive()
        null
    } catch (_: SerializationException) {
        null
    }

    private fun <T> Response<T>.emotes(set: (T) -> SevenTvEmoteSetDto?): List<SevenTvEmote>? =
        when {
            code() == 404 -> emptyList()
            isSuccessful -> body()?.let(set)?.toEmotes().orEmpty()
            else -> null
        }.also { errorBody()?.close() }
}

private const val MAX_CHANNELS = 32
private val sevenTvJson = Json { ignoreUnknownKeys = true }

private fun createSevenTvApi(): SevenTvApi = Retrofit.Builder()
    .baseUrl("https://7tv.io/v3/")
    .client(OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build())
    .addConverterFactory(sevenTvJson.asConverterFactory("application/json".toMediaType()))
    .build()
    .create(SevenTvApi::class.java)

@Serializable
internal data class SevenTvConnectionDto(
    @SerialName("emote_set") val emoteSet: SevenTvEmoteSetDto? = null
)

@Serializable
internal data class SevenTvEmoteSetDto(private val emotes: List<SevenTvActiveEmoteDto> = emptyList()) {
    fun toEmotes(): List<SevenTvEmote> = emotes.mapNotNull { it.toEmote() }
}

@Serializable
internal data class SevenTvActiveEmoteDto(
    private val id: String = "",
    private val name: String = "",
    private val flags: Int = 0,
    private val data: SevenTvEmoteDataDto? = null
) {
    fun toEmote(): SevenTvEmote? {
        if (id.isBlank() || name.isBlank()) return null
        val details = data ?: return null
        val host = details.host ?: return null
        val base = host.url.trimEnd('/').let { if (it.startsWith("//")) "https:$it" else it }
        if (!base.startsWith("https://") && !base.startsWith("http://")) return null
        val files = host.files.filter { it.name.isNotBlank() }
        val file = listOf("2x.webp", "1x.webp", "2x.png")
            .firstNotNullOfOrNull { name -> files.find { it.name == name } }
            ?: files.firstOrNull() ?: return null
        return SevenTvEmote(
            id = id,
            name = name,
            imageUrl = "$base/${file.name}",
            width = file.width,
            height = file.height,
            animated = details.animated,
            isZeroWidth = flags and 1 != 0 || details.flags and 256 != 0
        )
    }
}

@Serializable
internal data class SevenTvEmoteDataDto(
    val animated: Boolean = false,
    val flags: Int = 0,
    val host: SevenTvHostDto? = null
)

@Serializable
internal data class SevenTvHostDto(val url: String = "", val files: List<SevenTvImageDto> = emptyList())

@Serializable
internal data class SevenTvImageDto(val name: String = "", val width: Int = 0, val height: Int = 0)
