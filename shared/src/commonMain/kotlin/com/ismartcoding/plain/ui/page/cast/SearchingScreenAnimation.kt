package com.ismartcoding.plain.ui.page.cast

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

@Composable
fun SearchingScreenAnimation(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "cast_search")
    val dot1 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dot1",
    )
    val dot2 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(600),
        ),
        label = "dot2",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val monH = h * 0.55f
        val monW = monH * 1.6f
        val phoneH = h * 0.50f
        val phoneW = phoneH * 0.46f
        val gap = w * 0.12f
        val totalW = monW + gap + phoneW
        val startX = ((w - totalW) / 2f).coerceAtLeast(0f)

        val monLeft = startX
        val monTop = (h - monH) / 2f - h * 0.04f
        drawMonitor(monLeft, monTop, monW, monH)

        val phoneLeft = startX + monW + gap
        val phoneTop = (h - phoneH) / 2f
        drawPhone(phoneLeft, phoneTop, phoneW, phoneH)

        val dotStartX = phoneLeft
        val dotStartY = phoneTop + phoneH * 0.38f
        val dotEndX = monLeft + monW
        val dotEndY = monTop + monH * 0.50f

        for ((progress, baseAlpha) in listOf(dot1 to 1f, dot2 to 0.6f)) {
            val x = lerp(dotStartX, dotEndX, progress)
            val y = lerp(dotStartY, dotEndY, progress)
            val edgeFade = when {
                progress < 0.12f -> progress / 0.12f
                progress > 0.88f -> (1f - progress) / 0.12f
                else -> 1f
            }
            drawCircle(
                color = Color(0xFF1A73E8).copy(alpha = edgeFade * baseAlpha),
                radius = 6f,
                center = Offset(x, y),
            )
        }
    }
}


