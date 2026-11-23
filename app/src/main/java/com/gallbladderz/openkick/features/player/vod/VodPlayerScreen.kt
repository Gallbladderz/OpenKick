/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.player.vod

import android.content.Intent
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.data.local.DownloadMediaType
import com.gallbladderz.openkick.core.player.PlayerExpandedState
import com.gallbladderz.openkick.features.downloads.DownloadSource
import com.gallbladderz.openkick.features.downloads.VodDownloadAction
import com.gallbladderz.openkick.features.player.components.ChatReplayPanel
import com.gallbladderz.openkick.features.player.components.RecordedFollowButton
import com.gallbladderz.openkick.features.player.components.RecordedInfoPanel
import com.gallbladderz.openkick.features.player.components.RecordedPlayerPlaceholder
import com.gallbladderz.openkick.features.player.components.RecordedPlayerScreen
import com.gallbladderz.openkick.features.player.components.RecordedShareButton
import com.gallbladderz.openkick.features.player.components.recordedMetadata
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
internal fun VodPlayerRoute(
    videoId: String,
    channelSlug: String?,
    dragState: AnchoredDraggableState<PlayerExpandedState>,
    onBackClick: () -> Unit,
    onStreamerClick: (String) -> Unit
) {
    val viewModel: VodPlayerViewModel = koinViewModel(
        parameters = { parametersOf(videoId, channelSlug.orEmpty()) }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val label = stringResource(R.string.vod_desc)

    when (val state = uiState) {
        VodPlayerUiState.Loading -> RecordedPlayerPlaceholder(dragState, label, onBackClick)
        is VodPlayerUiState.Error -> RecordedPlayerPlaceholder(dragState, label, onBackClick, message = state.message)
        is VodPlayerUiState.Success -> {
            val player = viewModel.playerManager.player
            if (player == null) {
                RecordedPlayerPlaceholder(dragState, label, onBackClick, error = R.string.load_error)
                return
            }
            val metadata = state.metadata
            val slug = metadata.channelSlug
            RecordedPlayerScreen(
                player,
                dragState = dragState,
                title = metadata.title,
                channelName = metadata.channelUsername,
                onBack = onBackClick,
                chat = { playback, modifier ->
                    ChatReplayPanel(metadata.chatReplay, playback, modifier, channelSlug = slug)
                }
            ) { playback ->
                RecordedInfoPanel(
                    title = metadata.title,
                    channelName = metadata.channelUsername,
                    avatarUrl = metadata.channelAvatarUrl,
                    metadata = recordedMetadata(metadata.views, metadata.durationFormatted, metadata.createdAt),
                    categoryName = metadata.categoryName,
                    onProfile = slug.takeIf { it.isNotBlank() }?.let { { onStreamerClick(it) } }
                ) {
                    VodDownloadAction(
                        source = DownloadSource(
                            DownloadMediaType.VOD, videoId, metadata.title, metadata.channelUsername,
                            metadata.thumbnailUrl, (playback.durationMs / 1000).takeIf { it > 0 },
                            metadata.channelAvatarUrl
                        ),
                        playbackUrl = state.playbackUrl,
                        durationMs = playback.durationMs,
                        positionMs = playback.positionMs,
                        currentQuality = playback.quality
                    )
                    RecordedShareButton(onShare = {
                        val url = if (slug.isBlank()) "https://kick.com/video/$videoId"
                        else "https://kick.com/$slug/videos/$videoId"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_vod)))
                    })
                    RecordedFollowButton(slug)
                }
            }
        }
    }
}
