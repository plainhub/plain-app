package com.ismartcoding.plain.ui.base

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.ui.theme.waveActiveColor
import com.ismartcoding.plain.ui.theme.waveInactiveColor
import com.ismartcoding.plain.ui.theme.waveThumbColor

data class WaveOptions(
    val amplitude: Float = 6f,
    val frequency: Float = 0.12f,
    val lineWidth: Float = 3f,
    val thumbRadius: Float = 5f,
    val animationDuration: Int = 2000
)

data class WaveSliderColors(
    val activeColor: Color,
    val inactiveColor: Color,
    val thumbColor: Color
)

@Composable
fun WaveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: WaveSliderColors = WaveSliderColors(
        activeColor = MaterialTheme.colorScheme.waveActiveColor,
        inactiveColor = MaterialTheme.colorScheme.waveInactiveColor,
        thumbColor = MaterialTheme.colorScheme.waveThumbColor
    ),
    waveOptions: WaveOptions = WaveOptions(),
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPlaying: Boolean = true
) {
    var isDragging by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "waveAnimation")
    val animationOffset = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(waveOptions.animationDuration, easing = LinearEasing)
        ),
        label = "waveOffset"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val phaseShift = if (isPlaying) {
                animationOffset.value * 2 * Math.PI.toFloat()
            } else {
                0f
            }
            drawWaveContent(value, valueRange, colors, waveOptions, isPlaying, phaseShift)
        }

        Slider(
            value = value,
            onValueChange = {
                isDragging = true
                onValueChange(it)
            },
            onValueChangeFinished = {
                isDragging = false
                onValueChangeFinished?.invoke()
            },
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.Transparent,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            ),
            enabled = enabled,
            modifier = Modifier.fillMaxSize()
        )
    }
} 