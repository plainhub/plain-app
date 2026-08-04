package com.ismartcoding.plain.ui.components.mediaviewer.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.exit_fullscreen
import com.ismartcoding.plain.i18n.fullscreen
import com.ismartcoding.plain.i18n.fullscreen_exit
import com.ismartcoding.plain.i18n.mute
import com.ismartcoding.plain.i18n.pause
import com.ismartcoding.plain.i18n.picture_in_picture
import com.ismartcoding.plain.i18n.pip
import com.ismartcoding.plain.i18n.play
import com.ismartcoding.plain.i18n.play_arrow
import com.ismartcoding.plain.i18n.unmute
import com.ismartcoding.plain.i18n.volume_2
import com.ismartcoding.plain.i18n.volume_x
import com.ismartcoding.plain.ui.base.ControlChip
import com.ismartcoding.plain.ui.base.ControlChipIconButton
import com.ismartcoding.plain.ui.components.mediaviewer.PlaybackSpeedButton
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun VideoControlsBar(
    isPlaying: Boolean,
    isMuted: Boolean,
    isFullscreen: Boolean,
    playbackSpeed: Float,
    showPip: Boolean,
    onSpeedChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    onPlayPause: () -> Unit,
    onPip: () -> Unit,
    onFullscreenToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // [Speed]
        ControlChip {
            PlaybackSpeedButton(
                speed = playbackSpeed,
                onSpeedChange = onSpeedChange,
                tint = Color.White,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // [Sound]
        ControlChipIconButton(
            if (isMuted) Res.drawable.volume_x else Res.drawable.volume_2,
            contentDescription = stringResource(if (isMuted) Res.string.unmute else Res.string.mute),
            click = onMuteToggle
        )

        // [Play] — big circular button
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(60.dp),
        ) {
            IconButton(onClick = onPlayPause, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    painter = painterResource(if (isPlaying) Res.drawable.pause else Res.drawable.play_arrow),
                    contentDescription = stringResource(if (isPlaying) Res.string.pause else Res.string.play),
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        // [Pip] — hidden on iOS (showPip = false)
        if (showPip) {
            ControlChipIconButton(
                Res.drawable.pip,
                contentDescription = stringResource(Res.string.picture_in_picture),
                click = onPip
            )
        }

        // [Fullscreen]
        ControlChipIconButton(
            if (isFullscreen) Res.drawable.fullscreen_exit else Res.drawable.fullscreen,
            contentDescription = stringResource(if (isFullscreen) Res.string.exit_fullscreen else Res.string.fullscreen),
            click = onFullscreenToggle
        )
    }
}
