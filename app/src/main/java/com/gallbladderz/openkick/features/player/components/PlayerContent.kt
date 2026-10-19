/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */



package com.gallbladderz.openkick.features.player.components


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.media3.common.Player
import com.gallbladderz.openkick.core.datastore.ChatSettings
import com.gallbladderz.openkick.core.domain.chat.ChatEmoteSet
import com.gallbladderz.openkick.core.domain.chat.ChatMessage
import com.gallbladderz.openkick.core.domain.chat.ChatMode
import com.gallbladderz.openkick.core.player.PlayerExpandedState
import com.gallbladderz.openkick.core.ui.components.ViewerCountBadge
import com.gallbladderz.openkick.features.player.ChatSendState
import com.gallbladderz.openkick.features.player.PlayerUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun PlayerContent(
    streamerName: String,
    uiState: PlayerUiState,
    player: Player?,
    fraction: Float,
    isInPipMode: Boolean,
    isFullscreen: Boolean,
    showControls: Boolean,
    onShowControlsChange: (Boolean) -> Unit,
    playWhenReady: Boolean,
    playbackState: Int,
    isAtLiveEdge: Boolean,
    onSeekToLiveEdge: () -> Unit,
    onPlayerManagerResume: () -> Unit,
    onPlayerManagerPause: () -> Unit,
    onPlayerManagerRelease: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onShowSettings: () -> Unit,
    onBackClick: () -> Unit,
    onUpdateVideoBounds: (android.graphics.Rect) -> Unit,
    dragState: AnchoredDraggableState<PlayerExpandedState>,
    coroutineScope: CoroutineScope,
    chatMessages: List<ChatMessage>,
    onSendMessage: (String, ChatMessage?) -> Unit,
    chatSendState: ChatSendState,
    chatEmoteSets: List<ChatEmoteSet> = emptyList(),
    chatMode: ChatMode = ChatMode(),
    isAuthenticated: Boolean,
    chatSettings: ChatSettings = ChatSettings(),
    onChatSettingsClick: () -> Unit = {},
    onProfileClick: (String) -> Unit = {},
    isChatExpanded: Boolean = false,
    onPlayerInteraction: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current

    var isLandscapeChatOpen by remember { mutableStateOf(true) }

    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            isLandscapeChatOpen = true
        }
    }
    val screenWidth = configuration.screenWidthDp.dp
    val miniPlayerWidth = 120.dp
    val miniPlayerHeight = 64.dp
    val expandedHeight = minOf(screenWidth * (9f / 16f), configuration.screenHeightDp.dp)
    val playerBgColor = androidx.compose.ui.graphics.lerp(
        Color.Black,
        MaterialTheme.colorScheme.surfaceVariant,
        fraction
    )

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
                ) { onShowControlsChange(!showControls) }
        } else {
            Modifier
                .fillMaxWidth()
                .height(lerp(expandedHeight, miniPlayerHeight, fraction))
                .background(playerBgColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onPlayerInteraction()
                    if (dragState.currentValue == PlayerExpandedState.MINI) {
                        coroutineScope.launch { dragState.animateTo(PlayerExpandedState.EXPANDED) }
                    } else {
                        onShowControlsChange(!showControls)
                    }
                }
        }
    ) {
        Row(
            modifier = if (isInPipMode || isFullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .width(lerp(screenWidth, miniPlayerWidth, fraction))
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                when (uiState) {
                    is PlayerUiState.Loading -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    is PlayerUiState.Playing -> {
                        if (player != null) {
                            KickStreamPlayer(
                                player = player,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onGloballyPositioned { coords ->
                                        val bounds = coords.boundsInWindow()
                                        onUpdateVideoBounds(
                                            android.graphics.Rect(
                                                bounds.left.toInt(),
                                                bounds.top.toInt(),
                                                bounds.right.toInt(),
                                                bounds.bottom.toInt()
                                            )
                                        )
                                    }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(
                            visible = showControls && !isInPipMode && fraction < 0.1f,
                            enter = androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.fadeOut(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            PlayerControlsOverlay(
                                playWhenReady = playWhenReady,
                                playbackState = playbackState,
                                isFullscreen = isFullscreen,
                                isAtLiveEdge = isAtLiveEdge,
                                onSeekToLiveEdge = onSeekToLiveEdge,
                                onPlayerManagerResume = onPlayerManagerResume,
                                onPlayerManagerPause = onPlayerManagerPause,
                                onFullscreenToggle = onFullscreenToggle,
                                onShowSettings = onShowSettings,
                                onBackClick = onBackClick,
                                onToggleChat = { isLandscapeChatOpen = !isLandscapeChatOpen }
                            )
                        }

                        if (isChatExpanded) {
                            ViewerCountBadge(
                                viewers = uiState.viewers,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .background(
                                        Color.Black.copy(alpha = 0.55f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }

                    is PlayerUiState.Error -> {
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isFullscreen && isLandscapeChatOpen,
                enter = expandHorizontally(expandFrom = Alignment.Start),
                exit = shrinkHorizontally()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.35f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, dragAmount ->
                                if (dragAmount > 50) {
                                    isLandscapeChatOpen = false
                                    change.consume()
                                }
                            }
                        }
                ) {
                    ChatList(
                        chatMessages = chatMessages,
                        onSendMessage = onSendMessage,
                        chatSendState = chatSendState,
                        emoteSets = chatEmoteSets,
                        chatMode = chatMode,
                        isAuthenticated = isAuthenticated,
                        chatSettings = chatSettings,
                        onSettingsClick = onChatSettingsClick,
                        onProfileClick = onProfileClick
                    )
                }
            }
        }

        if (uiState is PlayerUiState.Playing) {
            MiniPlayerDetails(
                fraction = fraction,
                title = uiState.title,
                streamerName = streamerName,
                playWhenReady = playWhenReady,
                onPlayerManagerPause = onPlayerManagerPause,
                onPlayerManagerResume = onPlayerManagerResume,
                onExpand = {
                    coroutineScope.launch { dragState.animateTo(PlayerExpandedState.EXPANDED) }
                },
                onClose = {
                    onBackClick()
                    coroutineScope.launch { dragState.animateTo(PlayerExpandedState.HIDDEN) }
                },
                miniPlayerWidth = miniPlayerWidth
            )
        }
    }
}
