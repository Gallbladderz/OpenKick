package com.gallbladderz.openkick.features.categories

import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.categories.components.CategoryClipsList
import com.gallbladderz.openkick.features.categories.components.CategoryHeader
import com.gallbladderz.openkick.features.categories.components.CategoryStreamsGrid
import com.gallbladderz.openkick.features.categories.components.CategoryTabs
import com.gallbladderz.openkick.features.home.ClipUiModel
import com.gallbladderz.openkick.ui.components.StreamFilterBottomSheet
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsRoute(
    slug: String,
    viewModel: CategoryDetailsViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onStreamClick: (String) -> Unit = {},
    onClipClick: (ClipUiModel) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val langs by viewModel.selectedFilterLanguages.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    CategoryDetailsScreen(
        slug = slug,
        state = state,
        onLoadCategory = { viewModel.loadCategory(it) },
        onLoadMoreStreams = { viewModel.loadMoreStreams() },
        onLoadMoreClips = { viewModel.loadMoreClips() },
        onBackClick = onBackClick,
        onStreamClick = onStreamClick,
        onClipClick = onClipClick,
        onFilterClick = { showFilterSheet = true }
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
fun CategoryDetailsScreen(
    slug: String,
    state: CategoryDetailsUiState,
    onLoadCategory: (String) -> Unit,
    onLoadMoreStreams: () -> Unit,
    onLoadMoreClips: () -> Unit,
    onBackClick: () -> Unit,
    onStreamClick: (String) -> Unit = {},
    onClipClick: (ClipUiModel) -> Unit,
    onFilterClick: () -> Unit = {}
) {
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.streams_tab), stringResource(R.string.clips_tab))

    var isFollowed by remember { mutableStateOf(false) }

    val gridState = rememberSaveable(saver = androidx.compose.foundation.lazy.grid.LazyGridState.Saver) { androidx.compose.foundation.lazy.grid.LazyGridState() }
    val listState = rememberLazyListState()

    LaunchedEffect(gridState) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisible ->
            val totalItems = gridState.layoutInfo.totalItemsCount
            if (lastVisible != null && totalItems > 3 && lastVisible >= totalItems - 2) {
                onLoadMoreStreams()
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisible ->
            val totalItems = listState.layoutInfo.totalItemsCount
            if (lastVisible != null && totalItems > 3 && lastVisible >= totalItems - 2) {
                onLoadMoreClips()
            }
        }
    }

    LaunchedEffect(slug) {
        onLoadCategory(slug)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state is CategoryDetailsUiState.Success)
                            state.name
                        else stringResource(R.string.loading),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
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
                    IconButton(onClick = onFilterClick) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.filter_sort)
                        )
                    }
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "https://kick.com/categories/$slug")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, null))
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val currentState = state) {
                is CategoryDetailsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is CategoryDetailsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            currentState.message.asString(),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                is CategoryDetailsUiState.Success -> {
                    CategoryHeader(
                        bannerUrl = currentState.bannerUrl,
                        viewers = currentState.viewers,
                        tags = currentState.tags,
                        isFollowed = isFollowed,
                        onFollowClick = { isFollowed = !isFollowed }
                    )

                    CategoryTabs(
                        selectedTabIndex = selectedTabIndex,
                        tabs = tabs,
                        onTabSelected = { selectedTabIndex = it }
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (selectedTabIndex == 0) {
                            CategoryStreamsGrid(
                                streams = currentState.streams,
                                viewers = currentState.viewers,
                                gridState = gridState,
                                onStreamClick = onStreamClick
                            )
                        } else {
                            CategoryClipsList(
                                clips = currentState.clips,
                                listState = listState,
                                onClipClick = onClipClick
                            )
                        }
                    }
                }
            }
        }
    }
}