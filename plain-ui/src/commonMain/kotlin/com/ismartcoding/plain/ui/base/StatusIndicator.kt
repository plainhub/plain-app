package com.ismartcoding.plain.ui.base

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StatusIndicator(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pillColor: Color,
    dotColor: Color,
    textColor: Color,
    animationEnabled: Boolean = true,
    contentPaddingHorizontal: Dp = 12.dp,
    contentPaddingVertical: Dp = 6.dp,
    textStartPadding: Dp = 6.dp,
) {
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        color = pillColor,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = contentPaddingHorizontal,
                vertical = contentPaddingVertical,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                if (animationEnabled) {
                    StatusRipples(dotColor)
                }
                Canvas(modifier = Modifier.size(20.dp)) {
                    val center = this.center
                    drawCircle(
                        color = dotColor,
                        radius = size.minDimension * 0.22f,
                        center = center,
                    )
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
                modifier = Modifier.padding(start = textStartPadding),
            )
        }
    }
}

@Composable
private fun StatusRipples(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "statusRipple")
    val progress1 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "ring1",
    )
    val progress2 = infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1800, 900, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "ring2",
    )

    Canvas(Modifier.size(20.dp)) {
        fun ring(progress: Float) {
            drawCircle(color.copy(alpha = (1f - progress) * 0.5f), size.minDimension / 2f * progress)
        }
        ring(progress1.value)
        ring(progress2.value)
    }
}
