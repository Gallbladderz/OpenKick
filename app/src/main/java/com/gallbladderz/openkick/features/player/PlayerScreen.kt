package com.gallbladderz.openkick.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.home.KickStreamPlayer
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    streamerName: String,
    viewModel: PlayerViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()

    val isFollowed by viewModel.isStreamerFollowed(streamerName).collectAsStateWithLifecycle(initialValue = false)

    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Чат", "Инфо", "Клипы")

    LaunchedEffect(streamerName) {
        viewModel.loadStreamInfo(streamerName)
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            when (val currentState = state) {
                is PlayerUiState.Loading -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is PlayerUiState.Playing -> {
                    KickStreamPlayer(videoUrl = currentState.url, modifier = Modifier.fillMaxSize())
                }
                is PlayerUiState.Error -> {
                    Text(
                        text = currentState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(playingState.avatarUrl.ifEmpty { "https://ui-avatars.com/api/?name=$streamerName&background=random" })
                            .crossfade(true)
                            .build(),
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
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
                                modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${playingState.viewers}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Uptime",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "2:15:34",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { viewModel.toggleFollow(streamerName, isFollowed) }
                    ) {
                        Text(if (isFollowed) "Отписаться" else "Отслеживать")
                    }
                }
            }
        }

        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.Medium) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            when (selectedTabIndex) {
                0 -> {
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
                1 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Здесь скоро будет описание канала",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                2 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Здесь скоро будут клипы",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}