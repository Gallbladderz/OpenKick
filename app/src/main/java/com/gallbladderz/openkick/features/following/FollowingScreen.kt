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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

val KickGreen = Color(0xFF53FC18)

@Composable
fun FollowingScreen(
    viewModel: FollowingViewModel = koinViewModel(),
    onManageClick: () -> Unit = {},
    onStreamerClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Отслеживаемое", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }

        when (val uiState = state) {
            is FollowingUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KickGreen)
                }
            }
            is FollowingUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is FollowingUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // УПРАВЛЕНИЕ ПОДПИСКАМИ
                    item {
                        ListItem(
                            headlineContent = { Text("Управление подписками", fontWeight = FontWeight.Bold) },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Управление") },
                            modifier = Modifier.clickable { onManageClick() }
                        )
                    }

                    // В ЭФИРЕ
                    item { SectionHeader("В эфире") }
                    item {
                        if (uiState.liveChannels.isEmpty()) {
                            Text(
                                text = "Сейчас никто не стримит",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.liveChannels) { channel ->
                                    LiveChannelItem(channel, onClick = { onStreamerClick(channel.slug) })
                                }
                            }
                        }
                    }

                    // ОТСЛЕЖИВАЕМЫЕ КАТЕГОРИИ
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    item { SectionHeader("Категории") }
                    item {
                        if (uiState.categories.isEmpty()) {
                            Text(
                                text = "Вы не отслеживаете категории",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        } else {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.categories) { category ->
                                    FollowedCategoryItem(category, onClick = { onCategoryClick(category.slug) })
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
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun LiveChannelItem(channel: LiveChannelUi, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(channel.avatarUrl).crossfade(true).build(),
            contentDescription = channel.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(2.dp, KickGreen, CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = channel.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FollowedCategoryItem(category: FollowedCategoryUi, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.width(100.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(category.bannerUrl).crossfade(true).build(),
                contentDescription = category.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = formatShortViewers(category.viewers),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatShortViewers(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0).replace(".0K", "K")
        else -> count.toString()
    }
}