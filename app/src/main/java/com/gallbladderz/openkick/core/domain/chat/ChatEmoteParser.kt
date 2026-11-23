/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.domain.chat


object ChatEmoteParser {
    internal val tokenPattern = Regex("\\[emote:(\\d+):([^\\]]+)\\]")

    fun parseEmotes(content: String): List<ChatToken> {
        val emotesMatches = tokenPattern.findAll(content)
        val tokens = mutableListOf<ChatToken>()
        var currentIndex = 0

        for (match in emotesMatches) {
            val emoteId = match.groupValues[1]
            val emoteName = match.groupValues[2]
            val matchStart = match.range.first
            val matchEnd = match.range.last + 1

            if (matchStart > currentIndex) {
                tokens.add(
                    ChatToken.Text(
                        content.substring(
                            currentIndex,
                            matchStart
                        )
                    )
                )
            }
            tokens.add(ChatToken.Emote(emoteId, emoteName))
            currentIndex = matchEnd
        }

        if (currentIndex < content.length) {
            tokens.add(ChatToken.Text(content.substring(currentIndex)))
        }

        return tokens
    }
}
