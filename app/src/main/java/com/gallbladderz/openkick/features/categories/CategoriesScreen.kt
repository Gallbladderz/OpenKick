package com.gallbladderz.openkick.features.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = koinViewModel(),
    onCategoryClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Категории",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }

        when (val uiState = state) {
            is CategoriesUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is CategoriesUiState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.categories, key = { it.id }) { category ->
                        CategoryCard(
                            category = category,
                            viewModel = viewModel,
                            onClick = { onCategoryClick(category.slug) }
                        )
                    }
                }
            }

            is CategoriesUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(uiState.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun CategoryCard(
    category: CategoryUiModel,
    viewModel: CategoriesViewModel,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val isFollowed by viewModel.isCategoryFollowed(category.slug).collectAsStateWithLifecycle(initialValue = false)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(category.bannerUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = category.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatViewers(category.viewers),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(
                onClick = { viewModel.toggleCategoryFollow(category.slug, isFollowed) }, // 🔥 ПИШЕМ В БАЗУ ДАННЫХ ПРИ КЛИКЕ
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFollowed) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Отслеживать",
                    tint = if (isFollowed) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = category.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            category.tags.take(2).forEach { tag ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

fun formatViewers(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0).replace(".0K", "K")
        else -> count.toString()
    }
}