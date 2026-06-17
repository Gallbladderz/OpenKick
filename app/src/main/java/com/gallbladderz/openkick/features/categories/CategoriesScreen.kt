package com.gallbladderz.openkick.features.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.gallbladderz.openkick.core.network.KickApiConstants

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = koinViewModel(),
    onCategoryClick: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val uiState = state) {
            is CategoriesUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is CategoriesUiState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.categories, key = { it.id }) { category ->
                        CategoryCard(category = category, onClick = { onCategoryClick(category.name) })
                    }
                }
            }
            is CategoriesUiState.Error -> {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.fetchCategories() }) { Text("Повторить") }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(category: CategoryUiModel, onClick: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Card(
            modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(category.bannerUrl)
                    .addHeader("User-Agent", KickApiConstants.USER_AGENT)
                    .crossfade(true)
                    .build(),
                contentDescription = category.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(text = "👁 ${category.viewers}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}