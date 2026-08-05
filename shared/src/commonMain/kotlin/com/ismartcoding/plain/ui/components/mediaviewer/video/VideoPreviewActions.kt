package com.ismartcoding.plain.ui.components.mediaviewer.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.ellipsis
import com.ismartcoding.plain.i18n.more_info
import com.ismartcoding.plain.lib.extensions.formatMinSec
import com.ismartcoding.plain.platform.hasPipMode
import com.ismartcoding.plain.ui.base.PlayerSlider
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.theme.darkMask
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/**
 * Shared bottom controls for the non-fullscreen video preview in
 * [com.ismartcoding.plain.platform.MediaPreviewer].
 *
 * Layout (bottom-aligned):
 *   Row 1: progress slider + time labels + More button (right)
 *   Row 2: [VideoControlsBar] = [Speed][Sound][Play][Pip][Fullscreen]
 *
 * Share / Cast / Save buttons have been removed — Share is in
 * FileInfoBottomSheet, Cast is being moved there too. The More button
 * opens FileInfoBottomSheet via [state.showMediaInfo].
 *
 * The fullscreen button only toggles [VideoState.isFullscreenMode]; the
 * actual landscape + system-bar switching is handled by the platform
 * [VideoPlayerSurface] actual via direct side effects (no Dialog overlay).
 */
@Composable
fun VideoPreviewActions(
    m: PreviewItem,
    state: MediaPreviewerState,
) {
    val videoState = state.videoState
    if (!state.showActions || videoState.enablePip) return
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        while (true) {
            scope.launch { state.videoState.updateTime() }
            delay(1.seconds)
        }
    }

    val sliderProgress = if (videoState.totalTime <= 0L) 0f
    else (videoState.currentTime.toFloat() / videoState.totalTime.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = Modifier.fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 32.dp)
            .alpha(state.uiAlpha.value),
    ) {
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End,
        ) {
            // Row 1: progress slider + time + More button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    modifier = Modifier.width(52.dp),
                    text = videoState.currentTime.formatMinSec(),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    PlayerSlider(
                        modifier = Modifier.fillMaxWidth().height(20.dp),
                        progress = sliderProgress,
                        bufferedProgress = videoState.bufferedPercentage / 100f,
                        onProgressChange = { videoState.seekTo((it * videoState.totalTime).toLong()) },
                    )
                }
                Text(
                    modifier = Modifier.width(52.dp),
                    text = videoState.totalTime.formatMinSec(),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                // More button — opens FileInfoBottomSheet
                MoreIconButton { state.showMediaInfo = true }
            }

            // Row 2: control bar [Speed][Sound][Play][Pip][Fullscreen]
            VideoControlsBar(
                isPlaying = videoState.isPlaying,
                isMuted = videoState.isMuted,
                isFullscreen = videoState.isFullscreenMode,
                playbackSpeed = videoState.speed,
                showPip = hasPipMode(),
                onSpeedChange = { videoState.changeSpeed(it) },
                onMuteToggle = { videoState.toggleMute() },
                onPlayPause = { videoState.togglePlay() },
                onPip = { com.ismartcoding.plain.platform.enterPipMode(videoState) },
                onFullscreenToggle = { videoState.isFullscreenMode = !videoState.isFullscreenMode },
            )
        }
    }
}

/** Circular "more" button that opens the file info bottom sheet. */
@Composable
private fun MoreIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(32.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.darkMask())
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            painter = painterResource(Res.drawable.ellipsis),
            contentDescription = stringResource(Res.string.more_info),
            tint = Color.White,
        )
    }
}
