/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.gallbladderz.openkick.core.ui.utils.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerTimeline(
    progress: Float,
    positionMs: Long?,
    durationMs: Long?,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    sliderModifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    status: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val remainingColor = Color.White.copy(alpha = 0.28f)
    val isDragging by interactionSource.collectIsDraggedAsState()
    val targetHeight = 18.dp

    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val spacing = if (isPortrait) (-10).dp else 0.dp
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                status?.invoke()
                if (positionMs != null) {
                    Text(
                        text = buildAnnotatedString {
                            if (status != null) append(" · ")
                            append(formatTime(positionMs))
                            if (durationMs != null) {
                                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.72f))) {
                                    append(" / ")
                                    append(formatTime(durationMs))
                                }
                            }
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            actions()
        }
        if (enabled) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Slider(
                    value = progress.coerceIn(0f, 1f),
                    onValueChange = onValueChange,
                    onValueChangeFinished = onValueChangeFinished,
                    modifier = sliderModifier.fillMaxWidth().height(targetHeight),
                    interactionSource = interactionSource,
                    thumb = {
                        Box(
                            modifier = Modifier.size(width = 12.dp, height = targetHeight),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                Modifier.padding(bottom = if (isDragging) 0.dp else 1.dp)
                                    .size(if (isDragging) 12.dp else 10.dp)
                                    .background(accent, CircleShape)
                            )
                        }
                    },
                    track = { state ->
                        Canvas(Modifier.fillMaxWidth().height(targetHeight)) {
                            val thickness = 3.dp.toPx()
                            val radius = CornerRadius(thickness / 2)
                            val top = size.height - 6.dp.toPx() - radius.y
                            drawRoundRect(
                                color = remainingColor,
                                topLeft = Offset(0f, top),
                                size = Size(size.width, thickness),
                                cornerRadius = radius
                            )
                            val playedWidth = size.width * state.value.coerceIn(0f, 1f)
                            if (playedWidth > 0f) {
                                val left = if (layoutDirection == LayoutDirection.Rtl) {
                                    size.width - playedWidth
                                } else {
                                    0f
                                }
                                drawRoundRect(
                                    color = accent,
                                    topLeft = Offset(left, top),
                                    size = Size(playedWidth, thickness),
                                    cornerRadius = radius
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
