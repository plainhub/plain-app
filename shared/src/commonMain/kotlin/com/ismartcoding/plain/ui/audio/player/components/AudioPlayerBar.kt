package com.ismartcoding.plain.ui.audio.player.components

import com.ismartcoding.plain.features.media.MediaClient

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.media.audioIsPlayingFlow
import com.ismartcoding.plain.platform.media.audioPause
import com.ismartcoding.plain.platform.media.audioPlay
import com.ismartcoding.plain.platform.media.audioPlayerProgress
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.audio.player.AudioQueuePage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun AudioPlayerBar(
    audioQueueVM: AudioQueueViewModel,
    castVM: CastViewModel? = null,
    modifier: Modifier = Modifier,
    dragSelectState: DragSelectState? = null,
) {
    val audioOutput by com.ismartcoding.plain.features.audio.AudioCommands.output.collectAsState()
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var durationMs by remember { mutableFloatStateOf(1000f) }
    val isPlaying by audioIsPlayingFlow().collectAsState()
    var showQueue by remember { mutableStateOf(false) }
    val currentPlayingPath = audioQueueVM.selectedPath

    LaunchedEffect(currentPlayingPath.value) {
        scope.launch {
            val path = currentPlayingPath.value
            if (path.isNotEmpty()) {
                val audio = withIO { audioQueueVM.source.media.audioInfo(path) }
                title = audio.title; artist = audio.artist
                durationMs = audio.durationMs.toFloat(); progress = audioPlayerProgress().toFloat()
            }
        }
    }

    var progressUpdateJob: Job? = null
    LaunchedEffect(isPlaying) {
        progressUpdateJob?.cancel()
        if (isPlaying) {
            progressUpdateJob = scope.launch {
                while (isActive) { progress = audioPlayerProgress().toFloat(); delay(1000) }
            }
        }
    }

    AnimatedVisibility(
        visible = audioOutput.source.key == audioQueueVM.source.key && currentPlayingPath.value.isNotEmpty() &&
            dragSelectState?.selectMode != true &&
            castVM?.castMode?.value != true,
        enter = slideInVertically { it }, exit = slideOutVertically { it }, modifier = modifier,
    ) {
        AudioPlayerBarCard(
            title = title,
            artist = artist,
            progress = progress,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onClickContent = { audioQueueVM.audioPlayerVisible.value = true },
            onClickQueue = { showQueue = true },
            onPlayPause = { if (isPlaying) audioPause() else audioPlay() },
        )
    }

    if (showQueue) AudioQueuePage(audioQueueVM, onDismissRequest = { showQueue = false })
}
