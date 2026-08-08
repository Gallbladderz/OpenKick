package com.gallbladderz.openkick.features.profile

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
    onVideoClick: (VideoUiModel, ProfileInfoUi) -> Unit,
    onClipClick: (ClipUiModel) -> Unit,
    viewModel: StreamerProfileViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val loadingVideoId by viewModel.loadingVideoId.collectAsStateWithLifecycle()

    StreamerProfileScreen(
        slug = slug,
        state = state,
        isRefreshing = isRefreshing,
        loadingVideoId = loadingVideoId,
        onLoadProfile = { viewModel.loadProfile(it) },
        onRefresh = { viewModel.refresh() },
        onToggleFollow = { viewModel.toggleFollow() },
        onLoadMoreClips = { viewModel.loadMoreClips() },
        onLoadVideoPlaybackUrl = { videoId, onResult ->
            viewModel.loadVideoPlaybackUrl(
                videoId,
                onResult
            )
        },
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
    loadingVideoId: String?,
    onLoadProfile: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggleFollow: () -> Unit,
    onLoadMoreClips: () -> Unit,
    onLoadVideoPlaybackUrl: (String, (Result<String>) -> Unit) -> Unit,
    onBackClick: () -> Unit,
    onStreamClick: (String) -> Unit,
    onVideoClick: (VideoUiModel, ProfileInfoUi) -> Unit,
    onClipClick: (ClipUiModel) -> Unit
) {

    val tabs = listOf(
        stringResource(R.string.description),
        stringResource(R.string.vods),
        stringResource(R.string.filter_clips)
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(slug) {
        onLoadProfile(slug)
    }

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            onRefresh()
        }
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            pullRefreshState.startRefresh()
        } else {
            pullRefreshState.endRefresh()
        }
    }

    Scaffold(
        topBar = {
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
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                val shareText = context.getString(
                                    R.string.share_intent_text,
                                    state.info.username,
                                    state.info.slug
                                ).replace("\\n", "\n")
                                putExtra(Intent.EXTRA_TEXT, shareText)
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clipToBounds()
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            when (val uiState = state) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                is ProfileUiState.Error -> {
                    Text(
                        uiState.message.asString(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is ProfileUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ProfileHeader(
                            info = uiState.info,
                            isFollowing = uiState.isFollowing,
                            onFollowClick = { onToggleFollow() },
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
                                    loadingVideoId = loadingVideoId,
                                    onVideoClick = { video ->
                                        onLoadVideoPlaybackUrl(video.id) { result ->
                                            result.onSuccess { url ->
                                                val videoWithUrl = video.copy(videoUrl = url)
                                                onVideoClick(videoWithUrl, uiState.info)
                                            }.onFailure { error ->
                                                Toast.makeText(
                                                    context,
                                                    context.getString(
                                                        R.string.error_prefix_msg,
                                                        error.message
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
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
            }
            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}