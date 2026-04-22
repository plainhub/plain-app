package com.ismartcoding.plain.ui.page.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.theme.greenDot
import com.ismartcoding.plain.ui.theme.greenPill
import com.ismartcoding.plain.ui.theme.greenText


@Composable
fun OnlineSessionsIndicator(count: Int, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    val progress1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "ring1",
    )
    val progress2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1800, 900, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "ring2",
    )

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.greenPill,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val dotColor = MaterialTheme.colorScheme.greenDot
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    val center = this.center
                    listOf(progress1, progress2).forEach { p ->
                        drawCircle(
                            color = dotColor.copy(alpha = (1f - p) * 0.5f),
                            radius = (size.minDimension / 2f) * p,
                            center = center,
                        )
                    }
                    drawCircle(
                        color = dotColor,
                        radius = size.minDimension * 0.22f,
                        center = center
                    )
                }
            }
            Text(
                text = stringResource(R.string.clients_online, count),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.greenText,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}