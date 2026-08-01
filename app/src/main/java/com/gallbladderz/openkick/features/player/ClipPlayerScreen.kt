package com.gallbladderz.openkick.features.player

import android.content.Intent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.components.KickAvatar
import com.gallbladderz.openkick.features.player.components.CustomPlayerControls
import com.gallbladderz.openkick.features.player.components.KickStreamPlayer
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun ClipPlayerRoute(
    videoUrl: String,
    title: String,
    streamerName: String,
    streamerAvatarUrl: String,
    views: Int,
    durationFormatted: String,
    onBackClick: () -> Unit,
    onStreamerClick: (String) -> Unit = {},
    viewModel: PlayerViewModel = koinViewModel(),
    clipPlayerViewModel: ClipPlayerViewModel = koinViewModel()
) {
    val isFollowed by viewModel.isStreamerFollowed(streamerName)
        .collectAsStateWithLifecycle(initialValue = false)
    val fetchedAvatarUrl by clipPlayerViewModel.avatarUrl.collectAsStateWithLifecycle()

    ClipPlayerScreen(
        videoUrl = videoUrl,
        title = title,
        streamerName = streamerName,
        streamerAvatarUrl = streamerAvatarUrl,
        views = views,
        durationFormatted = durationFormatted,
        isFollowed = isFollowed,
        fetchedAvatarUrl = fetchedAvatarUrl,
        onToggleFollow = { viewModel.toggleFollow(streamerName, isFollowed) },
        onLoadAvatar = { clipPlayerViewModel.loadAvatar(streamerName) },
        onBackClick = onBackClick,
        onStreamerClick = onStreamerClick
    )
}

@Composable
fun ClipPlayerScreen(
    videoUrl: String,
    title: String,
    streamerName: String,
    streamerAvatarUrl: String,
    views: Int,
    durationFormatted: String,
    isFollowed: Boolean,
    fetchedAvatarUrl: String?,
    onToggleFollow: () -> Unit,
    onLoadAvatar: () -> Unit,
    onBackClick: () -> Unit,
    onStreamerClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val resolvedAvatarUrl = streamerAvatarUrl.replace("\\/", "/").ifBlank { fetchedAvatarUrl ?: "" }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_BUFFERING) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }


    LaunchedEffect(isPlaying, playbackState) {
        while (isPlaying && playbackState == Player.STATE_READY) {
            currentPosition = exoPlayer.currentPosition
            duration = exoPlayer.duration.coerceAtLeast(0L)
            delay(500)
        }
    }


    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(streamerName, streamerAvatarUrl) {
        if (resolvedAvatarUrl.isBlank() && streamerName.isNotBlank() && streamerName != context.getString(
                R.string.anonymous
            )
        ) {
            onLoadAvatar()
        }
    }


    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(play: Boolean) {
                isPlaying = play
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showControls = !showControls }
        ) {
            KickStreamPlayer(
                player = exoPlayer,
                modifier = Modifier.fillMaxSize()
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CustomPlayerControls(
                        playWhenReady = isPlaying,
                        playbackState = playbackState,
                        isFullscreen = false,
                        isLive = false,
                        currentPosition = currentPosition,
                        duration = duration,
                        onSeek = {
                            exoPlayer.seekTo(it)
                            currentPosition = it
                        },
                        onPlayPause = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        onFullscreen = { },
                        onSettings = { },
                        modifier = Modifier.fillMaxSize()
                    )

                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        ClipInfoPanel(
            title = title,
            streamerName = streamerName,
            streamerAvatarUrl = resolvedAvatarUrl,
            views = views,
            durationFormatted = durationFormatted,
            isFollowed = isFollowed,
            onToggleFollow = { onToggleFollow() },
            onShareClick = {
                val shareText = buildString {
                    append(title.ifBlank { context.getString(R.string.untitled) })
                    append('\n')
                    append(videoUrl)
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(Intent.createChooser(intent, null))
            },
            onStreamerClick = { onStreamerClick(streamerName) }
        )
    }
}

@Composable
private fun ClipInfoPanel(
    title: String,
    streamerName: String,
    streamerAvatarUrl: String,
    views: Int,
    durationFormatted: String,
    isFollowed: Boolean,
    onToggleFollow: () -> Unit,
    onShareClick: () -> Unit,
    onStreamerClick: () -> Unit
) {
    val anonymous = stringResource(R.string.anonymous)
    val canOpenStreamer = streamerName.isNotBlank() && streamerName != anonymous

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = title.ifBlank { stringResource(R.string.untitled) },
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
                avatarUrl = streamerAvatarUrl,
                streamerName = streamerName,
                size = 44.dp,
                modifier = Modifier.clickable(enabled = canOpenStreamer) { onStreamerClick() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = canOpenStreamer) { onStreamerClick() }
            ) {
                Text(
                    text = streamerName.ifBlank { anonymous },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.clip_views_duration, formatClipViews(views), durationFormatted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.share_desc)
                    )
                }

                FilledTonalButton(
                    onClick = onToggleFollow,
                    enabled = canOpenStreamer
                ) {
                    Text(if (isFollowed) stringResource(R.string.unfollow) else stringResource(R.string.follow))
                }
            }
        }
    }
}

private fun formatClipViews(views: Int): String {
    return when {
        views >= 1_000_000 -> String.format("%.1fM", views / 1_000_000.0)
        views >= 1_000 -> String.format("%.1fK", views / 1_000.0)
        else -> views.toString()
    }
}
