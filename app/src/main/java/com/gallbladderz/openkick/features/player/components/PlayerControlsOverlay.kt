/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gallbladderz.openkick.R

@Composable
fun PlayerControlsOverlay(
    playWhenReady: Boolean,
    playbackState: Int,
    isFullscreen: Boolean,
    isAtLiveEdge: Boolean,
    onSeekToLiveEdge: () -> Unit,
    onPlayerManagerResume: () -> Unit,
    onPlayerManagerPause: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onShowSettings: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CustomPlayerControls(
            playWhenReady = playWhenReady,
            playbackState = playbackState,
            isFullscreen = isFullscreen,
            isAtLiveEdge = isAtLiveEdge,
            onSeekToLive = onSeekToLiveEdge,
            onPlayPause = {
                if (playWhenReady) onPlayerManagerPause() else onPlayerManagerResume()
            },
            onFullscreen = onFullscreenToggle,
            onSettings = onShowSettings,
            modifier = Modifier.fillMaxSize()
        )
        IconButton(
            onClick = {
                if (isFullscreen) onFullscreenToggle() else onBackClick()
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
