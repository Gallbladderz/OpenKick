/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gallbladderz.openkick.core.domain.chat.SevenTvEmote
import com.gallbladderz.openkick.core.domain.chat.sevenTvEmoteIndex
import com.gallbladderz.openkick.core.network.chat.SevenTvRepository
import org.koin.compose.koinInject

internal class SevenTvCatalog(
    val channel: List<SevenTvEmote>,
    val global: List<SevenTvEmote>
) {
    val byName = sevenTvEmoteIndex(channel, global)
}

internal val LocalSevenTvCatalog = compositionLocalOf<SevenTvCatalog?> { null }

@Composable
internal fun SevenTvChat(enabled: Boolean, kickUserId: Long?, content: @Composable () -> Unit) {
    val catalog = if (enabled) rememberSevenTvCatalog(kickUserId) else null
    CompositionLocalProvider(LocalSevenTvCatalog provides catalog, content = content)
}

@Composable
private fun rememberSevenTvCatalog(kickUserId: Long?): SevenTvCatalog {
    val repository = koinInject<SevenTvRepository>()
    var global by remember(repository) { mutableStateOf(emptyList<SevenTvEmote>()) }
    var channel by remember(repository, kickUserId) { mutableStateOf(emptyList<SevenTvEmote>()) }
    LaunchedEffect(repository) {
        global = repository.globalEmotes()
    }
    LaunchedEffect(repository, kickUserId) {
        if (kickUserId != null) channel = repository.channelEmotes(kickUserId)
    }
    return remember(channel, global) { SevenTvCatalog(channel, global) }
}
