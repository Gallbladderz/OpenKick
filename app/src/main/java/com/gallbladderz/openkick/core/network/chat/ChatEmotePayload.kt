/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.core.network.chat

import com.gallbladderz.openkick.core.domain.chat.ChatEmote
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteSet
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object ChatEmotePayload {
    private val emoteKeys = setOf("emotes", "items", "data", "list")
    private val ignoredKeys = setOf("id", "name", "title", "label", "slug", "type", "code")

    fun parse(root: JsonElement): List<ChatEmoteSet> {
        val groups = linkedMapOf<String, LinkedHashMap<String, ChatEmote>>()
        visit(root, fallbackSet = "Kick", groups = groups)
        return groups
            .mapNotNull { (name, emotes) ->
                emotes.values.toList().takeIf { it.isNotEmpty() }?.let { ChatEmoteSet(name, it) }
            }
    }

    private fun visit(
        element: JsonElement,
        fallbackSet: String,
        groups: LinkedHashMap<String, LinkedHashMap<String, ChatEmote>>
    ) {
        when (element) {
            is JsonArray -> element.forEach { visit(it, fallbackSet, groups) }
            is JsonObject -> {
                val direct = asEmote(element)
                if (direct != null) {
                    val emoteSet = string(element, "set_name")
                        ?: string(element, "set")
                        ?: string(element, "category")
                        ?: string(element, "type")?.let(::prettify)
                        ?: fallbackSet
                    groups.getOrPut(emoteSet) { linkedMapOf() }[direct.id] = direct
                    return
                }

                val objectSetName = string(element, "name")
                    ?: string(element, "title")
                    ?: string(element, "label")

                element.forEach { (key, value) ->
                    if (key in ignoredKeys) return@forEach
                    val childSet = when {
                        key in emoteKeys && objectSetName != null -> objectSetName
                        key in emoteKeys -> prettify(key)
                        value is JsonArray || value is JsonObject -> prettify(key)
                        else -> fallbackSet
                    }
                    visit(value, childSet, groups)
                }
            }

            else -> Unit
        }
    }

    private fun asEmote(objectValue: JsonObject): ChatEmote? {
        if (objectValue.keys.any { it in emoteKeys }) return null
        val id = string(objectValue, "id") ?: string(objectValue, "emote_id") ?: return null
        val name = string(objectValue, "name")
            ?: string(objectValue, "code")
            ?: string(objectValue, "emote")
            ?: return null
        val imageUrl = string(objectValue, "image_url")
            ?: string(objectValue, "image")
            ?: string(objectValue, "url")
            ?: "https://files.kick.com/emotes/$id/fullsize"
        return ChatEmote(id = id, name = name, imageUrl = imageUrl)
    }

    private fun string(objectValue: JsonObject, key: String): String? =
        (objectValue[key] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() && it != "null" }

    private fun prettify(key: String): String = key
        .replace('_', ' ')
        .replace('-', ' ')
        .split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { it.replaceFirstChar { first -> first.uppercaseChar() } }
}
