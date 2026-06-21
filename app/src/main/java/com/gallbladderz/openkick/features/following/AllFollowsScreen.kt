package com.gallbladderz.openkick.features.following

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFollowsScreen(
    viewModel: FollowingViewModel = koinViewModel(),
    onBackClick: () -> Unit,
    onStreamerClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val kickGradient = remember {
        Brush.linearGradient(colors = listOf(Color(0xFF53FC18), Color(0xFF13B500)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Все отслеживаемые", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val uiState = state) {
                is FollowingUiState.Success -> {
                    
                    val allStreamers = uiState.liveStreamers + uiState.offlineStreamers

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allStreamers, key = { it.slug }) { streamer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onStreamerClick(streamer.slug) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(streamer.avatarUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = streamer.username,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .border(
                                            width = if (streamer.isLive) 3.dp else 1.dp,
                                            brush = if (streamer.isLive) kickGradient else SolidColor(MaterialTheme.colorScheme.surfaceVariant),
                                            shape = CircleShape
                                        )
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = streamer.username,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (streamer.isLive) {
                                        Text(
                                            text = "В эфире",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }

                                
                                FilledTonalButton(
                                    onClick = { viewModel.unfollowStreamer(streamer.slug) },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                ) {
                                    Text("Отписаться")
                                }
                            }
                        }
                    }
                }
                else -> {} 
            }
        }
    }
}