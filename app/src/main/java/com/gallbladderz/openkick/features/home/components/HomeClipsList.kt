package com.gallbladderz.openkick.features.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gallbladderz.openkick.R
import com.gallbladderz.openkick.features.home.ClipUiModel
import com.gallbladderz.openkick.ui.components.ClipCard

fun LazyListScope.homeClipsList(
    clips: List<ClipUiModel>,
    onClipClick: (ClipUiModel) -> Unit
) {
    item {
        Text(
            text = stringResource(R.string.top_clips_week),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    val clipRows = clips.chunked(2)

    itemsIndexed(clipRows) { _, rowItems ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ClipCard(
                clip = rowItems[0],
                modifier = Modifier.weight(1f),
                onClick = { onClipClick(rowItems[0]) }
            )

            if (rowItems.size > 1) {
                ClipCard(
                    clip = rowItems[1],
                    modifier = Modifier.weight(1f),
                    onClick = { onClipClick(rowItems[1]) }
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
