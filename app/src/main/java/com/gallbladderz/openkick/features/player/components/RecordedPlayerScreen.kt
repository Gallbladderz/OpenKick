/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.player.PlayerExpandedState
import com.gallbladderz.openkick.core.player.VideoQuality
import com.gallbladderz.openkick.core.player.toVideoQualityOptions
import com.gallbladderz.openkick.core.ui.utils.DeviceOrientation
import com.gallbladderz.openkick.core.ui.utils.findActivity
import com.gallbladderz.openkick.core.ui.utils.rememberDeviceOrientation
import com.gallbladderz.openkick.features.player.MiniPlayerHeight
import com.gallbladderz.openkick.features.player.MiniPlayerWidth
import com.gallbladderz.openkick.features.player.collapseFraction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class RecordedPlayback(
    val positionMs: Long,
    val durationMs: Long,
    val quality: VideoQuality?
)

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun RecordedPlayerScreen(
    player: Player,
    dragState: AnchoredDraggableState<PlayerExpandedState>,
    title: String,
    channelName: String,
    onBack: () -> Unit,
    offline: Boolean = false,
    chat: (@Composable (RecordedPlayback, Modifier) -> Unit)? = null,
    info: @Composable (RecordedPlayback) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    var isFullscreen by remember {
        mutableStateOf(
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                dragState.settledValue == PlayerExpandedState.EXPANDED
        )
    }
    var portraitExit by remember { mutableStateOf(false) }
    val toggleFullscreen = {
        portraitExit = isFullscreen
        isFullscreen = !isFullscreen
    }
    val fraction = if (isFullscreen) 0f else dragState.collapseFraction()
    var playWhenReady by remember(player) { mutableStateOf(player.playWhenReady) }
    var playbackState by remember(player) { mutableIntStateOf(player.playbackState) }
    var position by remember(player) { mutableLongStateOf(player.currentPosition) }
    var duration by remember(player) { mutableLongStateOf(player.duration.coerceAtLeast(0)) }
    var failed by remember(player) { mutableStateOf(player.playerError != null) }
    var showControls by remember { mutableStateOf(true) }
    var showQuality by remember { mutableStateOf(false) }
    var qualities by remember(player) {
        mutableStateOf(if (offline) emptyList() else player.currentTracks.toVideoQualityOptions(context))
    }
    var selectedQuality by remember(player) { mutableStateOf<VideoQuality?>(null) }
    val seekInteractions = remember { MutableInteractionSource() }
    val dragging by seekInteractions.collectIsDraggedAsState()
    val orientation = rememberDeviceOrientation()

    DisposableEffect(player, lifecycle) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                playWhenReady = player.playWhenReady
                playbackState = player.playbackState
                position = player.currentPosition
                duration = player.duration.coerceAtLeast(0)
                if (!offline && events.contains(Player.EVENT_TRACKS_CHANGED)) {
                    qualities = player.currentTracks.toVideoQualityOptions(context)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }
        }
        var resumePlayback = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    resumePlayback = player.playWhenReady
                    player.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (resumePlayback) player.play()
                    resumePlayback = false
                }
                else -> Unit
            }
        }
        player.addListener(listener)
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.pause()
        }
    }
    LaunchedEffect(player, playWhenReady, playbackState) {
        while (playWhenReady && playbackState == Player.STATE_READY) {
            position = player.currentPosition
            delay(250)
        }
    }
    LaunchedEffect(showControls, playWhenReady, dragging) {
        if (showControls && playWhenReady && !dragging) {
            delay(3_000)
            showControls = false
        }
    }
    LaunchedEffect(orientation, dragState.settledValue) {
        when (orientation) {
            DeviceOrientation.LANDSCAPE -> {
                if (!portraitExit && dragState.settledValue == PlayerExpandedState.EXPANDED) {
                    isFullscreen = true
                }
            }
            DeviceOrientation.PORTRAIT -> isFullscreen = false
            DeviceOrientation.UNKNOWN -> Unit
        }
    }
    LaunchedEffect(orientation, configuration.orientation, portraitExit) {
        if (portraitExit && orientation == DeviceOrientation.PORTRAIT &&
            configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        ) portraitExit = false
    }
    DisposableEffect(isFullscreen, portraitExit) {
        val activity = context.findActivity()
        val insets = activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val managesOrientation = isFullscreen || portraitExit
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            insets?.hide(WindowInsetsCompat.Type.systemBars())
            insets?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else if (portraitExit) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            if (managesOrientation) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                insets?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    BackHandler(enabled = isFullscreen || dragState.settledValue == PlayerExpandedState.EXPANDED) {
        if (isFullscreen) toggleFullscreen()
        else scope.launch { dragState.animateTo(PlayerExpandedState.MINI) }
    }

    if (failed) {
        RecordedPlayerPlaceholder(
            dragState, title, onBack,
            error = if (offline) R.string.download_file_error else R.string.load_error
        )
        return
    }
    val expand: () -> Unit = { scope.launch { dragState.animateTo(PlayerExpandedState.EXPANDED) } }
    val close: () -> Unit = { scope.launch { dragState.animateTo(PlayerExpandedState.HIDDEN) } }
    val resume = {
        if (playbackState == Player.STATE_ENDED) player.seekTo(0)
        player.play()
    }
    val playerActive = playWhenReady && playbackState != Player.STATE_ENDED
    val videoBackground = androidx.compose.ui.graphics.lerp(
        Color.Black, MaterialTheme.colorScheme.surfaceVariant, fraction
    )
    Column(recordedFrame(fraction, isFullscreen)) {
        val expandedVideoHeight = configuration.screenWidthDp.dp * (9f / 16f)
        PlayerGestureArea(
            player = player,
            fullscreen = isFullscreen,
            enabled = fraction < 0.1f && dragState.settledValue == PlayerExpandedState.EXPANDED,
            onToggleFullscreen = toggleFullscreen,
            modifier = (if (isFullscreen) Modifier.fillMaxSize()
            else Modifier.fillMaxWidth().height(lerp(expandedVideoHeight, MiniPlayerHeight, fraction)))
                .background(videoBackground)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    if (dragState.settledValue == PlayerExpandedState.MINI) expand()
                    else showControls = !showControls
                }
        ) {
            KickStreamPlayer(
                player,
                if (isFullscreen) Modifier.fillMaxSize() else Modifier
                    .width(lerp(configuration.screenWidthDp.dp, MiniPlayerWidth, fraction))
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = showControls && fraction < 0.1f,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                CustomPlayerControls(
                    playWhenReady = playerActive,
                    playbackState = playbackState,
                    isFullscreen = isFullscreen,
                    isLive = false,
                    currentPosition = position,
                    duration = duration,
                    onSeek = {
                        player.seekTo(it)
                        position = it
                    },
                    liveTimelineInteractions = seekInteractions,
                    onPlayPause = { if (playerActive) player.pause() else resume() },
                    onFullscreen = toggleFullscreen,
                    onSettings = if (offline) null else { { showQuality = true } },
                    onBack = { if (isFullscreen) toggleFullscreen() else onBack() },
                    modifier = Modifier.fillMaxSize()
                )
            }
            MiniPlayerDetails(
                fraction = fraction,
                title = title,
                streamerName = channelName,
                playWhenReady = playerActive,
                onPlayerManagerPause = player::pause,
                onPlayerManagerResume = resume,
                onExpand = expand,
                onClose = close,
                miniPlayerWidth = MiniPlayerWidth
            )
        }
        if (!isFullscreen && fraction < 0.5f) {
            val format = (player as? ExoPlayer)?.videoFormat
            val quality = selectedQuality ?: qualities.firstOrNull { option ->
                val track = option.trackIndex?.let { option.trackGroup?.getFormat(it) }
                track != null && track.height == format?.height && track.frameRate == format.frameRate
            }
            val playback = RecordedPlayback(position, duration, quality)
            Column(
                Modifier.weight(1f).fillMaxWidth().navigationBarsPadding()
                    .graphicsLayer { alpha = (1f - fraction * 2f).coerceIn(0f, 1f) }
                    .let { if (chat == null) it.verticalScroll(rememberScrollState()) else it }
            ) {
                info(playback)
                chat?.invoke(playback, Modifier.weight(1f))
            }
        }
    }
    if (showQuality) {
        QualitySelectionSheet(
            qualities, selectedQuality,
            onQualitySelect = { quality ->
                selectedQuality = quality
                val parameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, quality.isAudioOnly)
                if (quality.trackGroup == null || quality.trackIndex == null) {
                    parameters.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                } else {
                    parameters.setOverrideForType(TrackSelectionOverride(quality.trackGroup, listOf(quality.trackIndex)))
                }
                player.trackSelectionParameters = parameters.build()
                showQuality = false
            },
            onDismiss = { showQuality = false }
        )
    }
}

@Composable
private fun recordedFrame(fraction: Float, fullscreen: Boolean = false): Modifier {
    if (fullscreen) return Modifier.fillMaxSize().background(Color.Black)
    val windowHeight = LocalConfiguration.current.screenHeightDp.dp
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return Modifier.fillMaxWidth().height(lerp(windowHeight, MiniPlayerHeight, fraction))
        .background(MaterialTheme.colorScheme.background.copy(alpha = 1f - fraction))
        .padding(top = lerp(statusBarHeight, 0.dp, fraction))
        .clipToBounds()
}

@Composable
internal fun RecordedPlayerPlaceholder(
    dragState: AnchoredDraggableState<PlayerExpandedState>,
    title: String,
    onBack: () -> Unit,
    error: Int? = null,
    message: String? = null
) {
    val fraction = dragState.collapseFraction()
    val scope = rememberCoroutineScope()
    val expand: () -> Unit = { scope.launch { dragState.animateTo(PlayerExpandedState.EXPANDED) } }
    val close: () -> Unit = { scope.launch { dragState.animateTo(PlayerExpandedState.HIDDEN) } }
    BackHandler(enabled = dragState.settledValue == PlayerExpandedState.EXPANDED) {
        scope.launch { dragState.animateTo(PlayerExpandedState.MINI) }
    }
    Box(
        recordedFrame(fraction)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = fraction))
            .let { if (fraction > 0.5f) it.clickable(onClick = expand) else it }
    ) {
        if (fraction <= 0.5f) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button))
            }
            val text = message ?: error?.let { stringResource(it) }
            if (text != null) {
                Text(text, modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    color = MaterialTheme.colorScheme.error)
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
        MiniPlayerDetails(
            fraction = fraction,
            title = title,
            streamerName = "",
            playWhenReady = false,
            onPlayerManagerPause = {},
            onPlayerManagerResume = {},
            onExpand = expand,
            onClose = close,
            miniPlayerWidth = MiniPlayerWidth,
            showPlaybackControls = false
        )
    }
}
