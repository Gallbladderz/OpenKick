package com.gallbladderz.openkick.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.gallbladderz.openkick.features.home.ClipUiModel
import com.gallbladderz.openkick.ui.components.ClipCard
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamerProfileScreen(
    slug: String,
    onBackClick: () -> Unit,
    onVideoClick: (String) -> Unit,
    viewModel: StreamerProfileViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Описание", "Записи", "Клипы")

    LaunchedEffect(slug) {
        viewModel.loadProfile(slug)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state is ProfileUiState.Success) (state as ProfileUiState.Success).info.username else "Профиль", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val uiState = state) {
                is ProfileUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
                }
                is ProfileUiState.Error -> {
                    Text(uiState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                }
                is ProfileUiState.Success -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ProfileHeader(info = uiState.info)

                        PrimaryTabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = { Text(title, fontWeight = FontWeight.Medium) }
                                )
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
                            when (selectedTabIndex) {
                                0 -> BioTab(uiState.info.bio)
                                1 -> VideosTab(uiState.videos, onVideoClick)
                                2 -> ClipsTab(uiState.clips)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(info: ProfileInfoUi) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(info.bannerUrl).crossfade(true).build(),
            contentDescription = "Banner",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.surfaceVariant)
        )
        AsyncImage(
            model = ImageRequest.Builder(context).data(info.avatarUrl).crossfade(true).build(),
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(start = 16.dp)
                .size(80.dp)
                .align(Alignment.BottomStart)
                .clip(CircleShape)
                .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = info.username, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(text = "${info.followers} фолловеров", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun BioTab(bio: String) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (bio.isBlank()) {
            Text("Стример ничего о себе не написал...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(text = bio, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun VideosTab(videos: List<VideoUiModel>, onVideoClick: (String) -> Unit) {
    val context = LocalContext.current
    if (videos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Записей нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(videos) { video ->
            Row(modifier = Modifier.fillMaxWidth().clickable { onVideoClick(video.id) }) {
                Box(modifier = Modifier.width(160.dp).aspectRatio(16f / 9f)) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(video.thumbnailUrl).crossfade(true).build(),
                        contentDescription = "VOD",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface)
                    )
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Text(text = video.durationFormatted, style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = video.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "👁 ${video.views}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ClipsTab(clips: List<ClipUiModel>) {
    if (clips.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Клипов нет", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val clipRows = clips.chunked(2)
        items(clipRows) { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ClipCard(clip = rowItems[0], modifier = Modifier.weight(1f), onClick = { })
                if (rowItems.size > 1) {
                    ClipCard(clip = rowItems[1], modifier = Modifier.weight(1f), onClick = { })
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}