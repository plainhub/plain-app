package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CircularTimer(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
    trackColor: Color = Color.Gray.copy(alpha = 0.2f),
    progressColor: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val strokeWidthPx = strokeWidth.toPx()
        val radius = (size.minDimension - strokeWidthPx) / 2
        val centerOffset = Offset(size.width / 2f, size.height / 2f)

        // Background circle
        drawCircle(
            color = trackColor,
            radius = radius,
            center = centerOffset,
            style = Stroke(width = strokeWidthPx)
        )

        // Progress arc
        val sweepAngle = 360f * progress.coerceIn(0f, 1f)

        if (sweepAngle > 0) {
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round
                ),
                topLeft = Offset(
                    centerOffset.x - radius,
                    centerOffset.y - radius
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
        }
    }
} 