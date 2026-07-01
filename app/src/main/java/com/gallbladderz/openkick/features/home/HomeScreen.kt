package com.gallbladderz.openkick.features.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.components.ViewerCountBadge
import com.gallbladderz.openkick.features.categories.CategoriesScreen
import com.gallbladderz.openkick.ui.components.ClipCard
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.draw.clipToBounds

import com.gallbladderz.openkick.features.home.components.HomeFilterChipsRow
import com.gallbladderz.openkick.features.home.components.HeroStreamPager
import com.gallbladderz.openkick.features.home.components.StreamCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onStreamClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val defaultFilter = stringResource(R.string.filter_all)
    var selectedFilter by remember { mutableStateOf(defaultFilter) }
    var isGridMode by remember { mutableStateOf(false) }


    val pullRefreshState = rememberPullToRefreshState()


    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
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
                CategoriesScreen(onCategoryClick = onCategoryClick)
            } else {
                when (val uiState = state) {
                    is HomeUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    is HomeUiState.Success -> {
                        val listState = rememberLazyListState()

                        LaunchedEffect(listState) {
                            snapshotFlow {
                                listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                            }.collect { lastVisible ->
                                val totalItems = listState.layoutInfo.totalItemsCount
                                if (lastVisible != null && lastVisible >= totalItems - 5) {
                                    viewModel.loadMoreStreams()
                                }
                            }
                        }

                        val clipsFilter = stringResource(R.string.filter_clips)
                        val liveFilter = stringResource(R.string.live)

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
                                            onClick = { /* TODO: Плеер клипа */ }
                                        )

                                        if (rowItems.size > 1) {
                                            ClipCard(
                                                clip = rowItems[1],
                                                modifier = Modifier.weight(1f),
                                                onClick = { /* TODO: Плеер клипа */ }
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
                            onClick = { viewModel.fetchHomeData() },
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