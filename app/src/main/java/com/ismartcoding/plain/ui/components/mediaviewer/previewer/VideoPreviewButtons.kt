package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState

data class PlaybackSpeed(val speed: Float, val label: String)

@Composable
fun VideoButtons1(context: Context, videoState: VideoState) {
    var showSpeedMenu by rememberSaveable { mutableStateOf(false) }
    val playbackSpeeds = remember {
        listOf(PlaybackSpeed(0.25f, "0.25x"), PlaybackSpeed(0.5f, "0.5x"), PlaybackSpeed(1f, "1x"), PlaybackSpeed(2f, "2x"), PlaybackSpeed(3f, "3x"))
    }
    fun setSpeed(speed: Float) { videoState.changeSpeed(speed); showSpeedMenu = false }

    Box(contentAlignment = Alignment.TopEnd) {
        DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
            playbackSpeeds.forEach { speed ->
                DropdownMenuItem(modifier = Modifier.padding(end = 16.dp), onClick = { setSpeed(speed.speed) },
                    leadingIcon = { RadioButton(selected = videoState.speed == speed.speed, onClick = { setSpeed(speed.speed) }) },
                    text = { Text(text = speed.label) })
            }
        }
        IconButton(onClick = { showSpeedMenu = !showSpeedMenu }) {
            Icon(painter = painterResource(R.drawable.gauge), tint = Color.White, contentDescription = stringResource(R.string.change_playback_speed))
        }
    }
    IconButton(onClick = { videoState.toggleMute() }) {
        Icon(painter = painterResource(if (videoState.isMuted) R.drawable.volume_x else R.drawable.volume_2), tint = Color.White, contentDescription = stringResource(R.string.toggle_audio))
    }
    if (videoState.hasPipMode(context)) {
        IconButton(onClick = { videoState.enterPipMode(context) }) {
            Icon(painter = painterResource(id = R.drawable.pip), tint = Color.White, contentDescription = stringResource(R.string.picture_in_picture))
        }
    }
}
