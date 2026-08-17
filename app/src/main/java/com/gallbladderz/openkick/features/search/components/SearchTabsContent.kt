/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.gallbladderz.openkick.features.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.categories.CategoryCard
import com.gallbladderz.openkick.features.categories.CategoryUiModel
import com.gallbladderz.openkick.features.search.SearchCategoryUiModel
import com.gallbladderz.openkick.features.search.SearchStreamUiModel
import com.gallbladderz.openkick.features.search.SearchUiModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.grid.items as gridItems

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTabsContent(
    channels: List<SearchUiModel>,
    streams: List<SearchStreamUiModel>,
    categories: List<SearchCategoryUiModel>,
    onChannelClick: (String, Boolean) -> Unit,
    onStreamClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val titles = listOf(
        stringResource(R.string.tab_channels),
        stringResource(R.string.tab_streams),
        stringResource(R.string.tab_categories)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(text = title, overflow = TextOverflow.Ellipsis, maxLines = 1) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(channels) { channel ->
                            SearchChannelCard(
                                channel = channel,
                                onClick = { onChannelClick(channel.username, channel.isLive) }
                            )
                        }
                    }
                }

                1 -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val streamRows = streams.chunked(2)
                        itemsIndexed(streamRows) { _, rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SearchStreamCard(
                                    stream = rowItems[0],
                                    modifier = Modifier.weight(1f),
                                    onClick = { onStreamClick(rowItems[0].slug) }
                                )
                                if (rowItems.size > 1) {
                                    SearchStreamCard(
                                        stream = rowItems[1],
                                        modifier = Modifier.weight(1f),
                                        onClick = { onStreamClick(rowItems[1].slug) }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                2 -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        gridItems(categories, key = { it.slug }) { category ->
                            val categoryModel = CategoryUiModel(
                                id = category.slug.hashCode().toString(),
                                name = category.name,
                                slug = category.slug,
                                bannerUrl = category.thumbnailUrl,
                                viewers = category.viewers,
                                tags = category.tags
                            )
                            CategoryCard(
                                category = categoryModel,
                                isFollowed = false,
                                onToggleFollow = { },
                                onClick = { onCategoryClick(category.slug) }
                            )
                        }
                    }
                }
            }
        }
    }
}
