package com.ismartcoding.plain.ui.page.audioplayer

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
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.platform.audioSetPlaybackSpeed
import com.ismartcoding.plain.ui.components.mediaviewer.PlaybackSpeedButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.list_music as ui_drawable_list_music
import com.ismartcoding.plain.ui.resources.pause as ui_drawable_pause
import com.ismartcoding.plain.ui.resources.play_arrow as ui_drawable_play_arrow
import com.ismartcoding.plain.ui.resources.repeat as ui_drawable_repeat
import com.ismartcoding.plain.ui.resources.repeat1 as ui_drawable_repeat1
import com.ismartcoding.plain.ui.resources.shuffle as ui_drawable_shuffle
import com.ismartcoding.plain.ui.resources.skip_next as ui_drawable_skip_next
import com.ismartcoding.plain.ui.resources.skip_previous as ui_drawable_skip_previous
import com.ismartcoding.plain.ui.resources.timer as ui_drawable_timer
import com.ismartcoding.plain.i18n.pause

@Composable
fun AudioPlayerControls(
    playMode: MediaPlayMode,
    onPlayModeChange: (MediaPlayMode) -> Unit,
    isTimerActive: Boolean,
    onSleepTimer: () -> Unit,
    onOpenQueue: () -> Unit,
    isPlaying: Boolean,
    onPlayPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    speed: Float,
    scope: CoroutineScope,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SpeedButton(speed = speed, scope = scope)
        IconButton(
            onClick = {
                scope.launch {
                    val nextMode = when (playMode) {
                        MediaPlayMode.REPEAT -> MediaPlayMode.REPEAT_ONE
                        MediaPlayMode.REPEAT_ONE -> MediaPlayMode.SHUFFLE
                        MediaPlayMode.SHUFFLE -> MediaPlayMode.REPEAT
                    }
                    UserPrefs.audioPlayMode.value = nextMode
                    onPlayModeChange(nextMode)
                }
            },
            modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Icon(
                painter = painterResource(when (playMode) {
                    MediaPlayMode.REPEAT -> UiRes.drawable.ui_drawable_repeat
                    MediaPlayMode.REPEAT_ONE -> UiRes.drawable.ui_drawable_repeat1
                    MediaPlayMode.SHUFFLE -> UiRes.drawable.ui_drawable_shuffle
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
                painter = painterResource(UiRes.drawable.ui_drawable_timer), contentDescription = "Sleep timer",
                tint = if (isTimerActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        IconButton(
            onClick = onOpenQueue,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Icon(painter = painterResource(UiRes.drawable.ui_drawable_list_music), contentDescription = "Queue", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPlayPrevious,
            modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Icon(painter = painterResource(UiRes.drawable.ui_drawable_skip_previous), contentDescription = "Previous", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
        }
        IconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(64.dp).clip(CircleShape)
                .background(if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary),
        ) {
            Icon(
                painter = painterResource(if (isPlaying) UiRes.drawable.ui_drawable_pause else UiRes.drawable.ui_drawable_play_arrow),
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(40.dp),
            )
        }
        IconButton(
            onClick = onPlayNext,
            modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Icon(painter = painterResource(UiRes.drawable.ui_drawable_skip_next), contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun SpeedButton(speed: Float, scope: CoroutineScope) {
    fun applySpeed(s: Float) {
        audioSetPlaybackSpeed(s)
        scope.launch { UserPrefs.audioPlaybackSpeed.value = s }
    }
    PlaybackSpeedButton(
        speed = speed,
        onSpeedChange = { applySpeed(it) },
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    )
}
