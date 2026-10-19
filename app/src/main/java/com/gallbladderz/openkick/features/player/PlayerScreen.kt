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
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
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
import com.gallbladderz.openkick.MainActivity
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.datastore.ChatSettings
import com.gallbladderz.openkick.core.datastore.SettingsRepository
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteSet
import com.gallbladderz.openkick.core.domain.chat.ChatMessage
import com.gallbladderz.openkick.core.domain.model.ChannelLink
import com.gallbladderz.openkick.core.player.PlaybackService
import com.gallbladderz.openkick.core.player.PlayerExpandedState
import com.gallbladderz.openkick.core.player.VideoQuality
import com.gallbladderz.openkick.core.ui.utils.DeviceOrientation
import com.gallbladderz.openkick.core.ui.utils.findActivity
import com.gallbladderz.openkick.core.ui.utils.rememberDeviceOrientation
import com.gallbladderz.openkick.features.player.components.PlayerContent
import com.gallbladderz.openkick.features.player.components.PlayerTabs
import com.gallbladderz.openkick.features.player.components.QualitySelectionSheet
import com.gallbladderz.openkick.features.player.components.StreamerInfoOverlay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun PlayerRoute(
    streamerName: String,
    dragState: AnchoredDraggableState<PlayerExpandedState>,
    onBackClick: () -> Unit,
    onAvatarClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    viewModel: PlayerViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val chatSendState by viewModel.chatSendState.collectAsStateWithLifecycle()
    val chatEmoteSets by viewModel.chatEmoteSets.collectAsStateWithLifecycle()
    val chatMode by viewModel.chatMode.collectAsStateWithLifecycle()
    val channelLinks by viewModel.channelLinks.collectAsStateWithLifecycle()
    val isFollowed by viewModel.isStreamerFollowed(streamerName)
        .collectAsStateWithLifecycle(initialValue = false)

    val playWhenReady by viewModel.playerManager.playWhenReady.collectAsStateWithLifecycle()
    val playbackState by viewModel.playerManager.playbackState.collectAsStateWithLifecycle()
    val availableQualities by viewModel.availableQualities.collectAsStateWithLifecycle()
    val selectedQuality by viewModel.selectedQuality.collectAsStateWithLifecycle()

    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val settingsRepository: SettingsRepository = koinInject()
    val chatSettings by settingsRepository.chatSettingsFlow.collectAsStateWithLifecycle(
        initialValue = ChatSettings(),
        lifecycle = LocalLifecycleOwner.current.lifecycle
    )
    val settingsScope = rememberCoroutineScope()
    val settingsWriteMutex = remember { Mutex() }
    var displayedChatSettings by remember { mutableStateOf(chatSettings) }

    LaunchedEffect(chatSettings) { displayedChatSettings = chatSettings }

    val closePlayer = {
        viewModel.playerManager.release()
        viewModel.disconnectChat()
        onBackClick()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.playerManager.release()
            viewModel.disconnectChat()
        }
    }

    PlayerScreen(
        streamerName = streamerName,
        uiState = uiState,
        dragState = dragState,
        chatMessages = chatMessages,
        chatSendState = chatSendState,
        chatEmoteSets = chatEmoteSets,
        chatMode = chatMode,
        isAuthenticated = isAuthenticated,
        chatSettings = displayedChatSettings,
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
        onPlayerManagerRelease = {
            viewModel.playerManager.release()
            viewModel.disconnectChat()
        },
        onToggleFollow = { streamer, followed -> viewModel.toggleFollow(streamer, followed) },
        onSetVideoQuality = { viewModel.setVideoQuality(it) },
        onBackClick = closePlayer,
        onAvatarClick = onAvatarClick,
        onCategoryClick = { slug -> onCategoryClick(slug) },
        onSeekToLiveEdge = { viewModel.playerManager.seekToLiveEdge() },
        onSendChatMessage = { content, replyTo -> viewModel.sendChatMessage(content, replyTo) },
        onChatSettingsChange = { updated ->
            displayedChatSettings = updated
            settingsScope.launch {
                settingsWriteMutex.withLock {
                    settingsRepository.setChatSettings(updated)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    streamerName: String,
    uiState: PlayerUiState,
    dragState: AnchoredDraggableState<PlayerExpandedState>,
    chatMessages: List<ChatMessage>,
    chatSendState: ChatSendState,
    chatEmoteSets: List<ChatEmoteSet> = emptyList(),
    chatMode: com.gallbladderz.openkick.core.domain.chat.ChatMode = com.gallbladderz.openkick.core.domain.chat.ChatMode(),
    isAuthenticated: Boolean,
    channelLinks: List<ChannelLink>,
    chatSettings: ChatSettings = ChatSettings(),
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
    onPlayerManagerRelease: () -> Unit,
    onToggleFollow: (String, Boolean) -> Unit,
    onSetVideoQuality: (VideoQuality) -> Unit,
    onBackClick: () -> Unit,
    onAvatarClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onSeekToLiveEdge: () -> Unit = {},
    onSendChatMessage: (String, ChatMessage?) -> Unit = { _, _ -> },
    onChatSettingsChange: (ChatSettings) -> Unit = {}
) {
    val context = LocalContext.current
    val backgroundAudioTitle = stringResource(R.string.pip_background_audio_title)
    val backgroundAudioDescription = stringResource(R.string.pip_background_audio_desc)
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    var isInPipMode by remember {
        mutableStateOf(context.findActivity()?.isInPictureInPictureMode == true)
    }
    var videoViewBounds by remember { mutableStateOf(android.graphics.Rect()) }

    val fraction by remember {
        derivedStateOf {
            if (isInPipMode) return@derivedStateOf 0f

            val offset = if (dragState.offset.isNaN()) 0f else dragState.requireOffset()
            val expandedOffset = dragState.anchors.positionOf(PlayerExpandedState.EXPANDED)
            val miniOffset = dragState.anchors.positionOf(PlayerExpandedState.MINI)
            if (miniOffset == expandedOffset || offset.isNaN()) 0f
            else ((offset - expandedOffset) / (miniOffset - expandedOffset)).coerceIn(0f, 1f)
        }
    }

    var showControls by remember { mutableStateOf(false) }
    var isChatExpanded by remember { mutableStateOf(false) }
    var playerInteractionTick by remember { mutableIntStateOf(0) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showChatSettingsSheet by remember { mutableStateOf(false) }

    val deviceOrientation = rememberDeviceOrientation()
    var isFullscreen by remember {
        mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    var isAtLiveEdge by remember { mutableStateOf(true) }
    var baselineOffset by remember { mutableStateOf(-1L) }
    var hasFallenBehind by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val activity = context.findActivity()
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
            val activity = context.findActivity()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity?.setPictureInPictureParams(
                    PictureInPictureParams.Builder().setAutoEnterEnabled(false).build()
                )
            }
        }
    }

    DisposableEffect(context) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        var controller: MediaController? = null
        var disposed = false
        future.addListener({
            val builtController = runCatching { future.get() }.getOrNull()
            if (disposed) {
                builtController?.release()
            } else {
                controller = builtController
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            controller?.release()
            if (!future.isDone) future.cancel(false)
        }
    }

    LaunchedEffect(uiState, playWhenReady, videoViewBounds) {
        val activity = context.findActivity()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val backgroundAudioIntent = Intent(context, MainActivity::class.java)
                .setAction(PlaybackService.ACTION_PIP_AUDIO_ONLY)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                backgroundAudioIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val remoteAction = RemoteAction(
                Icon.createWithResource(context, R.drawable.ic_headphones),
                backgroundAudioTitle,
                backgroundAudioDescription,
                pendingIntent
            )

            val paramsBuilder = PictureInPictureParams.Builder()
                .setActions(listOf(remoteAction))
                .setAspectRatio(android.util.Rational(16, 9))

            if (!videoViewBounds.isEmpty) {
                paramsBuilder.setSourceRectHint(videoViewBounds)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                paramsBuilder.setAutoEnterEnabled(uiState is PlayerUiState.Playing && playWhenReady)
            }
            activity?.setPictureInPictureParams(paramsBuilder.build())
        }
    }

    LaunchedEffect(showControls, playWhenReady) {
        if (showControls && playWhenReady) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(playWhenReady, isFullscreen, isInPipMode, fraction, playerInteractionTick) {
        if (!playWhenReady || isFullscreen || isInPipMode || fraction > 0.1f) {
            isChatExpanded = false
            return@LaunchedEffect
        }

        delay(3_000)
        isChatExpanded = true
    }

    LaunchedEffect(playWhenReady, playbackState, player) {
        while (player != null) {
            val currentPlayer = player ?: break
            if (currentPlayer.isCurrentMediaItemLive) {
                val liveOffset = currentPlayer.currentLiveOffset
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
            delay(500)
        }
    }

    LaunchedEffect(deviceOrientation) {
        if (!isInPipMode) {
            when (deviceOrientation) {
                DeviceOrientation.LANDSCAPE -> {
                    if (dragState.currentValue == PlayerExpandedState.EXPANDED) {
                        isFullscreen = true
                    }
                }

                DeviceOrientation.PORTRAIT -> isFullscreen = false
                DeviceOrientation.UNKNOWN -> {}
            }
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
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    BackHandler(enabled = isFullscreen || dragState.currentValue == PlayerExpandedState.EXPANDED) {
        if (isFullscreen) {
            isFullscreen = false
        } else {
            coroutineScope.launch { dragState.animateTo(PlayerExpandedState.MINI) }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> onPlayerManagerInitialize()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(streamerName) {
        baselineOffset = -1L
        hasFallenBehind = false
        isAtLiveEdge = true
        onLoadStreamInfo(streamerName)
        onLoadChannelLinks(streamerName)
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val animatedTopPadding = lerp(statusBarHeight, 0.dp, fraction)
    val screenHeight = configuration.screenHeightDp.dp
    val miniPlayerHeight = 64.dp
    val currentHeight = lerp(screenHeight, miniPlayerHeight, fraction)

    val rootModifier = Modifier
        .fillMaxWidth()
        .let {
            if (isFullscreen || isInPipMode) it.fillMaxSize()
            else it.height(currentHeight)
        }
        .background(MaterialTheme.colorScheme.background.copy(alpha = 1f - fraction))
        .let {
            if (!isFullscreen && !isInPipMode) it.padding(top = animatedTopPadding) else it
        }
        .clipToBounds()

    Column(modifier = rootModifier) {
        Box(modifier = Modifier.let { if (isFullscreen || isInPipMode) it.weight(1f) else it }) {
            Column {
                PlayerContent(
                    streamerName = streamerName,
                    uiState = uiState,
                    player = player,
                    fraction = fraction,
                    isInPipMode = isInPipMode,
                    isFullscreen = isFullscreen,
                    showControls = showControls,
                    onShowControlsChange = { showControls = it },
                    isChatExpanded = isChatExpanded,
                    onPlayerInteraction = {
                        isChatExpanded = false
                        playerInteractionTick++
                    },
                    playWhenReady = playWhenReady,
                    playbackState = playbackState,
                    isAtLiveEdge = isAtLiveEdge,
                    onSeekToLiveEdge = {
                        onSeekToLiveEdge()
                        baselineOffset = -1L
                        hasFallenBehind = false
                        isAtLiveEdge = true
                        if (!playWhenReady) onPlayerManagerResume()
                    },
                    onPlayerManagerResume = onPlayerManagerResume,
                    onPlayerManagerPause = onPlayerManagerPause,
                    onPlayerManagerRelease = onPlayerManagerRelease,
                    onFullscreenToggle = { isFullscreen = !isFullscreen },
                    onShowSettings = { showSettingsSheet = true },
                    onBackClick = onBackClick,
                    onUpdateVideoBounds = { videoViewBounds = it },
                    dragState = dragState,
                    coroutineScope = coroutineScope,
                    chatMessages = chatMessages,
                    onSendMessage = onSendChatMessage,
                    chatSendState = chatSendState,
                    chatEmoteSets = chatEmoteSets,
                    chatMode = chatMode,
                    isAuthenticated = isAuthenticated,
                    chatSettings = chatSettings,
                    onChatSettingsClick = { showChatSettingsSheet = true },
                    onProfileClick = onAvatarClick
                )

                if (!isFullscreen && !isInPipMode) {
                    AnimatedVisibility(
                        visible = !isChatExpanded && uiState is PlayerUiState.Playing && fraction <= 0.5f,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        if (uiState is PlayerUiState.Playing) {
                            StreamerInfoOverlay(
                                streamerName = streamerName,
                                title = uiState.title,
                                avatarUrl = uiState.avatarUrl,
                                viewers = uiState.viewers,
                                categoryName = uiState.categoryName ?: "",
                                categorySlug = uiState.categorySlug,
                                isVerified = uiState.isVerified,
                                isFollowed = isFollowed,
                                fraction = fraction,
                                onToggleFollow = { onToggleFollow(streamerName, isFollowed) },
                                onAvatarClick = { onAvatarClick(streamerName) },
                                onCategoryClick = { slug -> onCategoryClick(slug) }
                            )
                        }
                    }
                }
            }
        }

        if (!isFullscreen && !isInPipMode && fraction <= 0.5f) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = 1f - (fraction * 2f).coerceIn(0f, 1f) }
            ) {
                PlayerTabs(
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { selectedTabIndex = it },
                    tabs = listOf(
                        stringResource(R.string.chat_tab),
                        stringResource(R.string.description)
                    ),
                    chatMessages = chatMessages,
                    channelLinks = channelLinks,
                    onSendMessage = onSendChatMessage,
                    chatSendState = chatSendState,
                    chatEmoteSets = chatEmoteSets,
                    chatMode = chatMode,
                    isAuthenticated = isAuthenticated,
                    chatSettings = chatSettings,
                    onChatSettingsClick = { showChatSettingsSheet = true },
                    onProfileClick = onAvatarClick,
                    modifier = Modifier.fillMaxSize()
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

        if (showChatSettingsSheet) {
            com.gallbladderz.openkick.features.player.components.ChatSettingsSheet(
                settings = chatSettings,
                onSettingsChange = onChatSettingsChange,
                onDismiss = { showChatSettingsSheet = false }
            )
        }
    }
}
