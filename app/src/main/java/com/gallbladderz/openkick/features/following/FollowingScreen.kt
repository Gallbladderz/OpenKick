/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.following

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.TvOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.components.KickPullToRefreshBox
import com.gallbladderz.openkick.core.ui.components.StreamActionsSheet
import com.gallbladderz.openkick.core.ui.components.ViewerCountBadge
import com.gallbladderz.openkick.core.ui.theme.LiveIndicatorRed
import com.gallbladderz.openkick.features.categories.formatViewers
import org.koin.androidx.compose.koinViewModel

private val FollowingGutter = 16.dp
private val LivePreviewWidth = 132.dp
private val LivePreviewShape = RoundedCornerShape(8.dp)
private val PreviewScrim = Brush.verticalGradient(
    listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
)

@Composable
fun FollowingRoute(
    viewModel: FollowingViewModel = koinViewModel(),
    onManageClick: () -> Unit = {},
    onStreamClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    FollowingScreen(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        onManageClick = onManageClick,
        onStreamClick = onStreamClick,
        onProfileClick = onProfileClick,
        onCategoryClick = onCategoryClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(
    state: FollowingUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onManageClick: () -> Unit = {},
    onStreamClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit = {}
) {
    val bottomBarPadding =
        80.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val liveCount = (state as? FollowingUiState.Success)?.liveStreamers?.size ?: 0

    Scaffold(topBar = { FollowingTopBar(liveCount, onManageClick) }) { paddingValues ->
        KickPullToRefreshBox(
            isRefreshing = isRefreshing && state != FollowingUiState.Loading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clipToBounds()
        ) {
            when (state) {
                FollowingUiState.Loading -> FollowingSkeleton()

                is FollowingUiState.Error -> FollowingMessage(
                    icon = Icons.Outlined.ErrorOutline,
                    message = state.message.asString(),
                    iconTint = MaterialTheme.colorScheme.error,
                    onRetry = onRefresh,
                    retryEnabled = !isRefreshing,
                    modifier = Modifier.fillMaxSize().padding(bottom = bottomBarPadding)
                )

                is FollowingUiState.Success -> {
                    val allStreamers = state.liveStreamers + state.offlineStreamers
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = bottomBarPadding)
                    ) {
                        if (allStreamers.isNotEmpty()) {
                            item(key = "channels", contentType = "channel_strip") {
                                Column {
                                    FollowingSectionHeader(
                                        title = stringResource(R.string.channels),
                                        modifier = Modifier.padding(horizontal = FollowingGutter)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = FollowingGutter),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(
                                            items = allStreamers,
                                            key = { it.slug },
                                            contentType = { "channel_avatar" }
                                        ) { streamer ->
                                            StoryAvatarItem(streamer) {
                                                if (streamer.isLive) {
                                                    onStreamClick(streamer.slug)
                                                } else {
                                                    onProfileClick(streamer.slug)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item(key = "live_header", contentType = "section_header") {
                                FollowingSectionHeader(
                                    title = stringResource(R.string.live),
                                    count = liveCount,
                                    modifier = Modifier.padding(
                                        start = FollowingGutter,
                                        top = 16.dp,
                                        end = FollowingGutter,
                                        bottom = 8.dp
                                    )
                                )
                            }

                            if (state.liveStreamers.isEmpty()) {
                                item(key = "no_live", contentType = "notice") {
                                    NoLiveChannelsNotice()
                                }
                            }

                            items(
                                count = state.liveStreamers.size,
                                key = { "stream:${state.liveStreamers[it].slug}" },
                                contentType = { "live_stream" }
                            ) { index ->
                                val streamer = state.liveStreamers[index]
                                LiveStreamRow(
                                    streamer = streamer,
                                    modifier = Modifier.padding(top = if (index == 0) 0.dp else 12.dp),
                                    onClick = { onStreamClick(streamer.slug) },
                                    onProfileClick = { onProfileClick(streamer.slug) }
                                )
                            }
                        }

                        if (state.categories.isNotEmpty()) {
                            item(key = "categories", contentType = "category_strip") {
                                Column {
                                    if (allStreamers.isNotEmpty()) Spacer(Modifier.height(20.dp))
                                    FollowingSectionHeader(
                                        title = stringResource(R.string.categories),
                                        modifier = Modifier.padding(horizontal = FollowingGutter)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = FollowingGutter),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(
                                            items = state.categories,
                                            key = { it.slug },
                                            contentType = { "category_poster" }
                                        ) { category ->
                                            FollowedCategoryItem(category) { onCategoryClick(category.slug) }
                                        }
                                    }
                                }
                            }
                        }

                        if (allStreamers.isEmpty() && state.categories.isEmpty()) {
                            item(key = "empty", contentType = "notice") {
                                FollowingMessage(
                                    icon = Icons.Outlined.FavoriteBorder,
                                    message = stringResource(R.string.empty_follow_someone),
                                    modifier = Modifier.fillParentMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowingTopBar(liveCount: Int, onManageClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(start = FollowingGutter, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.followed_streams),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (liveCount > 0) {
                val countDescription = "${stringResource(R.string.live)}: $liveCount"
                Row(
                    modifier = Modifier
                        .clearAndSetSemantics { contentDescription = countDescription }
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                    Text(
                        text = liveCount.toString(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        IconButton(onClick = onManageClick) {
            Icon(Icons.Outlined.People, contentDescription = stringResource(R.string.all_followed))
        }
    }
}

@Composable
private fun StoryAvatarItem(streamer: FollowedStreamerUi, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Box(contentAlignment = Alignment.TopCenter) {
            Box(Modifier.padding(bottom = 5.dp)) {
                FollowedChannelAvatar(
                    streamer = streamer,
                    size = 54.dp,
                    liveGap = 2.5.dp,
                    offlineAlpha = 0.6f,
                    showOfflineOutline = true
                )
            }
            if (streamer.isLive) {
                Text(
                    text = stringResource(R.string.following_live_badge),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
        Text(
            text = streamer.username,
            style = MaterialTheme.typography.labelSmall,
            color = if (streamer.isLive) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LiveStreamRow(
    streamer: FollowedStreamerUi,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showActions = true }
            )
            .padding(horizontal = FollowingGutter),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(LivePreviewWidth)
                .aspectRatio(16f / 9f)
                .clip(LivePreviewShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = streamer.streamThumbnailUrl.takeIf { it.isNotBlank() },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Spacer(
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(PreviewScrim)
            )
            ViewerCountBadge(
                viewers = streamer.viewers,
                modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = streamer.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(20.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = streamer.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (streamer.streamTitle.isNotBlank()) {
                Text(
                    text = streamer.streamTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (streamer.categoryName.isNotBlank()) {
                Text(
                    text = streamer.categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }

    if (showActions) {
        StreamActionsSheet(
            title = streamer.streamTitle,
            streamerName = streamer.username,
            channelSlug = streamer.slug,
            thumbnailUrl = streamer.streamThumbnailUrl,
            onDismiss = { showActions = false },
            onProfileClick = onProfileClick
        )
    }
}

@Composable
private fun NoLiveChannelsNotice() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FollowingGutter)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.TvOff,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.following_no_live_channels),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FollowedCategoryItem(category: FollowedCategoryUi, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(104.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = category.bannerUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Spacer(
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(PreviewScrim)
            )
            Row(
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(5.dp).background(LiveIndicatorRed, CircleShape))
                Text(
                    text = formatViewers(category.viewers),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FollowingSkeleton() {
    val placeholder = MaterialTheme.colorScheme.surfaceVariant
    val loadingLabel = stringResource(R.string.loading)
    val lineShape = RoundedCornerShape(4.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .semantics { contentDescription = loadingLabel }
            .padding(horizontal = FollowingGutter, vertical = 8.dp)
    ) {
        Spacer(Modifier.size(72.dp, 18.dp).background(placeholder, lineShape))
        Spacer(Modifier.height(14.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(count = 5, key = { it }, contentType = { "avatar_placeholder" }) {
                Column(
                    modifier = Modifier.width(64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.size(54.dp).background(placeholder, CircleShape))
                    Spacer(Modifier.height(8.dp))
                    Spacer(Modifier.size(40.dp, 10.dp).background(placeholder, lineShape))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Spacer(Modifier.size(56.dp, 18.dp).background(placeholder, lineShape))
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(
                        Modifier.width(LivePreviewWidth)
                            .aspectRatio(16f / 9f)
                            .background(placeholder, LivePreviewShape)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.size(20.dp).background(placeholder, CircleShape))
                            Spacer(Modifier.fillMaxWidth(0.5f).height(12.dp).background(placeholder, lineShape))
                        }
                        Spacer(Modifier.fillMaxWidth(0.9f).height(12.dp).background(placeholder, lineShape))
                        Spacer(Modifier.size(56.dp, 16.dp).background(placeholder, lineShape))
                    }
                }
            }
        }
    }
}
