/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.core.network.chat

import kotlinx.serialization.json.Json

internal object CentrifugoFrameParser {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    fun parse(rawText: String): CentrifugoFrameDto? {
        val jsonStart = rawText.indexOfFirst { it == '{' }
        if (jsonStart == -1) return null

        val jsonSlice = extractJsonObject(rawText, jsonStart) ?: return null
        return runCatching { json.decodeFromString<CentrifugoFrameDto>(jsonSlice) }.getOrNull()
    }

    fun extractJsonObject(source: String, startIndex: Int): String? {
        if (startIndex < 0 || startIndex >= source.length || source[startIndex] != '{') return null

        var depth = 0
        var inQuotes = false
        var escaped = false

        for (i in startIndex until source.length) {
            val c = source[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (c == '\\') {
                escaped = true
                continue
            }
            if (c == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (inQuotes) continue

            when (c) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(startIndex, i + 1)
                }
            }
        }
        return null
    }
}
