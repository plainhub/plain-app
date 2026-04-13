package com.ismartcoding.plain.ui.page.dlna

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.formatMinSec
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.PlayerSlider

@Composable
fun AudioPlayerControls(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
        PlayerSlider(
            modifier = Modifier.fillMaxWidth().height(24.dp),
            progress = progress,
            bufferedProgress = 0f,
            onProgressChange = onSeek,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = positionMs.formatMinSec(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            Text(text = durationMs.formatMinSec(), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        }
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.15f),
            modifier = Modifier.size(72.dp),
        ) {
            IconButton(modifier = Modifier.size(72.dp), onClick = onPlayPause) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play_arrow),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    }
}
