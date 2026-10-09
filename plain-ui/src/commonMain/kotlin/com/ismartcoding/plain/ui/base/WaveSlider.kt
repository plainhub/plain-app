package com.ismartcoding.plain.ui.base

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.waveInactiveColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: WaveSliderColors = WaveSliderColors(
        activeColor = MaterialTheme.colorScheme.primary,
        inactiveColor = MaterialTheme.colorScheme.waveInactiveColor,
        thumbColor = MaterialTheme.colorScheme.primary
    ),
    waveOptions: WaveOptions = WaveOptions(),
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPlaying: Boolean = true
) {
    require(valueRange.start.isFinite() && valueRange.endInclusive.isFinite() && valueRange.start <= valueRange.endInclusive)
    require(waveOptions.animationDurationMs > 0)
    val animated = isPlaying && enabled
    val animationOffset = if (animated) {
        rememberInfiniteTransition(label = "waveAnimation").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(waveOptions.animationDurationMs, easing = LinearEasing)),
            label = "waveOffset",
        )
    } else null
    val path = remember { androidx.compose.ui.graphics.Path() }
    Slider(
        value = if (value.isFinite()) value.coerceIn(valueRange) else valueRange.start,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        enabled = enabled && valueRange.start < valueRange.endInclusive,
        modifier = modifier,
        thumb = { Box(Modifier.size(waveOptions.thumbRadius.dp * 2)) },
        track = { slider ->
            Canvas(Modifier.fillMaxWidth().height((waveOptions.amplitude * 2 + waveOptions.lineWidth).dp)) {
                val phase = (animationOffset?.value ?: 0f) * 2 * kotlin.math.PI.toFloat()
                val effectiveColors = if (enabled) colors else colors.copy(
                    activeColor = colors.activeColor.copy(alpha = 0.38f),
                    thumbColor = colors.thumbColor.copy(alpha = 0.38f),
                )
                if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) {
                    scale(-1f, 1f) { drawWaveContent(slider.value, valueRange, effectiveColors, waveOptions, animated, phase, path) }
                } else {
                    drawWaveContent(slider.value, valueRange, effectiveColors, waveOptions, animated, phase, path)
                }
            }
        },
    )
}
