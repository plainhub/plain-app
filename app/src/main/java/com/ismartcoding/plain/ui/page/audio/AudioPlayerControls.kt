package com.ismartcoding.plain.ui.page.audio
import com.ismartcoding.plain.preferences.*

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.audio.AudioPlayer
import com.ismartcoding.plain.preferences.AudioPlayModePreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AudioPlayerControls(
    playMode: MediaPlayMode,
    onPlayModeChange: (MediaPlayMode) -> Unit,
    isTimerActive: Boolean,
    onSleepTimer: () -> Unit,
    onPlaylist: () -> Unit,
    isPlaying: Boolean,
    scope: CoroutineScope,
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = {
                scope.launch {
                    val nextMode = when (playMode) {
                        MediaPlayMode.REPEAT -> MediaPlayMode.REPEAT_ONE
                        MediaPlayMode.REPEAT_ONE -> MediaPlayMode.SHUFFLE
                        MediaPlayMode.SHUFFLE -> MediaPlayMode.REPEAT
                    }
                    TempData.audioPlayMode.value = nextMode
                    onPlayModeChange(nextMode)
                    AudioPlayModePreference.putAsync(nextMode)
                }
            },
            modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Icon(
                painter = painterResource(when (playMode) {
                    MediaPlayMode.REPEAT -> Res.drawable.repeat
                    MediaPlayMode.REPEAT_ONE -> Res.drawable.repeat1
                    MediaPlayMode.SHUFFLE -> Res.drawable.shuffle
                }),
                contentDescription = "Play mode",
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp),
            )
        }
        IconButton(
            onClick = onSleepTimer,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(
                if (isTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Icon(
                painter = painterResource(Res.drawable.timer), contentDescription = "Sleep timer",
                tint = if (isTimerActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        IconButton(
            onClick = onPlaylist,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Icon(painter = painterResource(Res.drawable.list_music), contentDescription = "Playlist", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { AudioPlayer.skipToPrevious() },
            modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Icon(painter = painterResource(Res.drawable.skip_previous), contentDescription = "Previous", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
        }
        IconButton(
            onClick = { if (isPlaying) AudioPlayer.pause() else AudioPlayer.play() },
            modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
        ) {
            Icon(
                painter = painterResource(if (isPlaying) Res.drawable.pause else Res.drawable.play_arrow),
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(48.dp),
            )
        }
        IconButton(
            onClick = { AudioPlayer.skipToNext() },
            modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Icon(painter = painterResource(Res.drawable.skip_next), contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
        }
    }
}
