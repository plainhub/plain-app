package com.ismartcoding.plain.ui.page.audioplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.lib.extensions.formatDuration
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.WaveSlider
import kotlinx.coroutines.CoroutineScope

/**
 * Cover-view playback section: scrubber with time labels plus transport and
 * mode buttons. Collapses while the lyrics view is shown.
 */
@Composable
fun AudioPlayerBottomControls(
    visible: Boolean,
    progress: Float,
    duration: Float,
    isPlaying: Boolean,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    playMode: MediaPlayMode,
    playbackSpeed: Float,
    isTimerActive: Boolean,
    onSleepTimer: () -> Unit,
    onPlaylist: () -> Unit,
    onPlayPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    scope: CoroutineScope,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
        exit = shrinkVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)),
    ) {
        Column {
            VerticalSpace(8.dp)
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
                WaveSlider(
                    value = progress,
                    onValueChange = onScrub,
                    onValueChangeFinished = onScrubFinished,
                    valueRange = 0f..maxOf(duration, 1f),
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    isPlaying = isPlaying,
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = progress.toLong().formatDuration(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = duration.toLong().formatDuration(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            AudioPlayerControls(
                playMode = playMode,
                onPlayModeChange = {},
                isTimerActive = isTimerActive,
                onSleepTimer = onSleepTimer,
                onPlaylist = onPlaylist,
                isPlaying = isPlaying,
                scope = scope,
                onPlayPrevious = onPlayPrevious,
                onPlayPause = onPlayPause,
                onPlayNext = onPlayNext,
                speed = playbackSpeed,
            )
        }
    }
}
