package com.gallbladderz.openkick.features.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onStreamClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val defaultFilter = stringResource(R.string.filter_all)
    var selectedFilter by remember { mutableStateOf(defaultFilter) }
    var isGridMode by remember { mutableStateOf(false) }

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

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {

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

                                val totalItems =
                                    listState.layoutInfo.totalItemsCount

                                if (
                                    lastVisible != null &&
                                    lastVisible >= totalItems - 5
                                ) {
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
        }
    }
}

@Composable
fun HomeFilterChipsRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    isGridMode: Boolean,
    onGridModeChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            val filters = listOf(stringResource(R.string.filter_all), stringResource(R.string.filter_categories), stringResource(R.string.filter_clips))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = { Text(filter, fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == filter,
                            borderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        if (selectedFilter == stringResource(R.string.filter_all)) {
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { onGridModeChange(!isGridMode) },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isGridMode) Icons.AutoMirrored.Filled.List else rememberGridViewIcon(),
                        contentDescription = stringResource(R.string.toggle_view),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun rememberGridViewIcon(): ImageVector {
    return remember {
        ImageVector.Builder(
            name = "GridView",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 3f)
                lineTo(11f, 3f)
                lineTo(11f, 11f)
                lineTo(3f, 11f)
                close()
                moveTo(13f, 3f)
                lineTo(21f, 3f)
                lineTo(21f, 11f)
                lineTo(13f, 11f)
                close()
                moveTo(3f, 13f)
                lineTo(11f, 13f)
                lineTo(11f, 21f)
                lineTo(3f, 21f)
                close()
                moveTo(13f, 13f)
                lineTo(21f, 13f)
                lineTo(21f, 21f)
                lineTo(13f, 21f)
                close()
            }
        }.build()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroStreamPager(
    streams: List<StreamUiModel>,
    onStreamClick: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { streams.size })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
        pageSpacing = 12.dp
    ) { page ->
        val stream = streams[page]
        StreamCard(
            stream = stream,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onStreamClick(stream.streamerName) }
        )
    }
}

@Composable
fun StreamCard(
    stream: StreamUiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(stream.thumbnailUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(12.dp))
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    ViewerCountBadge(viewers = stream.viewers)
                }
            }

            Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                Text(
                    text = stream.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${stream.streamerName} • ${stream.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}