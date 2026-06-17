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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.home.KickStreamPlayer
import com.gallbladderz.openkick.core.network.KickApiConstants
import org.koin.androidx.compose.koinViewModel

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
                            .addHeader("User-Agent", KickApiConstants.USER_AGENT)
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