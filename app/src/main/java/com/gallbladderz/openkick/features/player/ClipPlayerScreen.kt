@file:Suppress("DEPRECATION")
@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.gallbladderz.openkick.features.player
import com.gallbladderz.openkick.features.player.components.ClipInfoPanel

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.components.KickAvatar
import com.gallbladderz.openkick.core.ui.utils.findActivity
import com.gallbladderz.openkick.features.player.components.CustomPlayerControls
import com.gallbladderz.openkick.features.player.components.KickStreamPlayer
import com.gallbladderz.openkick.features.player.components.QualitySelectionSheet
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun ClipPlayerRoute(
    clipId: String,
    onBackClick: () -> Unit,
    onStreamerClick: (String) -> Unit = {},
    viewModel: PlayerViewModel = koinViewModel(),
    clipPlayerViewModel: ClipPlayerViewModel = koinViewModel()
) {
    LaunchedEffect(clipId) {
        clipPlayerViewModel.loadClip(clipId)
    }

    val activeClip by clipPlayerViewModel.activeClip.collectAsStateWithLifecycle()
    val fetchedAvatarUrl by clipPlayerViewModel.avatarUrl.collectAsStateWithLifecycle()

    val safeStreamerName = activeClip?.streamerName ?: ""
    val isFollowed by viewModel.isStreamerFollowed(safeStreamerName)
        .collectAsStateWithLifecycle(initialValue = false)

    if (activeClip != null) {
        val clip = activeClip!!
        ClipPlayerScreen(
            videoUrl = clip.videoUrl,
            title = clip.title,
            streamerName = clip.streamerName,
            streamerAvatarUrl = clip.streamerAvatarUrl,
            views = clip.views,
            durationFormatted = clip.durationFormatted,
            isFollowed = isFollowed,
            fetchedAvatarUrl = fetchedAvatarUrl,
            onToggleFollow = { viewModel.toggleFollow(clip.streamerName, isFollowed) },
            onLoadAvatar = { clipPlayerViewModel.loadAvatar(clip.streamerName) },
            onBackClick = onBackClick,
            onStreamerClick = onStreamerClick
        )
    } else {
        // Fallback or loading state could go here. For now, empty Box or immediate back navigation.
        Box(modifier = Modifier.fillMaxSize()) {
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
    val configuration = LocalConfiguration.current

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

    var isFullscreen by remember { mutableStateOf(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var availableQualities by remember { mutableStateOf<List<VideoQuality>>(emptyList()) }
    var selectedQuality by remember { mutableStateOf<VideoQuality?>(null) }

    fun setQuality(quality: VideoQuality) {
        selectedQuality = quality
        val builder = exoPlayer.trackSelectionParameters.buildUpon()
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

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
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

            override fun onTracksChanged(tracks: Tracks) {
                val parsedQualities = mutableListOf<VideoQuality>()
                for (group in tracks.groups) {
                    if (group.type == C.TRACK_TYPE_VIDEO) {
                        for (i in 0 until group.length) {
                            val format = group.getTrackFormat(i)
                            if (format.height != Format.NO_VALUE) {
                                val fps = if (format.frameRate > 0) format.frameRate.toInt().toString() else ""
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
                    VideoQuality(context.getString(R.string.audio_only), null, null, isAudioOnly = true)
                ) + sortedQualities
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

    val rootModifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .let { if (!isFullscreen) it.statusBarsPadding() else it }

    Column(modifier = rootModifier) {
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

        if (!isFullscreen) {
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

