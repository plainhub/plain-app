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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.theme.waveActiveColor
import com.ismartcoding.plain.ui.theme.waveInactiveColor
import com.ismartcoding.plain.ui.theme.waveThumbColor
import kotlin.math.sin

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

/**
 * A customizable wave slider component with animated wave effect
 * 
 * @param value Current value of the slider
 * @param onValueChange Callback invoked when slider value changes
 * @param valueRange Range of values for the slider
 * @param onValueChangeFinished Callback invoked when value change is finished
 * @param colors Color configuration for the slider
 * @param waveOptions Customization options for the wave appearance
 * @param modifier Modifier for the slider
 * @param enabled Whether the slider is enabled
 * @param isPlaying Whether the audio is playing (controls animation)
 */
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
    // Track if we are currently dragging
    var isDragging by remember { mutableStateOf(false) }
    
    // Animation parameters
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
        // WaveSlider visualization
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2
            
            // Convert dp values to pixels
            val amplitude = waveOptions.amplitude.dp.toPx()
            val lineWidth = waveOptions.lineWidth.dp.toPx()
            val thumbRadius = waveOptions.thumbRadius.dp.toPx()
            
            // Calculate the phase shift for animation (only when playing)
            val phaseShift = if (isPlaying) {
                animationOffset.value * 2 * Math.PI.toFloat()
            } else {
                0f
            }
            
            // Map the value to canvas width
            val range = valueRange.endInclusive - valueRange.start
            val normalizedValue = (value - valueRange.start) / range
            val progressPosition = normalizedValue * canvasWidth
            
            // Draw the inactive straight line from progress to end
            drawLine(
                color = colors.inactiveColor,
                start = Offset(progressPosition, centerY),
                end = Offset(canvasWidth, centerY),
                strokeWidth = lineWidth,
                cap = StrokeCap.Round
            )
            
            // Create active path (wavy)
            val activePath = Path()
            activePath.moveTo(0f, centerY)
            
            // Generate active wave path (left of the thumb)
            var x = 0f
            while (x <= progressPosition) {
                val waveY = if (isPlaying) {
                    centerY + amplitude * sin(waveOptions.frequency * x + phaseShift)
                } else {
                    centerY
                }
                activePath.lineTo(x, waveY)
                x += 2f // Small step for smooth wave
            }
            
            // Draw the active wavy path
            drawPath(
                path = activePath,
                color = colors.activeColor,
                style = Stroke(
                    width = lineWidth,
                    cap = StrokeCap.Round
                )
            )
            
            // Calculate the Y position for thumb based on the wave
            val thumbY = if (isPlaying) {
                centerY + amplitude * sin(waveOptions.frequency * progressPosition + phaseShift)
            } else {
                centerY
            }
            
            // Draw the thumb exactly on the wave
            drawCircle(
                color = colors.thumbColor,
                radius = thumbRadius,
                center = Offset(progressPosition, thumbY)
            )
        }
        
        // Invisible slider for touch interaction
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