/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.profile

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.home.ClipUiModel
import com.gallbladderz.openkick.features.profile.components.ClipsTab
import com.gallbladderz.openkick.features.profile.components.DescriptionTab
import com.gallbladderz.openkick.features.profile.components.ProfileHeader
import com.gallbladderz.openkick.features.profile.components.VideosTab
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun StreamerProfileRoute(
    slug: String,
    onBackClick: () -> Unit,
    onStreamClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onClipClick: (ClipUiModel) -> Unit,
    viewModel: StreamerProfileViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    StreamerProfileScreen(
        slug = slug,
        state = state,
        isRefreshing = isRefreshing,
        onLoadProfile = { viewModel.loadProfile(it) },
        onRefresh = { viewModel.refresh() },
        onToggleFollow = { viewModel.toggleFollow() },
        onLoadMoreClips = { viewModel.loadMoreClips() },
        onBackClick = onBackClick,
        onStreamClick = onStreamClick,
        onVideoClick = onVideoClick,
        onClipClick = onClipClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamerProfileScreen(
    slug: String,
    state: ProfileUiState,
    isRefreshing: Boolean,
    onLoadProfile: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggleFollow: () -> Unit,
    onLoadMoreClips: () -> Unit,
    onBackClick: () -> Unit,
    onStreamClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onClipClick: (ClipUiModel) -> Unit
) {
    LaunchedEffect(slug) {
        onLoadProfile(slug)
    }

    Scaffold(
        topBar = {
            ProfileTopAppBar(state = state, onBackClick = onBackClick)
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clipToBounds()
        ) {
            ProfileContent(
                state = state,
                onToggleFollow = onToggleFollow,
                onStreamClick = onStreamClick,
                onVideoClick = onVideoClick,
                onClipClick = onClipClick,
                onLoadMoreClips = onLoadMoreClips
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar(state: ProfileUiState, onBackClick: () -> Unit) {
    val context = LocalContext.current

    TopAppBar(
        title = {
            Text(
                text = if (state is ProfileUiState.Success) state.info.username else stringResource(
                    R.string.profile_title
                ),
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
        },
        actions = {
            if (state is ProfileUiState.Success) {
                IconButton(onClick = {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, "https://kick.com/${state.info.slug}")
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = stringResource(R.string.share_profile_desc)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onToggleFollow: () -> Unit,
    onStreamClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onClipClick: (ClipUiModel) -> Unit,
    onLoadMoreClips: () -> Unit
) {
    when (state) {
        is ProfileUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        is ProfileUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    state.message.asString(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        is ProfileUiState.Success -> {
            ProfileSuccessContent(
                uiState = state,
                onToggleFollow = onToggleFollow,
                onStreamClick = onStreamClick,
                onVideoClick = onVideoClick,
                onClipClick = onClipClick,
                onLoadMoreClips = onLoadMoreClips
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSuccessContent(
    uiState: ProfileUiState.Success,
    onToggleFollow: () -> Unit,
    onStreamClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    onClipClick: (ClipUiModel) -> Unit,
    onLoadMoreClips: () -> Unit
) {
    val tabs = listOf(
        stringResource(R.string.description),
        stringResource(R.string.vods),
        stringResource(R.string.filter_clips)
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        ProfileHeader(
            info = uiState.info,
            isFollowing = uiState.isFollowing,
            onFollowClick = onToggleFollow,
            onAvatarClick = { onStreamClick(uiState.info.slug) }
        )
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        ) { page ->
            when (page) {
                0 -> DescriptionTab(uiState.info.bio, uiState.links)
                1 -> VideosTab(
                    videos = uiState.videos,
                    onVideoClick = { video -> onVideoClick(video.id) }
                )

                2 -> ClipsTab(
                    clips = uiState.clips,
                    onClipClick = onClipClick,
                    onLoadMore = onLoadMoreClips
                )
            }
        }
    }
}
