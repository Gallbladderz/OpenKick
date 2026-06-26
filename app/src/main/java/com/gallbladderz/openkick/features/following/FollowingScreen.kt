package com.gallbladderz.openkick.features.following

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.core.ui.components.ViewerCountBadge
import org.koin.androidx.compose.koinViewModel
import java.util.Locale
import androidx.compose.ui.draw.clipToBounds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowingScreen(
    viewModel: FollowingViewModel = koinViewModel(),
    onManageClick: () -> Unit = {},
    onStreamerClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

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

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.followed_streams),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) { paddingValues ->

        // Вешаем nestedScroll на основной Box экрана
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clipToBounds()
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            when (val uiState = state) {
                is FollowingUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is FollowingUiState.Error -> {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is FollowingUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {

                        val allStreamers = uiState.liveStreamers + uiState.offlineStreamers
                        if (allStreamers.isNotEmpty()) {
                            item {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.channels),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(R.string.all_followed_arrow),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable { onManageClick() }
                                        )
                                    }

                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(allStreamers, key = { it.slug }) { streamer ->
                                            StoryAvatarItem(
                                                streamer = streamer,
                                                onClick = { onStreamerClick(streamer.slug) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.categories.isNotEmpty()) {
                            item {
                                Column {
                                    Text(
                                        text = stringResource(R.string.categories),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .padding(bottom = 12.dp)
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(uiState.categories, key = { it.slug }) { category ->
                                            FollowedCategoryItem(category, onClick = { onCategoryClick(category.slug) })
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.liveStreamers.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.live),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 12.dp)
                                )
                            }

                            items(uiState.liveStreamers, key = { "live_${it.slug}" }) { streamer ->
                                LiveStreamCard(
                                    streamer = streamer,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    onClick = { onStreamerClick(streamer.slug) }
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        if (allStreamers.isEmpty() && uiState.categories.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.empty_follow_someone),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Сам круглешок обновления поверх всего
            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StoryAvatarItem(streamer: FollowedStreamerUi, onClick: () -> Unit) {
    val context = LocalContext.current
    val kickGradient = remember { Brush.linearGradient(colors = listOf(Color(0xFF53FC18), Color(0xFF13B500))) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(streamer.avatarUrl).crossfade(true).build(),
            contentDescription = streamer.username,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .border(
                    width = if (streamer.isLive) 3.dp else 1.dp,
                    brush = if (streamer.isLive) kickGradient else SolidColor(MaterialTheme.colorScheme.surfaceVariant),
                    shape = CircleShape
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = streamer.username,
            style = MaterialTheme.typography.labelSmall,
            color = if (streamer.isLive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (streamer.isLive) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LiveStreamCard(streamer: FollowedStreamerUi, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    ElevatedCard(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(streamer.streamThumbnailUrl.ifEmpty { streamer.avatarUrl }).crossfade(true).build(),
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))
                )
                Box(
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 4.dp)
                ) { ViewerCountBadge(viewers = streamer.viewers) }
            }
            Row(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).fillMaxWidth(), verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(streamer.avatarUrl).crossfade(true).build(),
                    contentDescription = streamer.username,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = streamer.username, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = streamer.streamTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = streamer.categoryName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

@Composable
private fun FollowedCategoryItem(category: FollowedCategoryUi, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.width(100.dp).clickable { onClick() }) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            AsyncImage(model = ImageRequest.Builder(context).data(category.bannerUrl).crossfade(true).build(), contentDescription = category.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                Text(text = formatShortViewers(category.viewers), color = Color.White, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = category.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatShortViewers(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0).replace(".0K", "K")
        else -> count.toString()
    }
}