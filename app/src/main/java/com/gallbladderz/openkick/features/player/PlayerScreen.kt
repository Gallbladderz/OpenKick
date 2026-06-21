package com.gallbladderz.openkick.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.network.KickApiConstants
import com.gallbladderz.openkick.core.ui.components.KickAvatar
import com.gallbladderz.openkick.core.ui.components.ViewerCountBadge
import com.gallbladderz.openkick.features.player.models.ChatMessage
import org.koin.androidx.compose.koinViewModel


@OptIn(UnstableApi::class)
@Composable
fun KickStreamPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(videoUrl) {
        
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(KickApiConstants.USER_AGENT)
            .setDefaultRequestProperties(
                mapOf(
                    "Origin" to "https://kick.com",
                    "Referer" to "https://kick.com/"
                )
            )

        val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true

        onDispose {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = modifier
    )
}


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

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.chat_tab),
        stringResource(R.string.info_tab),
        stringResource(R.string.clips_tab)
    )

    
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
                        color = Color(0xFF7CFC00),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is PlayerUiState.Playing -> {
                    KickStreamPlayer(
                        videoUrl = currentState.url,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is PlayerUiState.Error -> {
                    Text(
                        text = currentState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        
        if (state is PlayerUiState.Playing) {
            val playingState = state as PlayerUiState.Playing
            StreamerInfoCard(
                streamerName = streamerName,
                title = playingState.title,
                avatarUrl = playingState.avatarUrl,
                viewers = playingState.viewers,
                isFollowed = isFollowed,
                onToggleFollow = { viewModel.toggleFollow(streamerName, isFollowed) }
            )
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
                0 -> ChatList(chatMessages = chatMessages)
                1 -> InfoPlaceholder()
                2 -> ClipsPlaceholder()
            }
        }
    }
}

@Composable
fun StreamerInfoCard(
    streamerName: String,
    title: String,
    avatarUrl: String,
    viewers: Int,
    isFollowed: Boolean,
    onToggleFollow: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = title,
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
            KickAvatar(
                avatarUrl = avatarUrl,
                streamerName = streamerName,
                size = 44.dp
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
                    ViewerCountBadge(viewers = viewers, textColor = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.uptime_desc),
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            FilledTonalButton(onClick = onToggleFollow) {
                Text(if (isFollowed) stringResource(R.string.unfollow) else stringResource(R.string.follow))
            }
        }
    }
}

@Composable
fun ChatList(chatMessages: List<ChatMessage>) {
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

@Composable
fun InfoPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.info_placeholder),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ClipsPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.clips_placeholder),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}