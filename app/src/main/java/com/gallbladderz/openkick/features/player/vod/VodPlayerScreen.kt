/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.vod

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.utils.findActivity
import com.gallbladderz.openkick.features.player.VideoQuality
import com.gallbladderz.openkick.features.player.components.ClipInfoPanel
import com.gallbladderz.openkick.features.player.components.CustomPlayerControls
import com.gallbladderz.openkick.features.player.components.KickStreamPlayer
import com.gallbladderz.openkick.features.player.components.QualitySelectionSheet
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun VodPlayerRoute(
    videoId: String,
    onBackClick: () -> Unit,
    onStreamerClick: (String) -> Unit
) {
    val viewModel: VodPlayerViewModel = koinViewModel(parameters = { parametersOf(videoId) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    VodPlayerScreen(
        uiState = uiState,
        videoId = videoId,
        exoPlayer = viewModel.playerManager.player,
        onBackClick = onBackClick,
        onStreamerClick = onStreamerClick
    )
}

@Composable
fun VodPlayerScreen(
    uiState: VodPlayerUiState,
    videoId: String,
    exoPlayer: Player?,
    onBackClick: () -> Unit,
    onStreamerClick: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current

    var isPlaying by remember { mutableStateOf(exoPlayer?.isPlaying == true) }
    var playbackState by remember {
        mutableIntStateOf(
            exoPlayer?.playbackState ?: Player.STATE_IDLE
        )
    }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }


    var showSettingsSheet by remember { mutableStateOf(false) }
    var availableQualities by remember { mutableStateOf<List<VideoQuality>>(emptyList()) }
    var selectedQuality by remember { mutableStateOf<VideoQuality?>(null) }


    fun setQuality(quality: VideoQuality) {
        selectedQuality = quality
        val builder = exoPlayer?.trackSelectionParameters?.buildUpon() ?: return
        if (quality.isAudioOnly) {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
        } else {
            builder.setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
            if (quality.trackGroup == null || quality.trackIndex == null) {
                builder.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            } else {
                builder.setOverrideForType(
                    TrackSelectionOverride(quality.trackGroup, listOf(quality.trackIndex))
                )
            }
        }
        exoPlayer.trackSelectionParameters = builder.build()
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(play: Boolean) {
                isPlaying = play
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                if (state == Player.STATE_READY) {
                    duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val parsedQualities = mutableListOf<VideoQuality>()
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_VIDEO) {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            if (format.height != Format.NO_VALUE) {
                                val fps = if (format.frameRate > 0) format.frameRate.toInt()
                                    .toString() else ""
                                val name = "${format.height}p${if (fps == "60") "60" else ""}"
                                parsedQualities.add(VideoQuality(name, group.mediaTrackGroup, i))
                            }
                        }
                    }
                }
                val sortedQualities = parsedQualities
                    .distinctBy { it.name }
                    .sortedByDescending { it.name.substringBefore("p").toIntOrNull() ?: 0 }
                availableQualities = listOf(
                    VideoQuality(context.getString(R.string.auto_quality), null, null),
                    VideoQuality(
                        context.getString(R.string.audio_only),
                        null,
                        null,
                        isAudioOnly = true
                    )
                ) + sortedQualities
            }
        }
        exoPlayer?.addListener(listener)
        onDispose { exoPlayer?.removeListener(listener) }
    }

    LaunchedEffect(isPlaying, playbackState) {
        while (isPlaying && playbackState == Player.STATE_READY) {
            currentPosition = exoPlayer?.currentPosition ?: 0L
            duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
            delay(500)
        }
    }

    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(configuration.orientation) {
        isFullscreen = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
    LaunchedEffect(isFullscreen) {
        val activity = context.findActivity() ?: return@LaunchedEffect
        val window = activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (isFullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            delay(500)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    BackHandler(enabled = isFullscreen) { isFullscreen = false }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer?.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer?.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val rootModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .let { if (!isFullscreen) it.statusBarsPadding() else it }

    Column(modifier = rootModifier) {
        when (uiState) {
            is VodPlayerUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is VodPlayerUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is VodPlayerUiState.Success -> {
                Box(
                    modifier = if (isFullscreen) {
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showControls = !showControls }
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showControls = !showControls }
                    }
                ) {
                    if (exoPlayer != null) {
                        KickStreamPlayer(player = exoPlayer, modifier = Modifier.fillMaxSize())

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
                                    isFullscreen = isFullscreen,
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
                                    onFullscreen = { isFullscreen = !isFullscreen },
                                    onSettings = { showSettingsSheet = true },
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = {
                                        if (isFullscreen) isFullscreen = false else onBackClick()
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(8.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
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
                }

                if (!isFullscreen) {
                    ClipInfoPanel(
                        title = uiState.metadata.title,
                        streamerName = uiState.metadata.channelUsername,
                        streamerAvatarUrl = "",
                        views = uiState.metadata.views,
                        durationFormatted = uiState.metadata.durationFormatted,
                        isFollowed = false,
                        onToggleFollow = { },
                        onShareClick = {
                            val slug = uiState.metadata.channelSlug
                            val shareText = listOf(
                                "https://kick.com/video",
                                slug,
                                videoId
                            ).filter { it.isNotBlank() }.joinToString("/")
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share VOD"))
                        },
                        onStreamerClick = { onStreamerClick(uiState.metadata.channelSlug) }
                    )
                }
            }
        }
    }

    if (showSettingsSheet) {
        QualitySelectionSheet(
            qualities = availableQualities,
            selectedQuality = selectedQuality,
            onQualitySelect = { quality ->
                setQuality(quality)
                showSettingsSheet = false
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}