/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Gesture layout adapted from QuinKick (https://github.com/kaws3664/QuinKick),
 * by OpenKick Translation and kaws3664, licensed under GPL-3.0-or-later.
 */

package com.gallbladderz.openkick.features.player.components

import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.provider.Settings
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.utils.findActivity
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.roundToInt

internal enum class PlayerGestureZone { Brightness, Fullscreen, Volume }

internal fun playerGestureZone(fraction: Float, fullscreen: Boolean): PlayerGestureZone? = when {
    fraction < 1f / 3f -> PlayerGestureZone.Brightness.takeIf { fullscreen }
    fraction > 2f / 3f -> PlayerGestureZone.Volume.takeIf { fullscreen }
    else -> PlayerGestureZone.Fullscreen
}

internal fun volumeBoostMillibels(percent: Float): Int =
    (2_000 * log10(percent.coerceIn(100f, 200f) / 100f)).roundToInt()

private data class GestureFeedback(val label: Int, val percent: Int)

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun PlayerGestureArea(
    player: Player?,
    fullscreen: Boolean,
    enabled: Boolean,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val window = context.findActivity()?.window
    val audio = remember(context) { context.getSystemService(AudioManager::class.java) }
    val boost = remember { PlayerVolumeBoost() }
    val toggleFullscreen by rememberUpdatedState(onToggleFullscreen)
    val gesturesEnabled by rememberUpdatedState(enabled)
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val system = WindowInsets.systemGestures
    val edge = with(density) { 24.dp.toPx() }
    val controls = with(density) { 52.dp.toPx() }
    val left = maxOf(edge, system.getLeft(density, direction).toFloat())
    val right = maxOf(edge, system.getRight(density, direction).toFloat())
    val top = maxOf(controls, system.getTop(density).toFloat())
    val bottom = maxOf(controls, system.getBottom(density).toFloat())
    var adjusting by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<GestureFeedback?>(null) }

    DisposableEffect(window, fullscreen, enabled) {
        val previousBrightness = window?.attributes?.screenBrightness
        onDispose {
            if (fullscreen && enabled && previousBrightness != null) window.setPlayerBrightness(previousBrightness)
        }
    }
    DisposableEffect(player, fullscreen, enabled) {
        val listener = object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                boost.attach(audioSessionId)
            }
        }
        if (fullscreen && enabled && player != null) {
            boost.attach(player.audioSessionId)
            player.addListener(listener)
        }
        onDispose {
            if (fullscreen && enabled && player != null) player.removeListener(listener)
            boost.close()
        }
    }
    LaunchedEffect(adjusting, feedback) {
        if (!adjusting && feedback != null) {
            delay(900)
            feedback = null
        }
    }

    val gestures = Modifier.pointerInput(
        fullscreen, window, audio, left, right, top, bottom
    ) {
        val fullscreenThreshold = 48.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown()
            if (!gesturesEnabled) return@awaitEachGesture
            val (x, y) = down.position
            if (x < left || x >= size.width - right || y < top || y >= size.height - bottom) {
                return@awaitEachGesture
            }
            val zone = playerGestureZone(x / size.width, fullscreen) ?: return@awaitEachGesture
            val start = when (zone) {
                PlayerGestureZone.Brightness -> {
                    if (window == null) return@awaitEachGesture
                    window.attributes.screenBrightness.takeIf { it >= 0 }
                        ?: (Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f)
                            .coerceIn(0f, 1f)
                }
                PlayerGestureZone.Volume -> {
                    if (audio == null || audio.isVolumeFixed) return@awaitEachGesture
                    audio.musicVolumePercent(boost)
                }
                PlayerGestureZone.Fullscreen -> 0f
            }
            var dragging = false
            var vertical = 0f
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val pointer = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                    if (pointer.isConsumed || event.changes.count { it.pressed } > 1) return@awaitEachGesture
                    if (!pointer.pressed) break
                    val delta = pointer.position - down.position
                    if (!dragging) {
                        val slop = viewConfiguration.touchSlop
                        if (abs(delta.x) >= abs(delta.y) && abs(delta.x) > slop) return@awaitEachGesture
                        if (abs(delta.y) <= slop) continue
                        if (zone == PlayerGestureZone.Fullscreen && (delta.y > 0) != fullscreen) {
                            return@awaitEachGesture
                        }
                        dragging = true
                        adjusting = true
                    }
                    pointer.consume()
                    vertical = delta.y
                    when (zone) {
                        PlayerGestureZone.Brightness -> {
                            val value = (start - vertical / size.height).coerceIn(0.01f, 1f)
                            window?.setPlayerBrightness(value)
                            feedback = GestureFeedback(R.string.gesture_brightness_percent, (value * 100).roundToInt())
                        }
                        PlayerGestureZone.Volume -> {
                            val value = (start - vertical / size.height * 200f).coerceIn(0f, 200f)
                            val actual = audio?.setMusicVolumePercent(value, boost) ?: 0
                            feedback = GestureFeedback(R.string.gesture_volume_percent, actual)
                        }
                        PlayerGestureZone.Fullscreen -> Unit
                    }
                }
                if (zone == PlayerGestureZone.Fullscreen &&
                    (if (fullscreen) vertical >= fullscreenThreshold else vertical <= -fullscreenThreshold)
                ) toggleFullscreen()
            } finally {
                if (dragging) adjusting = false
            }
        }
    }
    Box(modifier.then(gestures)) {
        content()
        feedback?.takeIf { fullscreen && enabled }?.let {
            Text(
                text = stringResource(it.label, it.percent),
                modifier = Modifier.align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

private fun Window.setPlayerBrightness(value: Float) {
    attributes = attributes.apply { screenBrightness = value }
}

private fun AudioManager.musicVolumePercent(boost: PlayerVolumeBoost): Float {
    val limit = getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    if (limit <= 0) return 0f
    val current = getStreamVolume(AudioManager.STREAM_MUSIC)
    if (current == limit) return boost.percent
    boost.setPercent(100f)
    return 100f * current / limit
}

private fun AudioManager.setMusicVolumePercent(percent: Float, boost: PlayerVolumeBoost): Int {
    val stream = AudioManager.STREAM_MUSIC
    val max = getStreamMaxVolume(stream)
    val target = (percent.coerceAtMost(100f) / 100f * max).roundToInt()
    if (getStreamVolume(stream) != target) {
        try {
            setStreamVolume(stream, target, 0)
        } catch (_: SecurityException) {
            return musicVolumePercent(boost).roundToInt()
        }
    }
    val current = getStreamVolume(stream)
    boost.setPercent(if (max > 0 && current == max) percent else 100f)
    return musicVolumePercent(boost).roundToInt()
}

private class PlayerVolumeBoost {
    private var sessionId = 0
    private var effect: LoudnessEnhancer? = null
    private var unavailable = false
    var percent = 100f
        private set

    fun attach(id: Int) {
        if (sessionId == id) return
        val previous = percent
        close()
        sessionId = id
        setPercent(previous)
    }

    fun setPercent(value: Float) {
        if (value <= 100f) {
            effect?.release()
            effect = null
            percent = 100f
            return
        }
        if (sessionId <= 0 || unavailable) return
        try {
            val enhancer = effect ?: LoudnessEnhancer(sessionId).also { effect = it }
            enhancer.setTargetGain(volumeBoostMillibels(value))
            check(enhancer.enabled || enhancer.setEnabled(true) == AudioEffect.SUCCESS)
            percent = value
        } catch (_: RuntimeException) {
            effect?.release()
            effect = null
            unavailable = true
            percent = 100f
        }
    }

    fun close() {
        setPercent(100f)
        sessionId = 0
        unavailable = false
    }
}
