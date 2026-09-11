package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.red

/**
 * Red diagonal stripe across the top-left corner, marking debug builds.
 * Pure canvas, never intercepts touches.
 */
@Composable
fun DebugCornerBadge(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.red
    Canvas(modifier.size(48.dp)) {
        drawLine(
            color = color,
            start = Offset(0f, size.height),
            end = Offset(size.width, 0f),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}
