/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.following

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.categories.formatViewers
import org.koin.androidx.compose.koinViewModel

@Composable
fun AllFollowsRoute(
    viewModel: FollowingViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onStreamClick: (String) -> Unit,
    onProfileClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    AllFollowsScreen(
        state = state,
        onUnfollowStreamer = { viewModel.unfollowStreamer(it) },
        onBackClick = onBackClick,
        onStreamClick = onStreamClick,
        onProfileClick = onProfileClick,
        onRefresh = { viewModel.refresh() },
        isRefreshing = isRefreshing
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFollowsScreen(
    state: FollowingUiState,
    onUnfollowStreamer: (String) -> Unit,
    onBackClick: () -> Unit,
    onStreamClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onRefresh: (() -> Unit)? = null,
    isRefreshing: Boolean = false
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.all_followed),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (state) {
                FollowingUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is FollowingUiState.Error -> FollowingMessage(
                    icon = Icons.Outlined.ErrorOutline,
                    message = state.message.asString(),
                    iconTint = MaterialTheme.colorScheme.error,
                    onRetry = onRefresh,
                    retryEnabled = !isRefreshing,
                    modifier = Modifier.fillMaxSize()
                )

                is FollowingUiState.Success -> {
                    val query = searchQuery.trim()
                    val live = remember(state.liveStreamers, query) {
                        state.liveStreamers.matchingQuery(query)
                    }
                    val offline = remember(state.offlineStreamers, query) {
                        state.offlineStreamers.matchingQuery(query)
                    }
                    val hasFollows = state.liveStreamers.isNotEmpty() || state.offlineStreamers.isNotEmpty()

                    Column(Modifier.fillMaxSize()) {
                        FollowsSearchField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        )

                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (live.isNotEmpty()) {
                                followsSection(
                                    key = "live",
                                    titleRes = R.string.live,
                                    streamers = live,
                                    onChannelClick = onStreamClick,
                                    onUnfollowStreamer = onUnfollowStreamer
                                )
                            }
                            if (offline.isNotEmpty()) {
                                followsSection(
                                    key = "offline",
                                    titleRes = R.string.following_offline,
                                    streamers = offline,
                                    onChannelClick = onProfileClick,
                                    onUnfollowStreamer = onUnfollowStreamer
                                )
                            }
                            if (live.isEmpty() && offline.isEmpty()) {
                                item(key = "empty", contentType = "notice") {
                                    FollowingMessage(
                                        icon = if (hasFollows) Icons.Default.Search else Icons.Outlined.FavoriteBorder,
                                        message = stringResource(
                                            if (hasFollows) R.string.following_search_empty else R.string.empty_follow_someone
                                        ),
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
}

@Composable
private fun FollowsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchLabel = stringResource(R.string.search)
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(searchLabel) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search))
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
}

private fun List<FollowedStreamerUi>.matchingQuery(query: String): List<FollowedStreamerUi> {
    if (query.isEmpty()) return this
    return filter {
        it.username.contains(query, ignoreCase = true) ||
            it.categoryName.contains(query, ignoreCase = true)
    }
}

private fun LazyListScope.followsSection(
    key: String,
    titleRes: Int,
    streamers: List<FollowedStreamerUi>,
    onChannelClick: (String) -> Unit,
    onUnfollowStreamer: (String) -> Unit
) {
    item(key = key, contentType = "section_header") {
        FollowingSectionHeader(
            title = stringResource(titleRes),
            count = streamers.size,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp)
        )
    }
    items(items = streamers, key = { "channel:${it.slug}" }, contentType = { "channel_row" }) { streamer ->
        FollowedChannelRow(
            streamer = streamer,
            onClick = { onChannelClick(streamer.slug) },
            onUnfollowClick = { onUnfollowStreamer(streamer.slug) }
        )
    }
}

@Composable
private fun FollowedChannelRow(
    streamer: FollowedStreamerUi,
    onClick: () -> Unit,
    onUnfollowClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FollowedChannelAvatar(streamer, size = 48.dp, liveGap = 2.dp, offlineAlpha = 0.75f)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = streamer.username,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (streamer.isLive) {
                val meta = streamer.categoryName.ifBlank { stringResource(R.string.live) }
                val metaStyle = MaterialTheme.typography.labelSmall
                val metaColor = MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = meta,
                        style = metaStyle,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (streamer.viewers > 0) {
                        Text(
                            text = "· ${stringResource(R.string.viewers_count, formatViewers(streamer.viewers))}",
                            style = metaStyle,
                            color = metaColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        FilledTonalButton(
            onClick = onUnfollowClick,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.unfollow_action),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
