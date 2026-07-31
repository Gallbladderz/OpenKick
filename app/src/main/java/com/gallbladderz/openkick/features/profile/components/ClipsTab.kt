package com.gallbladderz.openkick.features.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.home.ClipUiModel
import com.gallbladderz.openkick.ui.components.ClipCard

@Composable
fun ClipsTab(
    clips: List<ClipUiModel>,
    onClipClick: (ClipUiModel) -> Unit,
    onLoadMore: () -> Unit
) {
    if (clips.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.no_clips),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisible ->
                val totalItems = listState.layoutInfo.totalItemsCount
                if (lastVisible != null && totalItems > 3 && lastVisible >= totalItems - 2) {
                    onLoadMore()
                }
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val clipRows = clips.chunked(2)
        items(clipRows) { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ClipCard(
                    clip = rowItems[0],
                    modifier = Modifier.weight(1f),
                    onClick = { onClipClick(rowItems[0]) })
                if (rowItems.size > 1) {
                    ClipCard(
                        clip = rowItems[1],
                        modifier = Modifier.weight(1f),
                        onClick = { onClipClick(rowItems[1]) })
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}