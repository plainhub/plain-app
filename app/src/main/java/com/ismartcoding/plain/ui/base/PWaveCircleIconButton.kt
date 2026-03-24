package com.ismartcoding.plain.ui.base

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PWaveCircleIconButton(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 128.dp,
    iconSize: Dp = 64.dp,
    buttonColor: Color,
    iconTint: Color,
    waveColor: Color,
    waveEnabled: Boolean = true,
) {
    val maxScale = 2.1f
    val transition = rememberInfiniteTransition(label = "wave-circle")
    val wave1 = transition.animateFloat(
        initialValue = 1f,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave-1",
    )
    val wave2 = transition.animateFloat(
        initialValue = 1f,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, delayMillis = 730, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave-2",
    )
    val wave3 = transition.animateFloat(
        initialValue = 1f,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, delayMillis = 1460, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave-3",
    )

    Box(
        modifier = modifier.size(buttonSize * maxScale),
        contentAlignment = Alignment.Center,
    ) {
        if (waveEnabled) {
            WaveLayer(size = buttonSize, color = waveColor, scale = wave1.value, maxScale = maxScale)
            WaveLayer(size = buttonSize, color = waveColor, scale = wave2.value, maxScale = maxScale)
            WaveLayer(size = buttonSize, color = waveColor, scale = wave3.value, maxScale = maxScale)
        }
        Button(
            onClick = onClick,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = iconTint,
                disabledContainerColor = buttonColor.copy(alpha = 0.12f),
                disabledContentColor = iconTint.copy(alpha = 0.38f),
            ),
            modifier = Modifier.size(buttonSize),
        ) {
            Icon(
                painter = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
private fun WaveLayer(size: Dp, color: Color, scale: Float, maxScale: Float) {
    val progress = ((scale - 1f) / (maxScale - 1f)).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = (1f - progress) * 0.35f
            }
            .background(color = color, shape = CircleShape),
    )
}