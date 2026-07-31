package com.gallbladderz.openkick.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.home.components.HeroStreamPager
import com.gallbladderz.openkick.features.home.components.HomeFilterChipsRow
import com.gallbladderz.openkick.features.home.components.StreamCard
import com.gallbladderz.openkick.ui.components.ClipCard
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = koinViewModel(),
    onStreamClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onClipClick: (ClipUiModel) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        onLoadMoreStreams = { viewModel.loadMoreStreams() },
        onLoadMoreClips = { viewModel.loadMoreClips() },
        onFetchHomeData = { viewModel.fetchHomeData() },
        onStreamClick = onStreamClick,
        onCategoryClick = onCategoryClick,
        onClipClick = onClipClick,
        onSearchClick = onSearchClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onLoadMoreStreams: () -> Unit,
    onLoadMoreClips: () -> Unit,
    onFetchHomeData: () -> Unit,
    onStreamClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onClipClick: (ClipUiModel) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val defaultFilter = stringResource(R.string.filter_all)
    var selectedFilter by remember { mutableStateOf(defaultFilter) }
    var isGridMode by remember { mutableStateOf(false) }

    val pullRefreshState = rememberPullToRefreshState()

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "OpenKick",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        HomeFilterChipsRow(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it },
            isGridMode = isGridMode,
            onGridModeChange = { isGridMode = it }
        )


        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            if (selectedFilter == stringResource(R.string.filter_categories)) {
                com.gallbladderz.openkick.features.categories.CategoriesRoute(onCategoryClick = onCategoryClick)
            } else {
                when (val uiState = state) {
                    is HomeUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

is HomeUiState.Success -> {
                        val clipsFilter = stringResource(R.string.filter_clips)
                        val liveFilter = stringResource(R.string.live)
                        val listState = remember(selectedFilter) { LazyListState() }

                        LaunchedEffect(listState, selectedFilter) {
                            snapshotFlow {
                                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                            }.collect { lastVisible ->
                                val totalItems = listState.layoutInfo.totalItemsCount
                                if (lastVisible != null && totalItems > 3 && lastVisible >= totalItems - 2) {
                                    if (selectedFilter == clipsFilter) {
                                        onLoadMoreClips()
                                    } else {
                                        onLoadMoreStreams()
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (selectedFilter == clipsFilter) {
                                item {
                                    Text(
                                        text = stringResource(R.string.top_clips_week),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                val clipRows = uiState.clips.chunked(2)

                                itemsIndexed(clipRows) { _, rowItems ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        ClipCard(
                                            clip = rowItems[0],
                                            modifier = Modifier.weight(1f),
                                            onClick = { onClipClick(rowItems[0]) }
                                        )

                                        if (rowItems.size > 1) {
                                            ClipCard(
                                                clip = rowItems[1],
                                                modifier = Modifier.weight(1f),
                                                onClick = { onClipClick(rowItems[1]) }
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }

                            } else {
                                val heroStreams = uiState.streams.take(5)
                                val feedStreams = uiState.streams.drop(5)

                                if (heroStreams.isNotEmpty()) {
                                    item {
                                        HeroStreamPager(
                                            streams = heroStreams,
                                            onStreamClick = onStreamClick
                                        )
                                    }
                                }

                                if (feedStreams.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = liveFilter,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                }

                                if (isGridMode) {
                                    val streamRows = feedStreams.chunked(2)

                                    itemsIndexed(streamRows) { _, rowItems ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            StreamCard(
                                                stream = rowItems[0],
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    onStreamClick(rowItems[0].streamerName)
                                                }
                                            )

                                            if (rowItems.size > 1) {
                                                StreamCard(
                                                    stream = rowItems[1],
                                                    modifier = Modifier.weight(1f),
                                                    onClick = {
                                                        onStreamClick(rowItems[1].streamerName)
                                                    }
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }

                                } else {
                                    itemsIndexed(
                                        feedStreams,
                                        key = { _, it -> it.id }
                                    ) { _, stream ->
                                        StreamCard(
                                            stream = stream,
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            onClick = {
                                                onStreamClick(stream.streamerName)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    is HomeUiState.Error -> {
                        Button(
                            onClick = { onFetchHomeData() },
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text(stringResource(R.string.retry))
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
