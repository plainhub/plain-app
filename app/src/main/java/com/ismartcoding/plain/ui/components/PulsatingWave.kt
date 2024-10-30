package com.ismartcoding.plain.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun PulsatingWave(
    isPlaying: Boolean,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    if (!isPlaying) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "audio_playing_animation")
    val density = LocalDensity.current
    
    val scale1 = infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    
    val scale2 = infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    
    val scale3 = infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    
    Row(
        modifier = modifier.padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        val barWidth = with(density) { 3.dp.toPx() }
        val maxBarHeight = 16.dp
        
        Box(
            modifier = Modifier
                .width(with(density) { barWidth.toDp() })
                .height(with(density) { (maxBarHeight.toPx() * scale1.value).toDp() })
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
        
        Box(
            modifier = Modifier
                .width(with(density) { barWidth.toDp() })
                .height(with(density) { (maxBarHeight.toPx() * scale2.value).toDp() })
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
        
        Box(
            modifier = Modifier
                .width(with(density) { barWidth.toDp() })
                .height(with(density) { (maxBarHeight.toPx() * scale3.value).toDp() })
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
    }
} 