/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gallbladderz.openkick.R
import kotlin.math.roundToInt

/**
 * Floating chat panel shown on top of the video in fullscreen, as a tall
 * vertical sidebar docked to one side of the screen.
 *
 * Transparency (TikTok-style): the panel defaults to
 * [FloatingChatOpacity.TRANSPARENT] — no fill, no border, so the video
 * shows straight through it. Readability without a solid panel comes
 * from two things, not from covering the video again:
 * - a vertical gradient scrim behind the message list only, fading
 *   from ~0 at the top to ~0.45 black at the bottom (where the newest
 *   messages sit), plus a soft top scrim behind the header;
 * - chat text rendered white with a black drop shadow (see
 *   [LocalChatReadableOverVideo], provided here and consumed by
 *   ChatMessageItem), which stays legible over bright frames in either
 *   theme. The header button cycles Transparent -> Semi -> Solid.
 * Portrait chat is untouched: the local defaults to false there.
 *
 * Drag bug fixed (see FloatingChatGeometry.moveFloatingChat): the header
 * and resize grips previously used
 * `.pointerInput(clampedX, clampedY, clampedW, clampedH)`, so every drag
 * update changed the keys, restarted the pointerInput mid-gesture and
 * cancelled detectDragGestures after the first tiny move — the panel
 * barely moved. All drag handlers here now use `pointerInput(Unit)`
 * (stable key, never restarts) and read the LATEST geometry through
 * rememberUpdatedState, so a drag follows the finger continuously.
 *
 * Interaction contract (so the video gestures keep working):
 * - Dragging works only from the 48dp header bar; resizing from the
 *   left-edge grip or the bottom-right corner grip (or the +/- buttons).
 *   PlayerGestureArea ignores already-consumed pointers, so a drag that
 *   starts inside this panel never changes brightness/volume.
 * - Taps inside the panel are consumed here, so they do not toggle the
 *   player controls. The chat list itself stays scrollable inside the panel.
 * - Everything outside the panel keeps the existing brightness / volume /
 *   seek / tap-to-toggle-controls gestures.
 * - The panel receives the live [playback] value, so messages stay
 *   synchronised with the video while playing and while seeking.
 */
@Composable
internal fun FloatingChatOverlay(
    playback: RecordedPlayback,
    chat: @Composable (RecordedPlayback, Modifier) -> Unit,
    screenWidthDp: Float,
    screenHeightDp: Float,
    offsetXDp: Float,
    offsetYDp: Float,
    widthDp: Float,
    heightDp: Float,
    opacity: FloatingChatOpacity,
    onOpacityChange: (FloatingChatOpacity) -> Unit,
    onGeometryChange: (FloatingChatGeometry) -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val (clampedX, clampedY) = clampFloatingChatOffset(
        offsetXDp, offsetYDp, widthDp, heightDp, screenWidthDp, screenHeightDp
    )
    val (clampedW, clampedH) = clampFloatingChatSize(widthDp, heightDp, screenWidthDp, screenHeightDp)
    // Latest values for the gesture blocks. pointerInput(Unit) never
    // restarts, so these State objects are the only way the running
    // gesture sees geometry changes made by its own previous updates.
    val latestGeometry by rememberUpdatedState(FloatingChatGeometry(clampedX, clampedY, clampedW, clampedH))
    val latestOnGeometryChange by rememberUpdatedState(onGeometryChange)
    val latestScreen by rememberUpdatedState(screenWidthDp to screenHeightDp)
    val latestOpacity by rememberUpdatedState(opacity)
    val latestOnOpacityChange by rememberUpdatedState(onOpacityChange)
    val offsetPx = with(density) { Offset(clampedX.dp.toPx(), clampedY.dp.toPx()) }

    val panelAlpha = floatingChatPanelBackgroundAlpha(opacity)
    val borderAlpha = floatingChatPanelBorderAlpha(opacity)
    val overVideo = floatingChatReadableOverVideo(opacity)
    val headerShadow = Shadow(color = Color.Black, offset = Offset(1f, 1f), blurRadius = 4f)

    Box(
        modifier
            .offset { IntOffset(offsetPx.x.roundToInt(), offsetPx.y.roundToInt()) }
            .size(clampedW.dp, clampedH.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (panelAlpha > 0f) Modifier.background(Color.Black.copy(alpha = panelAlpha)) else Modifier)
            .then(
                if (borderAlpha > 0f) Modifier.border(
                    1.dp, Color.White.copy(alpha = borderAlpha), RoundedCornerShape(12.dp)
                ) else Modifier
            )
            // Consume taps so they do not toggle the player controls.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header / drag handle: the whole 48dp bar moves the panel.
            // pointerInput(Unit): stable key — must never restart mid-drag.
            // Transparent modes have no solid bar: only a soft gradient
            // scrim at the very top keeps the white icons legible.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .then(
                        if (opacity == FloatingChatOpacity.SOLID) {
                            Modifier.background(Color.Black.copy(alpha = 0.45f))
                        } else {
                            Modifier.background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = floatingChatHeaderScrimTopAlpha(opacity)),
                                        Color.Black.copy(alpha = 0f)
                                    )
                                )
                            )
                        }
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                val (sw, sh) = latestScreen
                                latestOnGeometryChange(
                                    snapFloatingChatToEdge(latestGeometry, sw, sh)
                                )
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val (sw, sh) = latestScreen
                                val delta = with(density) {
                                    Offset(dragAmount.x.toDp().value, dragAmount.y.toDp().value)
                                }
                                latestOnGeometryChange(
                                    moveFloatingChat(latestGeometry, delta.x, delta.y, sw, sh)
                                )
                            }
                        )
                    }
                    .padding(start = 8.dp, end = 2.dp)
            ) {
                Icon(
                    Icons.Filled.DragHandle,
                    contentDescription = stringResource(R.string.floating_chat_drag),
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    stringResource(R.string.chat_replay),
                    style = MaterialTheme.typography.titleSmall.copy(
                        shadow = if (overVideo) headerShadow else null
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp)
                )
                IconButton(
                    onClick = {
                        val (sw, sh) = latestScreen
                        latestOnGeometryChange(
                            dockFloatingChat(latestGeometry, dockLeft = true, sw, sh)
                        )
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.floating_chat_dock_left),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        val (sw, sh) = latestScreen
                        latestOnGeometryChange(
                            dockFloatingChat(latestGeometry, dockLeft = false, sw, sh)
                        )
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.floating_chat_dock_right),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Opacity cycle: Transparent -> Semi -> Solid -> Transparent.
                // The icon fades with the mode so the current state is visible.
                IconButton(
                    onClick = { latestOnOpacityChange(latestOpacity.next()) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Opacity,
                        contentDescription = stringResource(
                            when (opacity) {
                                FloatingChatOpacity.TRANSPARENT -> R.string.floating_chat_opacity_transparent
                                FloatingChatOpacity.SEMI -> R.string.floating_chat_opacity_semi
                                FloatingChatOpacity.SOLID -> R.string.floating_chat_opacity_solid
                            }
                        ),
                        tint = Color.White.copy(
                            alpha = when (opacity) {
                                FloatingChatOpacity.TRANSPARENT -> 0.55f
                                FloatingChatOpacity.SEMI -> 0.8f
                                FloatingChatOpacity.SOLID -> 1f
                            }
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        val (sw, sh) = latestScreen
                        val current = latestGeometry
                        val (w, h) = stepFloatingChatSize(
                            current.widthDp, current.heightDp, growing = false, sw, sh
                        )
                        val (x, y) = clampFloatingChatOffset(
                            current.offsetXDp, current.offsetYDp, w, h, sw, sh
                        )
                        latestOnGeometryChange(FloatingChatGeometry(x, y, w, h))
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = stringResource(R.string.floating_chat_smaller),
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = {
                        val (sw, sh) = latestScreen
                        val current = latestGeometry
                        val (w, h) = stepFloatingChatSize(
                            current.widthDp, current.heightDp, growing = true, sw, sh
                        )
                        val (x, y) = clampFloatingChatOffset(
                            current.offsetXDp, current.offsetYDp, w, h, sw, sh
                        )
                        latestOnGeometryChange(FloatingChatGeometry(x, y, w, h))
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.floating_chat_larger),
                        tint = Color.White
                    )
                }
                IconButton(onClick = onHide, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.floating_chat_hide),
                        tint = Color.White
                    )
                }
            }
            // Message area: the only scrim in transparent modes — a
            // vertical gradient, 0 at the top (video fully visible) to
            // ~0.45 black at the bottom, where the newest messages sit.
            // Solid mode needs no scrim: the panel fill already darkens it.
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = floatingChatScrimTopAlpha(opacity)),
                                Color.Black.copy(alpha = floatingChatScrimBottomAlpha(opacity))
                            )
                        )
                    )
            ) {
                CompositionLocalProvider(LocalChatReadableOverVideo provides overVideo) {
                    chat(playback, Modifier.fillMaxSize())
                }
            }
        }
        // Video-facing edge resize grip: on the left edge when the panel
        // is on the right side of the screen, on the right edge when it is
        // docked left. Dragging it horizontally changes the sidebar width.
        // pointerInput(Unit): stable key — must never restart mid-drag.
        // Whether the grip is on the left or the right is decided from the
        // latest geometry, so it flips sides as soon as the panel docks.
        val gripOnLeft = latestGeometry.offsetXDp > latestScreen.first / 2f
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(if (gripOnLeft) Alignment.CenterStart else Alignment.CenterEnd)
                .width(12.dp)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val (sw, sh) = latestScreen
                        val deltaX = with(density) { dragAmount.x.toDp().value }
                        val onLeft = latestGeometry.offsetXDp > sw / 2f
                        latestOnGeometryChange(
                            if (onLeft) resizeFloatingChatFromLeftEdge(latestGeometry, deltaX, sw, sh)
                            else resizeFloatingChatFromRightEdge(latestGeometry, deltaX, sw, sh)
                        )
                    }
                }
        ) {
            Canvas(Modifier.size(4.dp, 48.dp)) {
                val stroke = 2.dp.toPx()
                val color = Color.White.copy(alpha = 0.7f)
                // Two vertical lines, like a sidebar resize grip.
                drawLine(color, Offset(size.width * 0.25f, 0f), Offset(size.width * 0.25f, size.height), strokeWidth = stroke)
                drawLine(color, Offset(size.width * 0.75f, 0f), Offset(size.width * 0.75f, size.height), strokeWidth = stroke)
            }
        }
        // Bottom-right resize grip. Only this corner resizes both axes.
        // pointerInput(Unit): stable key — must never restart mid-drag.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(36.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val (sw, sh) = latestScreen
                        val current = latestGeometry
                        val delta = with(density) {
                            Offset(dragAmount.x.toDp().value, dragAmount.y.toDp().value)
                        }
                        val (w, h) = resizeFloatingChat(
                            current.widthDp, current.heightDp, delta.x, delta.y, sw, sh
                        )
                        val (x, y) = clampFloatingChatOffset(
                            current.offsetXDp, current.offsetYDp, w, h, sw, sh
                        )
                        latestOnGeometryChange(FloatingChatGeometry(x, y, w, h))
                    }
                }
        ) {
            Canvas(Modifier.size(20.dp)) {
                val stroke = 2.dp.toPx()
                val color = Color.White.copy(alpha = 0.7f)
                // Two diagonal lines in the corner, like a resize grip.
                drawLine(color, Offset(size.width, size.height * 0.35f), Offset(size.width * 0.35f, size.height), strokeWidth = stroke)
                drawLine(color, Offset(size.width, size.height * 0.7f), Offset(size.width * 0.7f, size.height), strokeWidth = stroke)
            }
        }
    }
}

/** Small button shown in fullscreen when the floating chat panel is hidden. */
@Composable
internal fun FloatingChatReopenButton(
    onShow: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onShow,
        modifier = modifier
            .size(48.dp)
            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
    ) {
        Icon(
            Icons.Filled.ChatBubble,
            contentDescription = stringResource(R.string.floating_chat_show),
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
