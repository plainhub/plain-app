package com.ismartcoding.plain.ui.base

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

internal fun DrawScope.drawWaveContent(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    colors: WaveSliderColors,
    waveOptions: WaveOptions,
    isPlaying: Boolean,
    phaseShift: Float,
) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    val centerY = canvasHeight / 2

    val amplitude = waveOptions.amplitude.dp.toPx()
    val lineWidth = waveOptions.lineWidth.dp.toPx()
    val thumbRadius = waveOptions.thumbRadius.dp.toPx()

    val range = valueRange.endInclusive - valueRange.start
    val normalizedValue = (value - valueRange.start) / range
    val progressPosition = normalizedValue * canvasWidth

    drawLine(
        color = colors.inactiveColor,
        start = Offset(progressPosition, centerY),
        end = Offset(canvasWidth, centerY),
        strokeWidth = lineWidth,
        cap = StrokeCap.Round,
    )

    val activePath = Path()
    activePath.moveTo(0f, centerY)

    var x = 0f
    while (x <= progressPosition) {
        val waveY = if (isPlaying) {
            centerY + amplitude * sin(waveOptions.frequency * x + phaseShift)
        } else {
            centerY
        }
        activePath.lineTo(x, waveY)
        x += 2f
    }

    drawPath(
        path = activePath,
        color = colors.activeColor,
        style = Stroke(width = lineWidth, cap = StrokeCap.Round),
    )

    val thumbY = if (isPlaying) {
        centerY + amplitude * sin(waveOptions.frequency * progressPosition + phaseShift)
    } else {
        centerY
    }

    drawCircle(
        color = colors.thumbColor,
        radius = thumbRadius,
        center = Offset(progressPosition, thumbY),
    )
}
