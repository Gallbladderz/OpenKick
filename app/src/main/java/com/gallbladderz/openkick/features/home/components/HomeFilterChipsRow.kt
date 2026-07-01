package com.gallbladderz.openkick.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gallbladderz.openkick.R
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path



@Composable
fun HomeFilterChipsRow(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    isGridMode: Boolean,
    onGridModeChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            val filters = listOf(stringResource(R.string.filter_all), stringResource(R.string.filter_categories), stringResource(R.string.filter_clips))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        label = { Text(filter, fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == filter,
                            borderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        if (selectedFilter == stringResource(R.string.filter_all)) {
            Box(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { onGridModeChange(!isGridMode) },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isGridMode) Icons.AutoMirrored.Filled.List else rememberGridViewIcon(),
                        contentDescription = stringResource(R.string.toggle_view),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun rememberGridViewIcon(): ImageVector {
    return remember {
        ImageVector.Builder(
            name = "GridView",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 3f)
                lineTo(11f, 3f)
                lineTo(11f, 11f)
                lineTo(3f, 11f)
                close()
                moveTo(13f, 3f)
                lineTo(21f, 3f)
                lineTo(21f, 11f)
                lineTo(13f, 11f)
                close()
                moveTo(3f, 13f)
                lineTo(11f, 13f)
                lineTo(11f, 21f)
                lineTo(3f, 21f)
                close()
                moveTo(13f, 13f)
                lineTo(21f, 13f)
                lineTo(21f, 21f)
                lineTo(13f, 21f)
                close()
            }
        }.build()
    }
}

