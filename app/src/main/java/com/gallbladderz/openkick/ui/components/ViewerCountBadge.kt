package com.gallbladderz.openkick.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ViewerCountBadge(
    viewers: Int,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    fontSize: Int = 12
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.Red)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$viewers",
            color = textColor,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
