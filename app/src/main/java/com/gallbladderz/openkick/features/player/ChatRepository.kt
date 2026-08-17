/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player

import com.gallbladderz.openkick.core.network.KickApiConstants
import com.gallbladderz.openkick.features.player.data.dto.ChatMessageDataDto
import com.gallbladderz.openkick.features.player.data.dto.PusherEventDto
import com.gallbladderz.openkick.features.player.data.dto.toDomainModel
import com.gallbladderz.openkick.features.player.models.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlin.math.min
import kotlin.math.pow

class ChatRepository(
    private val okHttpClient: OkHttpClient,
    private val coroutineScope: CoroutineScope
) {
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private var webSocket: WebSocket? = null

    private var reconnectAttempt = 0
    private var isManuallyDisconnected = false
    private var currentChatroomId: String? = null

    private val json = Json { ignoreUnknownKeys = true }

    fun connectToChat(chatroomId: String) {
        currentChatroomId = chatroomId
        isManuallyDisconnected = false
        webSocket?.cancel()

        val request = Request.Builder()
            .url(KickApiConstants.PUSHER_WS_URL)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempt = 0
                val subscribeMsg =
                    """{"event":"pusher:subscribe","data":{"auth":"","channel":"chatrooms.$chatroomId.v2"}}"""
                webSocket.send(subscribeMsg)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val eventDto = json.decodeFromString<PusherEventDto>(text)

                    if (eventDto.event == "App\\Events\\ChatMessageEvent") {
                        val dataString = eventDto.data ?: return
                        val messageDto = json.decodeFromString<ChatMessageDataDto>(dataString)
                        val newMessage = messageDto.toDomainModel()
                        _chatMessages.update { (listOf(newMessage) + it).take(100) }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ChatRepository", "WebSocket parsing error", e)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                android.util.Log.d("ChatRepository", "WebSocket closed: $code, $reason")
                if (!isManuallyDisconnected) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                android.util.Log.e("ChatRepository", "WebSocket failure", t)
                if (!isManuallyDisconnected) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun scheduleReconnect() {
        coroutineScope.launch {
            val delayMs = min(1000L * (2.0.pow(reconnectAttempt)).toLong(), 30000L)
            delay(delayMs)
            reconnectAttempt++
            currentChatroomId?.let { connectToChat(it) }
        }
    }

    fun disconnect() {
        isManuallyDisconnected = true
        coroutineScope.coroutineContext.cancelChildren()
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        _chatMessages.update { emptyList() }
    }
}
