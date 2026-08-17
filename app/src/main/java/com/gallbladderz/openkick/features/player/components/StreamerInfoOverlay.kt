/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.components

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.gallbladderz.openkick.R

@Composable
fun StreamerInfoOverlay(
    streamerName: String,
    title: String,
    avatarUrl: String,
    viewers: Int,
    categoryName: String,
    categorySlug: String?,
    isFollowed: Boolean,
    fraction: Float,
    onToggleFollow: () -> Unit,
    onAvatarClick: () -> Unit,
    onCategoryClick: (String) -> Unit
) {
    val context = LocalContext.current
    val shareDesc = stringResource(R.string.share_desc)

    Box(modifier = Modifier.graphicsLayer {
        alpha = 1f - (fraction * 2f).coerceIn(0f, 1f)
    }) {
        StreamerInfoCard(
            streamerName = streamerName,
            title = title,
            avatarUrl = avatarUrl,
            viewers = viewers,
            categoryName = categoryName,
            isFollowed = isFollowed,
            onToggleFollow = onToggleFollow,
            onAvatarClick = onAvatarClick,
            onCategoryClick = {
                categorySlug?.let { slug -> onCategoryClick(slug) }
            },
            onShareClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "https://kick.com/$streamerName"
                    )
                }
                context.startActivity(
                    Intent.createChooser(
                        shareIntent,
                        shareDesc
                    )
                )
            }
        )
    }
}
