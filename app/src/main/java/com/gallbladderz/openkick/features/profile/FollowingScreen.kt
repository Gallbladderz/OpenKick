package com.gallbladderz.openkick.features.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel

@Composable
fun FollowingScreen(
    viewModel: FollowingViewModel = koinViewModel(),
    onNavigateToAllFollows: () -> Unit,
    onStreamerClick: (String) -> Unit
) {
    val streamers by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(top = 16.dp)) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToAllFollows() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Все подписки",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Смотреть все",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isLoading && streamers.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
        } else if (streamers.isEmpty()) {
            Text(
                text = "Ты пока ни на кого не подписан.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(streamers, key = { it.slug }) { streamer ->
                    FollowedAvatarItem(streamer = streamer, onClick = { onStreamerClick(streamer.slug) })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Записи",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun FollowedAvatarItem(streamer: FollowedStreamer, onClick: () -> Unit) {
    val context = LocalContext.current
    val fallbackAvatar = "https://ui-avatars.com/api/?name=${streamer.slug}&background=random"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.width(64.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(streamer.avatarUrl.ifEmpty { fallbackAvatar })
                .crossfade(true)
                .build(),
            contentDescription = streamer.slug,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .then(
                    if (streamer.isLive) Modifier.border(2.dp, Color.Red, CircleShape)
                    else Modifier
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = streamer.slug,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}