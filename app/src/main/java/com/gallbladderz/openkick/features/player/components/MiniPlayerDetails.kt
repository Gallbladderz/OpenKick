/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MiniPlayerDetails(
    fraction: Float,
    title: String,
    playWhenReady: Boolean,
    onPlayerManagerPause: () -> Unit,
    onPlayerManagerResume: () -> Unit,
    onClose: () -> Unit,
    miniPlayerWidth: Dp
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = fraction > 0.5f,
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = miniPlayerWidth + 16.dp, end = 8.dp)
            .graphicsLayer {
                translationY = (1f - fraction) * 200f
                alpha = ((fraction - 0.7f) / 0.3f).coerceIn(0f, 1f)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .basicMarquee()
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (playWhenReady) onPlayerManagerPause() else onPlayerManagerResume()
                }) {
                    Icon(
                        imageVector = if (playWhenReady) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
