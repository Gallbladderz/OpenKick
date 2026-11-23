/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.domain.chat

data class SevenTvEmote(
    val id: String,
    val name: String,
    val imageUrl: String,
    val width: Int,
    val height: Int,
    val animated: Boolean,
    val isZeroWidth: Boolean
) {
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) (width.toFloat() / height).coerceIn(0.5f, 4f) else 1f
}

internal data class ChatDisplayEmote(
    val id: String,
    val name: String,
    val sevenTv: SevenTvEmote? = null
) {
    val key: String get() = "${if (sevenTv == null) "kick" else "7tv"}:$id:$name"
}

internal fun SevenTvEmote.toDisplayEmote() = ChatDisplayEmote(id, name, this)

internal sealed interface ChatDisplayToken {
    data class Text(val text: String) : ChatDisplayToken
    data class Emote(
        val base: ChatDisplayEmote,
        val overlays: List<ChatDisplayEmote> = emptyList()
    ) : ChatDisplayToken
}

internal fun sevenTvEmoteIndex(
    channel: List<SevenTvEmote>,
    global: List<SevenTvEmote>
): Map<String, SevenTvEmote> = buildMap {
    global.forEach { put(it.name, it) }
    channel.forEach { put(it.name, it) }
}

private val chatWords = Regex("[^\\p{javaWhitespace}\\p{Z}]+")

internal fun tokenizeChatDisplay(
    tokens: List<ChatToken>,
    sevenTv: Map<String, SevenTvEmote>
): List<ChatDisplayToken> {
    val result = mutableListOf<ChatDisplayToken>()
    val text = StringBuilder()

    fun flushText() {
        if (text.isEmpty()) return
        result += ChatDisplayToken.Text(text.toString())
        text.clear()
    }

    fun addEmote(emote: ChatDisplayEmote) {
        val previous = result.lastOrNull()
        if (emote.sevenTv?.isZeroWidth == true &&
            previous is ChatDisplayToken.Emote && text.all { it == ' ' || it == '\t' }
        ) {
            result[result.lastIndex] = previous.copy(overlays = previous.overlays + emote)
            text.clear()
        } else {
            flushText()
            result += ChatDisplayToken.Emote(emote)
        }
    }

    for (token in tokens) {
        when (token) {
            is ChatToken.Emote -> addEmote(ChatDisplayEmote(token.emoteId, token.emoteName))
            is ChatToken.Text -> {
                var position = 0
                if (sevenTv.isNotEmpty()) {
                    for (word in chatWords.findAll(token.text)) {
                        if (word.value.startsWith('@') || "://" in word.value ||
                            word.value.startsWith("www.", ignoreCase = true)
                        ) continue
                        val emote = sevenTv[word.value] ?: continue
                        text.append(token.text, position, word.range.first)
                        addEmote(emote.toDisplayEmote())
                        position = word.range.last + 1
                    }
                }
                text.append(token.text, position, token.text.length)
            }
        }
    }
    flushText()
    return result
}
