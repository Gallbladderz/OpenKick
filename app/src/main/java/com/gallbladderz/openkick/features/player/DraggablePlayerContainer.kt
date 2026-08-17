/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import com.gallbladderz.openkick.core.ui.utils.findActivity
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggablePlayerContainer(
    globalPlayerController: GlobalPlayerController,
    bottomOffsetPx: Float,
    modifier: Modifier = Modifier,
    content: @Composable (AnchoredDraggableState<PlayerExpandedState>) -> Unit
) {
    val density = LocalDensity.current
    val context = LocalContext.current


    var isInPipMode by remember {
        mutableStateOf(context.findActivity()?.isInPictureInPictureMode == true)
    }

    DisposableEffect(context) {
        val activity = context.findActivity()
        val pipListener = Consumer<PictureInPictureModeChangedInfo> { info ->
            isInPipMode = info.isInPictureInPictureMode
        }
        activity?.addOnPictureInPictureModeChangedListener(pipListener)
        onDispose {
            activity?.removeOnPictureInPictureModeChangedListener(pipListener)
        }
    }


    val currentPipMode by rememberUpdatedState(isInPipMode)

    val state = remember {
        AnchoredDraggableState(
            initialValue = globalPlayerController.playerState,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = exponentialDecay(),
            confirmValueChange = { newValue ->

                if (currentPipMode && newValue == PlayerExpandedState.HIDDEN) false else true
            }
        )
    }

    LaunchedEffect(globalPlayerController.playerState) {
        if (state.currentValue != globalPlayerController.playerState) {
            state.animateTo(globalPlayerController.playerState)
        }
    }

    LaunchedEffect(state.currentValue) {
        if (globalPlayerController.playerState != state.currentValue) {

            if (currentPipMode && state.currentValue == PlayerExpandedState.HIDDEN) return@LaunchedEffect
            globalPlayerController.updatePlayerState(state.currentValue)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxHeightPx = constraints.maxHeight.toFloat()


        val miniPlayerHeightPx = with(density) { 64.dp.toPx() }
        LaunchedEffect(maxHeightPx, bottomOffsetPx) {
            if (maxHeightPx > 10f) {
                val newAnchors = DraggableAnchors {
                    PlayerExpandedState.EXPANDED at 0f
                    PlayerExpandedState.MINI at (maxHeightPx - miniPlayerHeightPx - bottomOffsetPx).coerceAtLeast(
                        0f
                    )
                    PlayerExpandedState.HIDDEN at maxHeightPx
                }




                state.updateAnchors(newAnchors, state.targetValue)
            }
        }

        val nestedScrollConnection = remember(state) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y
                    return if (delta < 0 && source == NestedScrollSource.UserInput) {
                        if (state.currentValue != PlayerExpandedState.EXPANDED) {
                            val consumed = state.dispatchRawDelta(delta)
                            Offset(0f, consumed)
                        } else {
                            Offset.Zero
                        }
                    } else {
                        Offset.Zero
                    }
                }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    val delta = available.y
                    return if (delta > 0 && source == NestedScrollSource.UserInput) {
                        val consumedY = state.dispatchRawDelta(delta)
                        Offset(0f, consumedY)
                    } else {
                        Offset.Zero
                    }
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    return if (available.y < 0 && state.currentValue != PlayerExpandedState.EXPANDED) {
                        state.settle(available.y)
                        Velocity(0f, available.y)
                    } else {
                        Velocity.Zero
                    }
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    return if (available.y > 0) {
                        state.settle(available.y)
                        Velocity(0f, available.y)
                    } else {
                        state.settle(available.y)
                        Velocity.Zero
                    }
                }
            }
        }

        val rawOffset = if (state.offset.isNaN()) maxHeightPx else state.offset


        val finalOffset = if (isInPipMode) 0f else rawOffset

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, finalOffset.roundToInt()) }

                .let {
                    if (!isInPipMode) {
                        it
                            .anchoredDraggable(state = state, orientation = Orientation.Vertical)
                            .nestedScroll(nestedScrollConnection)
                    } else it
                }
        ) {
            content(state)
        }
    }
}