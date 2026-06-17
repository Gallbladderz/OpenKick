package com.gallbladderz.openkick.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.home.KickStreamPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.koin.androidx.compose.koinViewModel
import java.io.IOException

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Playing(
        val url: String,
        val avatarUrl: String,
        val viewers: Int,
        val title: String
    ) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

data class ChatMessage(val sender: String, val content: String)

class PlayerViewModel(private val okHttpClient: OkHttpClient) : ViewModel() {
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    private var webSocket: WebSocket? = null

    fun loadStreamInfo(streamerName: String) {
        _uiState.value = PlayerUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://kick.com/api/v2/channels/$streamerName")
                .addHeader("User-Agent", "KickMobile/40.21.0 (com.kick.mobile; platform: android; build:60006889)")
                .addHeader("X-App-Platform", "Android")
                .addHeader("X-App-Version", "40.21.0")
                .addHeader("X-Kick-App", "mobile")
                .addHeader("Accept", "application/json")
                .build()

            try {
                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    _uiState.value = PlayerUiState.Error("Стример не найден или оффлайн (Код: ${response.code})")
                    return@launch
                }

                processJson(responseBody)
            } catch (e: IOException) {
                _uiState.value = PlayerUiState.Error("Ошибка сети: ${e.message}")
            }
        }
    }

    private fun processJson(jsonString: String) {
        try {
            val jsonElement = Json { ignoreUnknownKeys = true }.parseToJsonElement(jsonString)

            if (jsonElement is JsonObject) {
                val livestreamObj = jsonElement["livestream"]?.jsonObject
                val userObj = jsonElement["user"]?.jsonObject

                val url = jsonElement["playback_url"]?.jsonPrimitive?.content
                    ?: livestreamObj?.get("playback_url")?.jsonPrimitive?.content

                val chatroomId = jsonElement["chatroom"]?.jsonObject?.get("id")?.jsonPrimitive?.content
                    ?: jsonElement["chatroom_id"]?.jsonPrimitive?.content

                var avatar = userObj?.get("profile_pic")?.jsonPrimitive?.content ?: ""
                avatar = avatar.replace("\\/", "/") // чистим слеши

                val viewers = livestreamObj?.get("viewer_count")?.jsonPrimitive?.intOrNull ?: 0
                val title = livestreamObj?.get("session_title")?.jsonPrimitive?.content ?: "Трансляция"

                if (!url.isNullOrEmpty()) {
                    _uiState.value = PlayerUiState.Playing(url, avatar, viewers, title)

                    if (chatroomId != null) {
                        connectToChat(chatroomId)
                    }
                } else {
                    _uiState.value = PlayerUiState.Error("Стример сейчас оффлайн")
                }
            } else {
                _uiState.value = PlayerUiState.Error("Пришел не JSON объект")
            }
        } catch (e: Exception) {
            _uiState.value = PlayerUiState.Error("Ошибка обработки ответа API")
        }
    }

    private fun connectToChat(chatroomId: String) {
        webSocket?.cancel()

        val request = Request.Builder()
            .url("wss://ws-us2.pusher.com/app/32cbd69e4b950bf97679?protocol=7&client=js&version=7.6.0&flash=false")
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val subscribeMsg = """{"event":"pusher:subscribe","data":{"auth":"","channel":"chatrooms.$chatroomId.v2"}}"""
                webSocket.send(subscribeMsg)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(text).jsonObject
                    val event = json["event"]?.jsonPrimitive?.content

                    if (event == "App\\Events\\ChatMessageEvent") {
                        val dataString = json["data"]?.jsonPrimitive?.content ?: return
                        val dataJson = Json { ignoreUnknownKeys = true }.parseToJsonElement(dataString).jsonObject

                        val sender = dataJson["sender"]?.jsonObject?.get("username")?.jsonPrimitive?.content ?: "Аноним"
                        val content = dataJson["content"]?.jsonPrimitive?.content ?: ""

                        val newMessage = ChatMessage(sender, content)
                        _chatMessages.value = (listOf(newMessage) + _chatMessages.value).take(100)
                    }
                } catch (e: Exception) {
                }
            }
        })
    }

    override fun onCleared() {
        super.onCleared()
        webSocket?.cancel()
    }
}

@Composable
fun PlayerScreen(
    streamerName: String,
    viewModel: PlayerViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isFollowed by remember { mutableStateOf(false) }

    LaunchedEffect(streamerName) {
        viewModel.loadStreamInfo(streamerName)
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }
            Text(
                text = streamerName,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            when (val currentState = state) {
                is PlayerUiState.Loading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is PlayerUiState.Playing -> {
                    KickStreamPlayer(videoUrl = currentState.url, modifier = Modifier.fillMaxSize())
                }
                is PlayerUiState.Error -> {
                    Text(
                        text = currentState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        if (state is PlayerUiState.Playing) {
            val playingState = state as PlayerUiState.Playing

            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    text = playingState.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(playingState.avatarUrl.ifEmpty { "https://ui-avatars.com/api/?name=$streamerName&background=random" })
                            .addHeader("User-Agent", "KickMobile/40.21.0")
                            .crossfade(true)
                            .build(),
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = streamerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${playingState.viewers}",
                                color = Color.Red,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = { isFollowed = !isFollowed },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFollowed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                            contentColor = if (isFollowed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isFollowed) "Отписаться" else "Отслеживать")
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = stringResource(R.string.stream_chat),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                reverseLayout = true,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(chatMessages) { message ->
                    Text(
                        text = "${message.sender}: ${message.content}",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}