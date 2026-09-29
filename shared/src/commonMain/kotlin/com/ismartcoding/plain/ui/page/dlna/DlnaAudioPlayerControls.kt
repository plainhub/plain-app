package com.ismartcoding.plain.ui.page.dlna

import com.ismartcoding.plain.i18n.*
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
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.extensions.formatMinSec
import com.ismartcoding.plain.ui.base.PlayerSlider
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.pause as ui_drawable_pause
import com.ismartcoding.plain.ui.resources.play_arrow as ui_drawable_play_arrow
import com.ismartcoding.plain.i18n.pause

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
                    painter = painterResource(if (isPlaying) UiRes.drawable.ui_drawable_pause else UiRes.drawable.ui_drawable_play_arrow),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    }
}
