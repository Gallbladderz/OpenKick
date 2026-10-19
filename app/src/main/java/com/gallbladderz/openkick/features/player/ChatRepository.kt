/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.player


import android.content.Context
import android.util.Log
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.auth.SessionManager
import com.gallbladderz.openkick.core.data.mapper.toDomainModel
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteParser
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteSet
import com.gallbladderz.openkick.core.domain.chat.ChatMessage
import com.gallbladderz.openkick.core.domain.chat.ChatMode
import com.gallbladderz.openkick.core.domain.chat.ChatModeType
import com.gallbladderz.openkick.core.domain.chat.ChatToken
import com.gallbladderz.openkick.core.domain.chat.ReplyToInfo
import com.gallbladderz.openkick.core.network.KickApiConstants
import com.gallbladderz.openkick.core.network.api.ChatEmotesApi
import com.gallbladderz.openkick.core.network.api.ChatSendApi
import com.gallbladderz.openkick.core.network.api.RealtimeApi
import com.gallbladderz.openkick.core.network.chat.CentrifugoErrorDto
import com.gallbladderz.openkick.core.network.chat.CentrifugoErrors
import com.gallbladderz.openkick.core.network.chat.CentrifugoFrameDto
import com.gallbladderz.openkick.core.network.chat.CentrifugoFrameParser
import com.gallbladderz.openkick.core.network.chat.ChatEmotePayload
import com.gallbladderz.openkick.core.network.chat.ChatMessageDataDto
import com.gallbladderz.openkick.core.network.chat.SendChatMessageRequest
import com.gallbladderz.openkick.core.network.chat.SendChatMetadata
import com.gallbladderz.openkick.core.network.chat.SendChatOriginalMessage
import com.gallbladderz.openkick.core.network.chat.SendChatOriginalSender
import com.gallbladderz.openkick.core.network.dto.realtime.RealtimeConnectionRequest
import com.gallbladderz.openkick.core.ui.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.Buffer
import okio.ByteString
import retrofit2.HttpException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

sealed interface ChatSendState {
    data object Idle : ChatSendState
    data object Sending : ChatSendState
    data class Error(val message: UiText) : ChatSendState
}

class ChatRepository(
    private val context: Context,
    okHttpClient: OkHttpClient,
    private val realtimeApi: RealtimeApi,
    private val chatSendApi: ChatSendApi,
    private val chatEmotesApi: ChatEmotesApi,
    private val sessionManager: SessionManager,
    private val coroutineScope: CoroutineScope
) {
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private val _chatSendState = MutableStateFlow<ChatSendState>(ChatSendState.Idle)
    val chatSendState = _chatSendState.asStateFlow()

    private val _chatEmoteSets = MutableStateFlow<List<ChatEmoteSet>>(emptyList())
    val chatEmoteSets = _chatEmoteSets.asStateFlow()

    private val internalDeque = java.util.ArrayDeque<ChatMessage>()
    private val lock = Any()

    @Volatile
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var emoteLoadJob: Job? = null

    @Volatile
    private var channelSubscriberBadges: Map<Int, String> = emptyMap()

    @Volatile
    private var chatMode: ChatMode = ChatMode()

    @Volatile
    private var reconnectAttempt = 0
    private val maxReconnectAttempts = 10

    @Volatile
    private var isManuallyDisconnected = false

    @Volatile
    private var currentChatroomId: String? = null

    @Volatile
    private var clientId: String? = null

    @Volatile
    private var centrifugoToken: String? = null

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val wsClient: OkHttpClient = okHttpClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "ChatRepository"

        private const val MAX_RECONNECT_DELAY_MS = 30_000L
    }

    fun reportSystemMessage(text: UiText) {
        val renderedText = text.asString(context)
        Log.d(TAG, "System: $renderedText")
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = context.getString(R.string.chat_system_sender),
            senderColor = "",
            content = renderedText,
            tokens = listOf(ChatToken.Text(renderedText))
        )
        synchronized(lock) {
            internalDeque.addFirst(msg)
            if (internalDeque.size > 100) internalDeque.removeLast()
            _chatMessages.value = internalDeque.toList()
        }
    }

    fun sendMessage(content: String, replyTo: ChatMessage? = null) {
        when (chatMode.type) {
            ChatModeType.DISABLED -> {
                _chatSendState.value =
                    ChatSendState.Error(UiText.StringResource(R.string.chat_disabled_hint))
                return
            }

            ChatModeType.SUBSCRIBERS -> if (chatMode.isSubscriber != true) {
                _chatSendState.value =
                    ChatSendState.Error(UiText.StringResource(R.string.chat_subscriber_required))
                return
            }

            else -> Unit
        }
        val chatroomId = currentChatroomId
        if (chatroomId.isNullOrBlank()) {
            Log.w(TAG, "Cannot send message: no active chatroom")
            _chatSendState.value =
                ChatSendState.Error(UiText.StringResource(R.string.chat_not_connected))
            return
        }

        val pendingId = "pending_${UUID.randomUUID()}"
        val username = sessionManager.cachedAccount()?.username
            ?: context.getString(R.string.chat_you)

        val pendingMessage = ChatMessage(
            id = pendingId,
            sender = username,
            senderColor = "#FFFFFF",
            content = content,
            tokens = ChatEmoteParser.parseEmotes(content),
            replyTo = replyTo?.let {
                ReplyToInfo(
                    messageId = it.id,
                    senderName = it.sender,
                    content = it.content
                )
            },
            isPending = true
        )

        synchronized(lock) {
            internalDeque.addFirst(pendingMessage)
            if (internalDeque.size > 100) internalDeque.removeLast()
            _chatMessages.value = internalDeque.toList()
        }

        _chatSendState.value = ChatSendState.Sending

        coroutineScope.launch {
            try {
                val request = SendChatMessageRequest(
                    content = content,
                    type = if (replyTo == null) "message" else "reply",
                    messageRef = System.currentTimeMillis().toString(),
                    metadata = if (replyTo != null) {
                        SendChatMetadata(
                            originalSender = SendChatOriginalSender(
                                username = replyTo.sender
                            ),
                            originalMessage = SendChatOriginalMessage(
                                id = replyTo.id,
                                content = replyTo.content
                            )
                        )
                    } else null
                )

                chatSendApi.sendChatMessage(chatroomId, request)?.close()

                synchronized(lock) {
                    val updated = internalDeque.filter { it.id != pendingId }
                    internalDeque.clear()
                    internalDeque.addAll(updated)
                    _chatMessages.value = internalDeque.toList()
                }

                _chatSendState.value = ChatSendState.Idle
                Log.d(TAG, "Message sent successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)

                synchronized(lock) {
                    val updated = internalDeque.filter { it.id != pendingId }
                    internalDeque.clear()
                    internalDeque.addAll(updated)
                    _chatMessages.value = internalDeque.toList()
                }

                val message = if (e is HttpException) {
                    parseChatError(e.response()?.errorBody()?.use { it.string() })
                        ?: "HTTP ${e.code()}"
                } else {
                    e.message
                }
                _chatSendState.value = ChatSendState.Error(
                    message?.let { UiText.DynamicString(it) }
                        ?: UiText.StringResource(R.string.chat_send_failed)
                )
            }
        }
    }

    private fun parseChatError(error: String?): String? = runCatching {
        val body = json.parseToJsonElement(error.orEmpty()) as? JsonObject
        val validationErrors = (body?.get("errors") as? JsonObject)
            ?.values
            ?.flatMap { fieldErrors ->
                (fieldErrors as? JsonArray)?.mapNotNull {
                    (it as? JsonPrimitive)?.content
                }.orEmpty()
            }
            ?.joinToString("; ")
            ?.takeIf { it.isNotBlank() }
        validationErrors ?: (body?.get("message") as? JsonPrimitive)?.content
    }.getOrNull()

    fun loadEmotes(channelSlug: String) {
        emoteLoadJob?.cancel()
        if (sessionManager.tokens() == null || channelSlug.isBlank()) {
            _chatEmoteSets.value = emptyList()
            return
        }

        val requestedChannel = channelSlug.trim()
        emoteLoadJob = coroutineScope.launch {
            _chatEmoteSets.value = emptyList()
            var firstError: Throwable? = null
            suspend fun tryLoad(request: suspend () -> kotlinx.serialization.json.JsonElement): Boolean {
                try {
                    val sets = ChatEmotePayload.parse(request())
                    if (sets.isNotEmpty()) {
                        _chatEmoteSets.value = sets
                        Log.d(
                            TAG,
                            "Loaded ${sets.sumOf { it.emotes.size }} emotes in ${sets.size} sets"
                        )
                        return true
                    }
                } catch (error: Throwable) {
                    if (firstError == null) firstError = error
                }
                return false
            }

            if (!tryLoad { chatEmotesApi.getChannelEmotes(requestedChannel) }) {
                tryLoad { chatEmotesApi.getChannelEmotesV2(requestedChannel) }
            }
            if (_chatEmoteSets.value.isEmpty()) {
                firstError?.let {
                    Log.w(
                        TAG,
                        "Could not load Kick emotes for $requestedChannel",
                        it
                    )
                }
            }
        }
    }

    fun clearEmotes() {
        emoteLoadJob?.cancel()
        _chatEmoteSets.value = emptyList()
    }

    fun setChannelSubscriberBadges(badges: Map<Int, String>) {
        channelSubscriberBadges = badges
    }

    fun setChatMode(mode: ChatMode) {
        chatMode = mode
    }

    fun connectToChat(chatroomId: String) {
        if (currentChatroomId == chatroomId && webSocket != null) return
        reconnectAttempt = 0
        openConnection(chatroomId)
    }

    private fun openConnection(chatroomId: String) {
        if (currentChatroomId != chatroomId) {
            synchronized(lock) {
                internalDeque.clear()
                _chatMessages.value = emptyList()
            }
        }

        currentChatroomId = chatroomId
        isManuallyDisconnected = false
        reconnectJob?.cancel()

        val oldSocket = webSocket
        webSocket = null
        oldSocket?.cancel()

        coroutineScope.launch {
            try {

                val connectionClientId =
                    clientId ?: UUID.randomUUID().toString().replace("-", "").also { clientId = it }

                Log.d(TAG, "Requesting Centrifugo token with client_id: $connectionClientId")
                val response = realtimeApi.getConnectionToken(
                    RealtimeConnectionRequest(clientId = connectionClientId)
                )
                centrifugoToken = response.data.token
                Log.d(TAG, "Received token: ${centrifugoToken?.take(20)}...")

                startWebSocketConnection(chatroomId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get token", e)
                reportSystemMessage(
                    UiText.StringResource(
                        R.string.chat_token_error,
                        e.message.orEmpty()
                    )
                )
                scheduleReconnect()
            }
        }
    }

    private fun startWebSocketConnection(chatroomId: String) {
        val traceId = UUID.randomUUID().toString().replace("-", "")
        val authToken = sessionManager.tokens()?.accessToken ?: "null"

        val request = Request.Builder()
            .url(KickApiConstants.CENTRIFUGO_WS_URL)
            .header("Cache-Control", "no-store")
            .header("x-kick-app", KickApiConstants.HEADER_APP)
            .header("User-Agent", KickApiConstants.USER_AGENT)
            .header("x-app-version", KickApiConstants.APP_VERSION)
            .header("x-app-platform", KickApiConstants.HEADER_PLATFORM)
            .header("x-req-trace", traceId)
            .header("Authorization", "Bearer $authToken")
            .header("Sec-WebSocket-Protocol", "centrifuge-protobuf")
            .build()

        Log.d(TAG, "Opening WebSocket for chatroom $chatroomId")

        webSocket = wsClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (webSocket !== this@ChatRepository.webSocket) return
                Log.d(TAG, "WebSocket opened")
                reconnectAttempt = 0

                webSocket.send(buildConnectFrame(centrifugoToken ?: ""))
                webSocket.send(buildSubscribeFrame("chatrooms.$chatroomId.v2"))
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (webSocket !== this@ChatRepository.webSocket) return

                if (bytes.size == 1 && bytes[0] == 0.toByte()) {
                    webSocket.send(ByteString.of(0))
                    Log.d(TAG, "Ping/pong")
                    return
                }

                handleBinaryFrame(bytes.utf8())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (webSocket !== this@ChatRepository.webSocket) return
                handleBinaryFrame(text)
            }

            private fun handleBinaryFrame(rawText: String) {
                try {
                    Log.d(TAG, "Received frame: ${rawText.take(200)}")

                    val frame = parseCentrifugoFrame(rawText) ?: return

                    frame.error?.let { error ->
                        Log.e(TAG, "Centrifugo error ${error.code}: ${error.message}")
                        reportSystemMessage(describeCentrifugoError(error.code, error.message))
                        handleCentrifugoError(error)
                        return
                    }

                    val eventDto = frame.resolvedEvent ?: return

                    when (eventDto.event) {
                        "App\\Events\\ChatMessageEvent" -> {
                            val dataElem = eventDto.data ?: return
                            val dataString = if (dataElem is JsonPrimitive) {
                                dataElem.content
                            } else {
                                dataElem.toString()
                            }
                            val messageDto = json.decodeFromString<ChatMessageDataDto>(dataString)
                            val newMessage = messageDto.toDomainModel(channelSubscriberBadges)
                            synchronized(lock) {
                                internalDeque.addFirst(newMessage)
                                if (internalDeque.size > 100) {
                                    internalDeque.removeLast()
                                }
                                _chatMessages.value = internalDeque.toList()
                            }
                            Log.d(TAG, "Chat message: ${newMessage.sender}: ${newMessage.content}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Parse error", e)
                    reportSystemMessage(
                        UiText.StringResource(
                            R.string.chat_parse_error,
                            e.message.orEmpty()
                        )
                    )
                }
            }

            private fun describeCentrifugoError(code: Int?, fallback: String?): UiText =
                when (code) {
                    CentrifugoErrors.INTERNAL_SERVER_ERROR -> UiText.StringResource(R.string.chat_server_error)
                    CentrifugoErrors.UNAUTHORIZED -> UiText.StringResource(R.string.chat_token_expired)
                    CentrifugoErrors.UNKNOWN_CHANNEL -> UiText.StringResource(R.string.chat_channel_not_found)
                    CentrifugoErrors.PERMISSION_DENIED -> UiText.StringResource(R.string.chat_permission_denied)
                    CentrifugoErrors.METHOD_NOT_FOUND -> UiText.StringResource(R.string.chat_method_not_supported)
                    CentrifugoErrors.ALREADY_SUBSCRIBED -> UiText.StringResource(R.string.chat_already_subscribed)
                    CentrifugoErrors.LIMIT_EXCEEDED -> UiText.StringResource(R.string.chat_connection_limit)
                    CentrifugoErrors.BAD_REQUEST -> UiText.StringResource(R.string.chat_bad_request)
                    CentrifugoErrors.NOT_AVAILABLE -> UiText.StringResource(R.string.chat_unavailable)
                    else -> UiText.DynamicString(fallback?.takeIf { it.isNotBlank() } ?: "")
                }

            private fun parseCentrifugoFrame(rawText: String): CentrifugoFrameDto? =
                CentrifugoFrameParser.parse(rawText)

            private fun handleCentrifugoError(error: CentrifugoErrorDto) {
                when (error.code) {
                    CentrifugoErrors.UNAUTHORIZED,
                    CentrifugoErrors.PERMISSION_DENIED -> {

                        isManuallyDisconnected = true
                        reconnectJob?.cancel()
                    }

                    CentrifugoErrors.BAD_REQUEST -> {

                        isManuallyDisconnected = true
                        reconnectJob?.cancel()
                    }

                    else -> scheduleReconnect()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (webSocket !== this@ChatRepository.webSocket) return
                Log.d(TAG, "WebSocket closed: $code - $reason")
                this@ChatRepository.webSocket = null
                if (!isManuallyDisconnected) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (webSocket !== this@ChatRepository.webSocket) return
                Log.e(TAG, "WebSocket failure: ${response?.code}", t)
                this@ChatRepository.webSocket = null

                if (response?.code == 401 || response?.code == 403) {
                    reportSystemMessage(
                        UiText.StringResource(
                            R.string.chat_authorization_error,
                            response.code
                        )
                    )
                    isManuallyDisconnected = true
                } else {
                    reportSystemMessage(
                        UiText.StringResource(
                            R.string.chat_network_error,
                            t.javaClass.simpleName,
                            t.message.orEmpty()
                        )
                    )
                    if (!isManuallyDisconnected) {
                        scheduleReconnect()
                    }
                }
            }
        })
    }

    private fun buildConnectFrame(token: String): ByteString {
        val tokenBytes = token.encodeToByteArray()

        val connectReqBuffer = Buffer()
        connectReqBuffer.writeByte(0x0A)
        writeVarint(connectReqBuffer, tokenBytes.size)
        connectReqBuffer.write(tokenBytes)
        val connectReqBytes = connectReqBuffer.readByteArray()

        val cmdBuffer = Buffer()
        cmdBuffer.writeByte(0x08)
        cmdBuffer.writeByte(0x01)
        cmdBuffer.writeByte(0x22)
        writeVarint(cmdBuffer, connectReqBytes.size)
        cmdBuffer.write(connectReqBytes)
        val cmdBytes = cmdBuffer.readByteArray()

        val frameBuffer = Buffer()
        writeVarint(frameBuffer, cmdBytes.size)
        frameBuffer.write(cmdBytes)
        return frameBuffer.readByteString()
    }

    private fun buildSubscribeFrame(channel: String): ByteString {
        val channelBytes = channel.encodeToByteArray()

        val subReqBuffer = Buffer()
        subReqBuffer.writeByte(0x0A)
        writeVarint(subReqBuffer, channelBytes.size)
        subReqBuffer.write(channelBytes)
        val subReqBytes = subReqBuffer.readByteArray()

        val cmdBuffer = Buffer()
        cmdBuffer.writeByte(0x08)
        cmdBuffer.writeByte(0x02)
        cmdBuffer.writeByte(0x2A)
        writeVarint(cmdBuffer, subReqBytes.size)
        cmdBuffer.write(subReqBytes)
        val cmdBytes = cmdBuffer.readByteArray()

        val frameBuffer = Buffer()
        writeVarint(frameBuffer, cmdBytes.size)
        frameBuffer.write(cmdBytes)
        return frameBuffer.readByteString()
    }

    private fun writeVarint(buffer: Buffer, value: Int) {
        var v = value
        while ((v and 0x7F.inv()) != 0) {
            buffer.writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
        buffer.writeByte(v and 0x7F)
    }

    private fun scheduleReconnect() {
        if (isManuallyDisconnected) return

        val attempt = reconnectAttempt
        if (attempt >= maxReconnectAttempts) {
            Log.w(TAG, "Max reconnect attempts reached ($attempt)")
            reportSystemMessage(UiText.StringResource(R.string.chat_reconnect_exhausted))
            return
        }

        reconnectJob?.cancel()
        reconnectJob = coroutineScope.launch {

            val baseDelay = min(1000L * 2.0.pow(attempt).toLong(), MAX_RECONNECT_DELAY_MS)
            val jitter = Random.nextLong(0, (baseDelay / 2).coerceAtLeast(1L))
            val delayMs =
                min(baseDelay + jitter, MAX_RECONNECT_DELAY_MS + MAX_RECONNECT_DELAY_MS / 2)
            Log.d(TAG, "Reconnecting in ${delayMs}ms (attempt ${attempt + 1})")
            delay(delayMs)

            if (isManuallyDisconnected) return@launch

            reconnectAttempt = attempt + 1
            currentChatroomId?.let { openConnection(it) }
        }
    }

    fun disconnect() {
        isManuallyDisconnected = true
        currentChatroomId = null
        channelSubscriberBadges = emptyMap()
        chatMode = ChatMode()
        reconnectJob?.cancel()
        emoteLoadJob?.cancel()
        val socketToClose = webSocket
        webSocket = null
        socketToClose?.close(1000, "Normal closure")

        synchronized(lock) {
            internalDeque.clear()
            _chatMessages.update { emptyList() }
        }
        clearEmotes()
    }
}
