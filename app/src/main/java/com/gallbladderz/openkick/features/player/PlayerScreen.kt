@file:Suppress("DEPRECATION")

package com.gallbladderz.openkick.features.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.components.KickAvatar
import com.gallbladderz.openkick.core.ui.components.ViewerCountBadge
import com.gallbladderz.openkick.features.player.models.ChannelLink
import com.gallbladderz.openkick.features.player.models.ChatMessage

import com.gallbladderz.openkick.features.player.components.CustomPlayerControls
import com.gallbladderz.openkick.features.player.components.KickStreamPlayer
import com.gallbladderz.openkick.features.player.components.StreamerInfoCard
import com.gallbladderz.openkick.features.player.components.ChatList
import com.gallbladderz.openkick.features.player.components.InfoTabContent
import com.gallbladderz.openkick.features.player.components.QualitySelectionSheet
import com.gallbladderz.openkick.core.ui.utils.FullscreenHandler
import com.gallbladderz.openkick.core.ui.utils.findActivity
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    streamerName: String,
    onBackClick: () -> Unit,
    onAvatarClick: (String) -> Unit = {},
    viewModel: PlayerViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val channelLinks by viewModel.channelLinks.collectAsStateWithLifecycle()
    val isFollowed by viewModel.isStreamerFollowed(streamerName).collectAsStateWithLifecycle(initialValue = false)

    var showControls by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }



    var isFullscreen by remember {
        mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    val playWhenReady by viewModel.playerManager.playWhenReady.collectAsStateWithLifecycle()
    val playbackState by viewModel.playerManager.playbackState.collectAsStateWithLifecycle()
    val availableQualities by viewModel.availableQualities.collectAsStateWithLifecycle()
    val selectedQuality by viewModel.selectedQuality.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

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
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
                Lifecycle.Event.ON_PAUSE -> viewModel.pause()
                Lifecycle.Event.ON_RESUME -> viewModel.play()
                Lifecycle.Event.ON_DESTROY -> viewModel.playerManager.release()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(streamerName) {
        viewModel.loadStreamInfo(streamerName)
        viewModel.loadChannelLinks(streamerName)
    }

    val rootModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .let { if (!isFullscreen) it.statusBarsPadding() else it }

    Column(modifier = rootModifier) {
        Box(
            modifier = if (isFullscreen) {
                Modifier.fillMaxSize().background(Color.Black).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { showControls = !showControls }
            } else {
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black).clickable(
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
                    KickStreamPlayer(
                        player = viewModel.playerManager.player,
                        modifier = Modifier.fillMaxSize()
                    )

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showControls,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CustomPlayerControls(
                                playWhenReady = playWhenReady,
                                playbackState = playbackState,
                                isFullscreen = isFullscreen,
                                onPlayPause = {
                                    if (playWhenReady) viewModel.playerManager.pause()
                                    else viewModel.playerManager.resume()
                                },
                                onFullscreen = { isFullscreen = !isFullscreen },
                                onSettings = { showSettingsSheet = true },
                                modifier = Modifier.fillMaxSize()
                            )

                            IconButton(
                                onClick = { if (isFullscreen) isFullscreen = false else onBackClick() },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
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

        if (!isFullscreen) {
            if (state is PlayerUiState.Playing) {
                val playingState = state as PlayerUiState.Playing
                StreamerInfoCard(
                    streamerName = streamerName,
                    title = playingState.title,
                    avatarUrl = playingState.avatarUrl,
                    viewers = playingState.viewers,
                    isFollowed = isFollowed,
                    onToggleFollow = { viewModel.toggleFollow(streamerName, isFollowed) },
                    onAvatarClick = { onAvatarClick(streamerName) }
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
                    1 -> InfoTabContent(links = channelLinks)
                }
            }
        }
    }

    if (showSettingsSheet) {
        QualitySelectionSheet(
            qualities = availableQualities,
            selectedQuality = selectedQuality,
            onQualitySelect = { quality ->
                viewModel.setVideoQuality(quality)
                showSettingsSheet = false
            },
            onDismiss = { showSettingsSheet = false }
        )
    }
}

