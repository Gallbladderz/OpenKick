package com.gallbladderz.openkick.features.profile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.home.ClipUiModel
import com.gallbladderz.openkick.ui.components.ClipCard

@Composable
fun ClipsTab(clips: List<ClipUiModel>) {
    if (clips.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_clips), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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