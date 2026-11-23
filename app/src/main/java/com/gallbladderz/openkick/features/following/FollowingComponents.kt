/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.following

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.theme.KickGradientEnd
import com.gallbladderz.openkick.core.ui.theme.KickGradientStart

private val LiveAvatarGradient = Brush.linearGradient(listOf(KickGradientStart, KickGradientEnd))

@Composable
internal fun FollowedChannelAvatar(
    streamer: FollowedStreamerUi,
    size: Dp,
    liveGap: Dp,
    offlineAlpha: Float,
    showOfflineOutline: Boolean = false
) {
    val frame = when {
        streamer.isLive -> Modifier
            .border(2.dp, LiveAvatarGradient, CircleShape)
            .padding(2.dp + liveGap)

        showOfflineOutline -> Modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(1.dp)

        else -> Modifier
    }

    AsyncImage(
        model = streamer.avatarUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(size)
            .then(frame)
            .clip(CircleShape)
            .alpha(if (streamer.isLive) 1f else offlineAlpha)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
internal fun FollowingSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun FollowingMessage(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onRetry: (() -> Unit)? = null,
    retryEnabled: Boolean = true
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = iconTint)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry, enabled = retryEnabled) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
