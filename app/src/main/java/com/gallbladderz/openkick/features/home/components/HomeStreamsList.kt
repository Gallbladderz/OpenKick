/*
 * SPDX-FileCopyrightText: 2026 Gallbladderz
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gallbladderz.openkick.features.home.StreamUiModel

fun LazyListScope.homeStreamsList(
    streams: List<StreamUiModel>,
    isGridMode: Boolean,
    liveFilterText: String,
    onStreamClick: (String) -> Unit
) {
    val heroStreams = streams.take(5)
    val feedStreams = streams.drop(5)

    if (heroStreams.isNotEmpty()) {
        item {
            HeroStreamPager(
                streams = heroStreams,
                onStreamClick = onStreamClick
            )
        }
    }

    if (feedStreams.isNotEmpty()) {
        item {
            Text(
                text = liveFilterText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    if (isGridMode) {
        val streamRows = feedStreams.chunked(2)

        itemsIndexed(streamRows) { _, rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StreamCard(
                    stream = rowItems[0],
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onStreamClick(rowItems[0].streamerName)
                    }
                )

                if (rowItems.size > 1) {
                    StreamCard(
                        stream = rowItems[1],
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onStreamClick(rowItems[1].streamerName)
                        }
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

    } else {
        itemsIndexed(
            feedStreams,
            key = { _, it -> it.id }
        ) { _, stream ->
            StreamCard(
                stream = stream,
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
                    onStreamClick(stream.streamerName)
                }
            )
        }
    }
}
