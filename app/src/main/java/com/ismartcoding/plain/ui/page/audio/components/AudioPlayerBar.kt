package com.ismartcoding.plain.ui.page.audio.components

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
import androidx.compose.ui.platform.LocalContext
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.data.DPlaylistAudio
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.page.audio.AudioPlayerPage
import com.ismartcoding.plain.ui.page.audio.AudioPlaylistPage
import com.ismartcoding.plain.ui.page.audio.SleepTimerPage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun AudioPlayerBar(audioPlaylistVM: AudioPlaylistViewModel, castVM: CastViewModel, modifier: Modifier = Modifier, dragSelectState: DragSelectState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(1f) }
    val isPlaying by AudioPlayer.isPlayingFlow.collectAsState()
    var showPlayer by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showPlaylist by remember { mutableStateOf(false) }
    val currentPlayingPath = audioPlaylistVM.selectedPath

    LaunchedEffect(currentPlayingPath.value) {
        scope.launch {
            val path = currentPlayingPath.value
            if (path.isNotEmpty()) {
                val audio = withIO { DPlaylistAudio.fromPath(context, path) }
                title = audio.title; artist = audio.artist
                duration = audio.duration.toFloat(); progress = AudioPlayer.playerProgress / 1000f
            }
            if (showPlayer) showPlayer = path.isNotEmpty()
        }
    }

    var progressUpdateJob: Job? = null
    LaunchedEffect(isPlaying) {
        progressUpdateJob?.cancel()
        if (isPlaying) {
            progressUpdateJob = scope.launch {
                while (isActive) { progress = AudioPlayer.playerProgress / 1000f; delay(1000) }
            }
        }
    }

    AnimatedVisibility(
        visible = currentPlayingPath.value.isNotEmpty() && !dragSelectState.selectMode && !castVM.castMode.value,
        enter = slideInVertically { it }, exit = slideOutVertically { it }, modifier = modifier
    ) {
        AudioPlayerBarCard(
            title = title, artist = artist, progress = progress, duration = duration,
            isPlaying = isPlaying,
            onClickContent = { showPlayer = true }, onClickPlaylist = { showPlaylist = true }
        )
    }

    if (showPlayer) AudioPlayerPage(audioPlaylistVM, onDismissRequest = { showPlayer = false })
    if (showSleepTimer) SleepTimerPage(onDismissRequest = { showSleepTimer = false })
    if (showPlaylist) AudioPlaylistPage(audioPlaylistVM, onDismissRequest = { showPlaylist = false })
}
