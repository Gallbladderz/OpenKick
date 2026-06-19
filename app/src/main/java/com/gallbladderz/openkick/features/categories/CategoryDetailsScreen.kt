package com.gallbladderz.openkick.features.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsScreen(
    slug: String,
    viewModel: CategoryDetailsViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Стримы", "Клипы")

    var isFollowed by remember { mutableStateOf(false) }

    LaunchedEffect(slug) {
        viewModel.loadCategory(slug)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state is CategoryDetailsUiState.Success)
                            (state as CategoryDetailsUiState.Success).name
                        else "Загрузка...",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
                        Text(currentState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is CategoryDetailsUiState.Success -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(currentState.bannerUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Category Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(80.dp)
                                .height(110.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${currentState.viewers} зрителей",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (currentState.tags.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(currentState.tags) { tag ->
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text(tag, fontSize = 10.sp) },
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            FilledTonalButton(
                                onClick = { isFollowed = !isFollowed },
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isFollowed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isFollowed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(if (isFollowed) "Отписаться" else "Отслеживать")
                            }
                        }
                    }

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

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (selectedTabIndex == 0) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Здесь скоро появятся стримы", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Здесь скоро будут клипы", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}