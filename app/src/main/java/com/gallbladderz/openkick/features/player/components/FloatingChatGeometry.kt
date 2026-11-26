/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import androidx.compose.ui.geometry.Offset

/** Minimum floating chat panel size, in dp. */
const val FLOATING_CHAT_MIN_WIDTH_DP = 240f
const val FLOATING_CHAT_MIN_HEIGHT_DP = 140f

/** Maximum panel width as a fraction of the fullscreen width (vertical sidebar). */
const val FLOATING_CHAT_MAX_WIDTH_FRACTION = 0.55f

/** Size step for the +/- buttons in the floating panel header, in dp. */
const val FLOATING_CHAT_SIZE_STEP_DP = 40f

/**
 * Default floating panel size as a fraction of the fullscreen size.
 * The panel is a tall vertical sidebar: ~30% wide, ~92% tall. It is
 * deliberately narrow in transparent (TikTok-style) mode so as little
 * of the video as possible is covered.
 */
const val FLOATING_CHAT_DEFAULT_WIDTH_FRACTION = 0.30f
const val FLOATING_CHAT_DEFAULT_HEIGHT_FRACTION = 0.92f

/** Default panel width bounds, in dp (applied after the fraction). */
const val FLOATING_CHAT_DEFAULT_MIN_WIDTH_DP = 260f
const val FLOATING_CHAT_DEFAULT_MAX_WIDTH_DP = 420f

/** Margin between the default panel position and the screen edge, in dp. */
const val FLOATING_CHAT_DEFAULT_MARGIN_DP = 8f

/** Distance from a screen edge, in dp, within which a released drag snaps/docks. */
const val FLOATING_CHAT_SNAP_THRESHOLD_DP = 48f

/** Floating chat geometry, all values in dp. */
data class FloatingChatGeometry(
    val offsetXDp: Float,
    val offsetYDp: Float,
    val widthDp: Float,
    val heightDp: Float
)

/**
 * Default geometry: a tall vertical sidebar docked to the right side,
 * vertically centred with a small top/bottom margin.
 * On an 800x360dp landscape screen this is 260x331.2dp at (532, 14.4).
 */
fun defaultFloatingChatGeometry(screenWidthDp: Float, screenHeightDp: Float): FloatingChatGeometry {
    val width = (screenWidthDp * FLOATING_CHAT_DEFAULT_WIDTH_FRACTION)
        .coerceIn(FLOATING_CHAT_DEFAULT_MIN_WIDTH_DP, FLOATING_CHAT_DEFAULT_MAX_WIDTH_DP)
        .coerceAtMost((screenWidthDp - 2f * FLOATING_CHAT_DEFAULT_MARGIN_DP).coerceAtLeast(FLOATING_CHAT_MIN_WIDTH_DP))
    val height = (screenHeightDp * FLOATING_CHAT_DEFAULT_HEIGHT_FRACTION)
        .coerceIn(FLOATING_CHAT_MIN_HEIGHT_DP, screenHeightDp)
    return FloatingChatGeometry(
        offsetXDp = (screenWidthDp - width - FLOATING_CHAT_DEFAULT_MARGIN_DP).coerceAtLeast(0f),
        offsetYDp = ((screenHeightDp - height) / 2f).coerceAtLeast(0f),
        widthDp = width,
        heightDp = height
    )
}

/** Clamp a panel offset so the whole panel stays on screen. */
fun clampFloatingChatOffset(
    offsetXDp: Float,
    offsetYDp: Float,
    widthDp: Float,
    heightDp: Float,
    screenWidthDp: Float,
    screenHeightDp: Float
): Pair<Float, Float> = Pair(
    offsetXDp.coerceIn(0f, (screenWidthDp - widthDp).coerceAtLeast(0f)),
    offsetYDp.coerceIn(0f, (screenHeightDp - heightDp).coerceAtLeast(0f))
)

/** Maximum panel width for the current screen (never below the minimum). */
fun floatingChatMaxWidthDp(screenWidthDp: Float): Float =
    maxOf(FLOATING_CHAT_MIN_WIDTH_DP, minOf(screenWidthDp, screenWidthDp * FLOATING_CHAT_MAX_WIDTH_FRACTION))

/** Clamp a panel size: width [min, 55% of screen], height [min, screen height]. */
fun clampFloatingChatSize(
    widthDp: Float,
    heightDp: Float,
    screenWidthDp: Float,
    screenHeightDp: Float
): Pair<Float, Float> = Pair(
    widthDp.coerceIn(FLOATING_CHAT_MIN_WIDTH_DP, floatingChatMaxWidthDp(screenWidthDp)),
    heightDp.coerceIn(FLOATING_CHAT_MIN_HEIGHT_DP, screenHeightDp.coerceAtLeast(FLOATING_CHAT_MIN_HEIGHT_DP))
)

/**
 * Move a panel by one drag delta (in dp), clamped to the screen.
 * Pure function: applying a series of deltas one at a time to the
 * *latest* geometry is exactly what a continuous drag must do — this is
 * the regression guard for the old bug, where the drag handler restarted
 * on every geometry change and re-applied each delta to a stale geometry.
 */
fun moveFloatingChat(
    geometry: FloatingChatGeometry,
    deltaXDp: Float,
    deltaYDp: Float,
    screenWidthDp: Float,
    screenHeightDp: Float
): FloatingChatGeometry {
    val (x, y) = clampFloatingChatOffset(
        geometry.offsetXDp + deltaXDp, geometry.offsetYDp + deltaYDp,
        geometry.widthDp, geometry.heightDp, screenWidthDp, screenHeightDp
    )
    return geometry.copy(offsetXDp = x, offsetYDp = y)
}

/** Apply a whole drag (a series of deltas) to a geometry, delta by delta. */
fun applyDragDeltas(
    geometry: FloatingChatGeometry,
    deltas: List<Offset>,
    screenWidthDp: Float,
    screenHeightDp: Float
): FloatingChatGeometry = deltas.fold(geometry) { current, delta ->
    moveFloatingChat(current, delta.x, delta.y, screenWidthDp, screenHeightDp)
}

/** Dock the panel to one side as a full-height vertical sidebar. */
fun dockFloatingChat(
    geometry: FloatingChatGeometry,
    dockLeft: Boolean,
    screenWidthDp: Float,
    screenHeightDp: Float
): FloatingChatGeometry {
    val (w, h) = clampFloatingChatSize(geometry.widthDp, screenHeightDp * FLOATING_CHAT_DEFAULT_HEIGHT_FRACTION, screenWidthDp, screenHeightDp)
    val y = ((screenHeightDp - h) / 2f).coerceAtLeast(0f)
    val x = if (dockLeft) FLOATING_CHAT_DEFAULT_MARGIN_DP
    else (screenWidthDp - w - FLOATING_CHAT_DEFAULT_MARGIN_DP).coerceAtLeast(0f)
    return FloatingChatGeometry(offsetXDp = x, offsetYDp = y, widthDp = w, heightDp = h)
}

/**
 * Snap to an edge on drag release: within [thresholdDp] of the left edge
 * docks left, within it of the right edge docks right, otherwise unchanged.
 */
fun snapFloatingChatToEdge(
    geometry: FloatingChatGeometry,
    screenWidthDp: Float,
    screenHeightDp: Float,
    thresholdDp: Float = FLOATING_CHAT_SNAP_THRESHOLD_DP
): FloatingChatGeometry = when {
    geometry.offsetXDp <= thresholdDp -> dockFloatingChat(geometry, dockLeft = true, screenWidthDp, screenHeightDp)
    geometry.offsetXDp + geometry.widthDp >= screenWidthDp - thresholdDp ->
        dockFloatingChat(geometry, dockLeft = false, screenWidthDp, screenHeightDp)
    else -> geometry
}

/**
 * Resize by dragging the panel's left edge: dragging left grows the panel
 * (and moves its left edge left), dragging right shrinks it. The right
 * edge stays fixed, so the offset moves by the actual width change.
 */
fun resizeFloatingChatFromLeftEdge(
    geometry: FloatingChatGeometry,
    deltaXDp: Float,
    screenWidthDp: Float,
    screenHeightDp: Float
): FloatingChatGeometry {
    val (clampedW, _) = clampFloatingChatSize(
        geometry.widthDp - deltaXDp, geometry.heightDp, screenWidthDp, screenHeightDp
    )
    val actualChange = clampedW - geometry.widthDp
    val (x, y) = clampFloatingChatOffset(
        geometry.offsetXDp - actualChange, geometry.offsetYDp,
        clampedW, geometry.heightDp, screenWidthDp, screenHeightDp
    )
    return geometry.copy(offsetXDp = x, offsetYDp = y, widthDp = clampedW)
}

/**
 * Resize by dragging the panel's right edge: dragging right grows the
 * panel, dragging left shrinks it. The left edge stays fixed.
 */
fun resizeFloatingChatFromRightEdge(
    geometry: FloatingChatGeometry,
    deltaXDp: Float,
    screenWidthDp: Float,
    screenHeightDp: Float
): FloatingChatGeometry {
    val (clampedW, _) = clampFloatingChatSize(
        geometry.widthDp + deltaXDp, geometry.heightDp, screenWidthDp, screenHeightDp
    )
    return geometry.copy(widthDp = clampedW)
}

/** Apply a resize drag delta (in dp) to a panel size, clamped to the screen. */
fun resizeFloatingChat(
    widthDp: Float,
    heightDp: Float,
    deltaXDp: Float,
    deltaYDp: Float,
    screenWidthDp: Float,
    screenHeightDp: Float
): Pair<Float, Float> = clampFloatingChatSize(
    widthDp + deltaXDp, heightDp + deltaYDp, screenWidthDp, screenHeightDp
)

/** Apply a +/- size step to a panel size, clamped to the screen. */
fun stepFloatingChatSize(
    widthDp: Float,
    heightDp: Float,
    growing: Boolean,
    screenWidthDp: Float,
    screenHeightDp: Float
): Pair<Float, Float> {
    val delta = if (growing) FLOATING_CHAT_SIZE_STEP_DP else -FLOATING_CHAT_SIZE_STEP_DP
    return clampFloatingChatSize(widthDp + delta, heightDp + delta, screenWidthDp, screenHeightDp)
}

/**
 * Fullscreen chat opacity mode (TikTok-style transparency).
 *
 * - [TRANSPARENT] (default): no panel fill and no border — the video
 *   shows straight through. Readability comes only from the gradient
 *   scrim behind the message list (fading to 0 at the top) plus white
 *   text with a black drop shadow.
 * - [SEMI]: a light 0.35-alpha panel fill plus a stronger scrim.
 * - [SOLID]: the old opaque look — 0.75-alpha panel fill, no scrim,
 *   theme text colours (no shadow needed).
 */
enum class FloatingChatOpacity {
    TRANSPARENT,
    SEMI,
    SOLID
}

/** Cycle Transparent -> Semi -> Solid -> Transparent. */
fun FloatingChatOpacity.next(): FloatingChatOpacity = when (this) {
    FloatingChatOpacity.TRANSPARENT -> FloatingChatOpacity.SEMI
    FloatingChatOpacity.SEMI -> FloatingChatOpacity.SOLID
    FloatingChatOpacity.SOLID -> FloatingChatOpacity.TRANSPARENT
}

/** Alpha of the panel's flat black fill (0 = fully transparent). */
fun floatingChatPanelBackgroundAlpha(mode: FloatingChatOpacity): Float = when (mode) {
    FloatingChatOpacity.TRANSPARENT -> 0f
    FloatingChatOpacity.SEMI -> 0.35f
    FloatingChatOpacity.SOLID -> 0.75f
}

/** Alpha of the panel's white border (0 = no border). */
fun floatingChatPanelBorderAlpha(mode: FloatingChatOpacity): Float = when (mode) {
    FloatingChatOpacity.TRANSPARENT -> 0f
    FloatingChatOpacity.SEMI -> 0.15f
    FloatingChatOpacity.SOLID -> 0.25f
}

/**
 * Alpha of the gradient scrim behind the message list, at its top edge.
 * Transparent mode fades to 0 at the top so the video stays fully
 * visible there; the bottom (see below) carries the readability.
 */
fun floatingChatScrimTopAlpha(mode: FloatingChatOpacity): Float = when (mode) {
    FloatingChatOpacity.TRANSPARENT -> 0f
    FloatingChatOpacity.SEMI -> 0.10f
    FloatingChatOpacity.SOLID -> 0f
}

/** Alpha of the gradient scrim behind the message list, at its bottom edge. */
fun floatingChatScrimBottomAlpha(mode: FloatingChatOpacity): Float = when (mode) {
    FloatingChatOpacity.TRANSPARENT -> 0.45f
    FloatingChatOpacity.SEMI -> 0.50f
    FloatingChatOpacity.SOLID -> 0f
}

/**
 * Alpha at the top of the header's own gradient (fades to 0 at the
 * header's bottom edge). The header has no solid bar in transparent
 * modes — this soft top scrim is the only thing keeping the white
 * header icons legible over a bright frame.
 */
fun floatingChatHeaderScrimTopAlpha(mode: FloatingChatOpacity): Float = when (mode) {
    FloatingChatOpacity.TRANSPARENT -> 0.30f
    FloatingChatOpacity.SEMI -> 0.25f
    FloatingChatOpacity.SOLID -> 0f
}

/**
 * Whether chat text must be rendered white with a black drop shadow so
 * it stays legible directly over video. True whenever the video can
 * show through the panel; false only in Solid mode, where the opaque
 * dark panel makes the theme colours readable on their own.
 */
fun floatingChatReadableOverVideo(mode: FloatingChatOpacity): Boolean =
    mode != FloatingChatOpacity.SOLID
