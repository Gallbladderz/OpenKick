/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

@file:Suppress("DEPRECATION")
@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.gallbladderz.openkick.features.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.utils.findActivity
import com.gallbladderz.openkick.features.player.components.CustomPlayerControls
import com.gallbladderz.openkick.features.player.components.KickStreamPlayer
import com.gallbladderz.openkick.features.player.components.PlayerTabs
import com.gallbladderz.openkick.features.player.components.QualitySelectionSheet
import com.gallbladderz.openkick.features.player.components.StreamerInfoCard
import com.gallbladderz.openkick.features.player.models.ChannelLink
import com.gallbladderz.openkick.features.player.models.ChatMessage
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayerRoute(
    streamerName: String,
    onBackClick: () -> Unit,
    onAvatarClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    viewModel: PlayerViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val channelLinks by viewModel.channelLinks.collectAsStateWithLifecycle()
    val isFollowed by viewModel.isStreamerFollowed(streamerName)
        .collectAsStateWithLifecycle(initialValue = false)
    val playWhenReady by viewModel.playerManager.playWhenReady.collectAsStateWithLifecycle()
    val playbackState by viewModel.playerManager.playbackState.collectAsStateWithLifecycle()
    val availableQualities by viewModel.availableQualities.collectAsStateWithLifecycle()
    val selectedQuality by viewModel.selectedQuality.collectAsStateWithLifecycle()

    PlayerScreen(
        streamerName = streamerName,
        state = state,
        chatMessages = chatMessages,
        channelLinks = channelLinks,
        isFollowed = isFollowed,
        playWhenReady = playWhenReady,
        playbackState = playbackState,
        availableQualities = availableQualities,
        selectedQuality = selectedQuality,
        player = viewModel.playerManager.player,
        onLoadStreamInfo = { viewModel.loadStreamInfo(it) },
        onLoadChannelLinks = { viewModel.loadChannelLinks(it) },
        onPlayerManagerInitialize = { viewModel.playerManager.initializePlayer() },
        onPlayerManagerPause = { viewModel.playerManager.pause() },
        onPlayerManagerResume = { viewModel.playerManager.resume() },
        onToggleFollow = { streamer, followed -> viewModel.toggleFollow(streamer, followed) },
        onSetVideoQuality = { viewModel.setVideoQuality(it) },
        onBackClick = onBackClick,
        onAvatarClick = onAvatarClick,
        onCategoryClick = onCategoryClick,
        onSeekToLiveEdge = { viewModel.playerManager.seekToLiveEdge() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    streamerName: String,
    state: PlayerUiState,
    chatMessages: List<ChatMessage>,
    channelLinks: List<ChannelLink>,
    isFollowed: Boolean,
    playWhenReady: Boolean,
    playbackState: Int,
    availableQualities: List<VideoQuality>,
    selectedQuality: VideoQuality?,
    player: Player?,
    onLoadStreamInfo: (String) -> Unit,
    onLoadChannelLinks: (String) -> Unit,
    onPlayerManagerInitialize: () -> Unit,
    onPlayerManagerPause: () -> Unit,
    onPlayerManagerResume: () -> Unit,
    onToggleFollow: (String, Boolean) -> Unit,
    onSetVideoQuality: (VideoQuality) -> Unit,
    onBackClick: () -> Unit,
    onAvatarClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onSeekToLiveEdge: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    var showControls by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    var isFullscreen by remember {
        mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isInPipMode by remember { mutableStateOf(false) }


    var isAtLiveEdge by remember { mutableStateOf(true) }
    var baselineOffset by remember { mutableStateOf(-1L) }
    var hasFallenBehind by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullToRefreshState()

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            onLoadStreamInfo(streamerName)
            delay(1000)
            pullRefreshState.endRefresh()
        }
    }

    DisposableEffect(context) {
        val activity = context.findActivity() as? ComponentActivity
        val pipListener = Consumer<androidx.core.app.PictureInPictureModeChangedInfo> { info ->
            isInPipMode = info.isInPictureInPictureMode
            if (!info.isInPictureInPictureMode) {
                isFullscreen = false
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        activity?.addOnPictureInPictureModeChangedListener(pipListener)

        onDispose {
            activity?.removeOnPictureInPictureModeChangedListener(pipListener)
        }
    }

    DisposableEffect(context) {
        onDispose {
            val activity = context.findActivity() as? ComponentActivity
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity?.setPictureInPictureParams(
                    PictureInPictureParams.Builder().setAutoEnterEnabled(false).build()
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        MediaController.Builder(context, sessionToken).buildAsync()
    }

    val ACTION_BACKGROUND_AUDIO = "com.gallbladderz.openkick.ACTION_BACKGROUND_AUDIO"

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: Intent?) {
                if (intent?.action == ACTION_BACKGROUND_AUDIO) {
                    val activity = context.findActivity() as? ComponentActivity
                    activity?.moveTaskToBack(true)
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(ACTION_BACKGROUND_AUDIO),
            ContextCompat.RECEIVER_EXPORTED
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(state, playWhenReady) {
        val activity = context.findActivity() as? ComponentActivity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val backgroundAudioIntent = Intent(ACTION_BACKGROUND_AUDIO)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                backgroundAudioIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val remoteAction = RemoteAction(
                Icon.createWithResource(context, R.drawable.ic_headphones),
                "Background Audio",
                "Play audio in background",
                pendingIntent
            )

            val paramsBuilder = PictureInPictureParams.Builder()
                .setActions(listOf(remoteAction))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                paramsBuilder.setAutoEnterEnabled(state is PlayerUiState.Playing && playWhenReady)
            }
            activity?.setPictureInPictureParams(paramsBuilder.build())
        }
    }

    val tabs = listOf(
        stringResource(R.string.chat_tab),
        stringResource(R.string.description)
    )

    LaunchedEffect(showControls, playWhenReady) {
        if (showControls && playWhenReady) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(playWhenReady, playbackState, player) {
        while (true) {
            player?.let {
                if (it.isCurrentMediaItemLive) {
                    val liveOffset = it.currentLiveOffset
                    if (liveOffset != androidx.media3.common.C.TIME_UNSET) {
                        if (baselineOffset == -1L || liveOffset < baselineOffset) {
                            baselineOffset = liveOffset
                        }

                        if (playbackState == Player.STATE_READY && !playWhenReady) {
                            hasFallenBehind = true
                        }

                        isAtLiveEdge =
                            playWhenReady && !hasFallenBehind && (liveOffset <= baselineOffset + 4000L)
                    }
                } else {
                    isAtLiveEdge = true
                }
            }
            delay(500)
        }
    }

    LaunchedEffect(configuration.orientation) {
        if (!isInPipMode) {
            isFullscreen = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
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

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    onPlayerManagerInitialize()
                    onLoadStreamInfo(streamerName)
                }

                Lifecycle.Event.ON_STOP -> {
                }

                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(streamerName) {
        onLoadChannelLinks(streamerName)
    }

    val rootModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .let { if (!isFullscreen && !isInPipMode) it.statusBarsPadding() else it }

    Column(modifier = rootModifier) {
        Box(
            modifier = Modifier
                .nestedScroll(pullRefreshState.nestedScrollConnection)
                .let { if (isFullscreen || isInPipMode) it.weight(1f) else it }
        ) {
            Column(
                modifier = if (!isFullscreen && !isInPipMode) {
                    Modifier.verticalScroll(rememberScrollState())
                } else {
                    Modifier
                }
            ) {
                Box(
                    modifier = if (isInPipMode) {
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    } else if (isFullscreen) {
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
                    when (val currentState = state) {
                        is PlayerUiState.Loading -> {
                            CircularProgressIndicator(
                                color = Color(0xFF7CFC00),
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        is PlayerUiState.Playing -> {
                            if (player != null) {
                                KickStreamPlayer(
                                    player = player,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF7CFC00),
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = showControls && !isInPipMode,
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    CustomPlayerControls(
                                        playWhenReady = playWhenReady,
                                        playbackState = playbackState,
                                        isFullscreen = isFullscreen,
                                        isAtLiveEdge = isAtLiveEdge,
                                        onSeekToLive = {
                                            onSeekToLiveEdge()
                                            baselineOffset = -1L
                                            hasFallenBehind = false
                                            isAtLiveEdge = true
                                            if (!playWhenReady) onPlayerManagerResume()
                                        },
                                        onPlayPause = {
                                            if (playWhenReady) onPlayerManagerPause()
                                            else onPlayerManagerResume()
                                        },
                                        onFullscreen = { isFullscreen = !isFullscreen },
                                        onSettings = { showSettingsSheet = true },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    IconButton(
                                        onClick = {
                                            if (isFullscreen) isFullscreen =
                                                false else onBackClick()
                                        },
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                            .background(
                                                Color.Black.copy(alpha = 0.5f),
                                                shape = MaterialTheme.shapes.small
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = stringResource(R.string.back),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
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

                if (!isFullscreen && !isInPipMode) {
                    if (state is PlayerUiState.Playing) {
                        val playingState = state
                        StreamerInfoCard(
                            streamerName = streamerName,
                            title = playingState.title,
                            avatarUrl = playingState.avatarUrl,
                            viewers = playingState.viewers,
                            categoryName = playingState.categoryName,
                            isFollowed = isFollowed,
                            onToggleFollow = { onToggleFollow(streamerName, isFollowed) },
                            onAvatarClick = { onAvatarClick(streamerName) },
                            onCategoryClick = {
                                playingState.categorySlug?.let { slug -> onCategoryClick(slug) }
                            },
                            onShareClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "https://kick.com/$streamerName")
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        context.getString(R.string.share_desc)
                                    )
                                )
                            }
                        )
                    }
                }
            }

            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }

        if (!isFullscreen && !isInPipMode) {
            PlayerTabs(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                tabs = tabs,
                chatMessages = chatMessages,
                channelLinks = channelLinks,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (showSettingsSheet) {
        QualitySelectionSheet(
            qualities = availableQualities,
            selectedQuality = selectedQuality,
            onQualitySelect = { quality ->
                onSetVideoQuality(quality)
                showSettingsSheet = false
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}