/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.home.components.HomeFilterChipsRow
import com.gallbladderz.openkick.features.home.components.homeClipsList
import com.gallbladderz.openkick.features.home.components.homeStreamsList
import com.gallbladderz.openkick.ui.components.StreamFilterBottomSheet
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val isGridMode by viewModel.isGridMode.collectAsStateWithLifecycle()
    val langs by viewModel.selectedFilterLanguages.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        onSearchClick = onSearchClick,
        onFilterClick = { showFilterSheet = true },
        isGridMode = isGridMode,
        onGridModeChange = { viewModel.setGridMode(it) }
    )

    if (showFilterSheet) {
        StreamFilterBottomSheet(
            sheetState = sheetState,
            currentSort = sort,
            currentLanguages = langs,
            onDismissRequest = { showFilterSheet = false },
            onApply = { newSort, newLangs ->
                viewModel.updateFiltersAndRefresh(newSort, newLangs)
                showFilterSheet = false
            }
        )
    }
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
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit = {},
    isGridMode: Boolean,
    onGridModeChange: (Boolean) -> Unit
) {
    val defaultFilter = stringResource(R.string.filter_all)
    var selectedFilter by remember { mutableStateOf(defaultFilter) }

    val density = LocalDensity.current
    val filterChipsMaxHeightPx = with(density) { 56.dp.toPx() }
    var filterChipsHeightOffset by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = filterChipsHeightOffset + delta
                filterChipsHeightOffset = newOffset.coerceIn(-filterChipsMaxHeightPx, 0f)
                return Offset.Zero
            }
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { (filterChipsMaxHeightPx + filterChipsHeightOffset).toDp() })
                .clipToBounds(),
            contentAlignment = Alignment.BottomCenter
        ) {
            HomeFilterChipsRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                isGridMode = isGridMode,
                onGridModeChange = { onGridModeChange(it) },
                onFilterClick = onFilterClick
            )
        }


        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .nestedScroll(nestedScrollConnection)
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
                        val listState = rememberSaveable(
                            selectedFilter,
                            saver = LazyListState.Saver
                        ) { LazyListState() }

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
                                homeClipsList(
                                    clips = uiState.clips,
                                    onClipClick = onClipClick
                                )
                            } else {
                                homeStreamsList(
                                    streams = uiState.streams,
                                    isGridMode = isGridMode,
                                    liveFilterText = liveFilter,
                                    onStreamClick = onStreamClick
                                )
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
        }
    }
}
