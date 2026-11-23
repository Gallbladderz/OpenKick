/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player

import android.content.Intent
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.data.local.DownloadMediaType
import com.gallbladderz.openkick.core.player.PlayerExpandedState
import com.gallbladderz.openkick.features.downloads.ClipDownloadAction
import com.gallbladderz.openkick.features.downloads.DownloadSource
import com.gallbladderz.openkick.features.player.components.ChatReplayPanel
import com.gallbladderz.openkick.features.player.components.RecordedFollowButton
import com.gallbladderz.openkick.features.player.components.RecordedInfoPanel
import com.gallbladderz.openkick.features.player.components.RecordedPlayerPlaceholder
import com.gallbladderz.openkick.features.player.components.RecordedPlayerScreen
import com.gallbladderz.openkick.features.player.components.RecordedShareButton
import com.gallbladderz.openkick.features.player.components.recordedMetadata
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun ClipPlayerRoute(
    clipId: String,
    dragState: AnchoredDraggableState<PlayerExpandedState>,
    onBackClick: () -> Unit,
    onStreamerClick: (String) -> Unit = {},
    clipPlayerViewModel: ClipPlayerViewModel = koinViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(clipId) { clipPlayerViewModel.loadClip(clipId) }

    val activeClip by clipPlayerViewModel.activeClip.collectAsStateWithLifecycle()
    val loadFailed by clipPlayerViewModel.loadFailed.collectAsStateWithLifecycle()
    val clip = activeClip
    if (clip == null) {
        RecordedPlayerPlaceholder(
            dragState, stringResource(R.string.clips_tab), onBackClick,
            error = R.string.load_error.takeIf { loadFailed }
        )
        return
    }

    val slug = clip.streamerName.takeUnless { it == "Anonymous" }.orEmpty()
    val channelName = clip.streamerDisplayName.ifBlank { slug }
    RecordedPlayerScreen(
        clipPlayerViewModel.player,
        dragState = dragState,
        title = clip.title,
        channelName = channelName,
        onBack = onBackClick,
        chat = { playback, modifier -> ChatReplayPanel(clip.chatReplay, playback, modifier, channelSlug = slug) }
    ) {
        RecordedInfoPanel(
            title = clip.title,
            channelName = channelName,
            avatarUrl = clip.streamerAvatarUrl,
            metadata = recordedMetadata(clip.views, clip.durationFormatted, clip.createdAt, clip.creatorName),
            categoryName = clip.categoryName,
            onProfile = slug.takeIf { it.isNotBlank() }?.let { { onStreamerClick(it) } }
        ) {
            ClipDownloadAction(
                DownloadSource(DownloadMediaType.CLIP, clipId, clip.title, channelName,
                    clip.thumbnailUrl, clip.durationSec, clip.streamerAvatarUrl)
            )
            RecordedShareButton(onShare = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://kick.com/clips/$clipId")
                }
                context.startActivity(Intent.createChooser(intent, null))
            })
            RecordedFollowButton(slug)
        }
    }
}
