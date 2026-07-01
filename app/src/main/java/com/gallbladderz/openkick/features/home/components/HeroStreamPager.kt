package com.gallbladderz.openkick.features.home.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gallbladderz.openkick.features.home.StreamUiModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroStreamPager(
    streams: List<StreamUiModel>,
    onStreamClick: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { streams.size })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp),
        pageSpacing = 12.dp
    ) { page ->
        val stream = streams[page]
        StreamCard(
            stream = stream,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onStreamClick(stream.streamerName) }
        )
    }
}

