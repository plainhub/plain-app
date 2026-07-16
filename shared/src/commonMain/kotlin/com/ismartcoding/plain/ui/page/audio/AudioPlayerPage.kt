package com.ismartcoding.plain.ui.page.audio

import com.ismartcoding.plain.helpers.TimeHelper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.extensions.formatDuration
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.platform.audioIsPlayingFlow
import com.ismartcoding.plain.platform.audioPause
import com.ismartcoding.plain.platform.audioPlayerProgress
import com.ismartcoding.plain.platform.audioPlay
import com.ismartcoding.plain.platform.audioSeekTo
import com.ismartcoding.plain.platform.audioSkipToNext
import com.ismartcoding.plain.platform.audioSkipToPrevious
import com.ismartcoding.plain.platform.playlistAudioFromPath
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.WaveSlider
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.page.audio.components.AudioPlayerCover
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerPage(audioPlaylistVM: AudioPlaylistViewModel, onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableFloatStateOf(0f) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    val isPlaying by audioIsPlayingFlow().collectAsState()
    val playMode by TempData.audioPlayMode.collectAsState()
    var showPlaylist by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var isTimerActive by remember { mutableStateOf(false) }
    val currentPlayingPath = audioPlaylistVM.selectedPath

    LaunchedEffect(currentPlayingPath.value) {
        scope.launch {
            val path = currentPlayingPath.value
            if (path.isNotEmpty()) {
                val audio = withIO { playlistAudioFromPath(path) }
                duration = audio.duration.toFloat()
                if (!isDragging) progress = audioPlayerProgress() / 1000f
                title = audio.title; artist = audio.artist
            }
            isTimerActive = TempData.audioSleepTimerFutureTime > TimeHelper.nowMillis()
        }
    }

    LaunchedEffect(isPlaying, isDragging) {
        if (isPlaying && !isDragging) { while (true) { progress = audioPlayerProgress() / 1000f; delay(1000) } }
    }

    LaunchedEffect(Unit) {
        while (true) { isTimerActive = TempData.audioSleepTimerFutureTime > TimeHelper.nowMillis(); delay(1000) }
    }

    PModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 32.dp).height(280.dp), contentAlignment = Alignment.Center) {
                AudioPlayerCover(path = currentPlayingPath.value)
            }
            AudioPlayerTrackInfo(title = title, artist = artist)
            VerticalSpace(32.dp)
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
                WaveSlider(
                    value = progress, onValueChange = { isDragging = true; progress = minOf(it, duration) },
                    onValueChangeFinished = { if (duration > 0 && progress >= 0) audioSeekTo(progress.toLong()); isDragging = false },
                    valueRange = 0f..maxOf(duration, 1f), modifier = Modifier.fillMaxWidth().height(32.dp), isPlaying = isPlaying,
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = (audioPlayerProgress() / 1000).formatDuration(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = duration.toLong().formatDuration(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            AudioPlayerControls(
                playMode = playMode, onPlayModeChange = {}, isTimerActive = isTimerActive,
                onSleepTimer = { showSleepTimer = true }, onPlaylist = { showPlaylist = true }, isPlaying = isPlaying, scope = scope,
                onPlayPrevious = { audioSkipToPrevious() },
                onPlayPause = { if (isPlaying) audioPause() else audioPlay() },
                onPlayNext = { audioSkipToNext() },
            )
        }
    }

    if (showSleepTimer) {
        SleepTimerPage(onDismissRequest = { showSleepTimer = false; isTimerActive = TempData.audioSleepTimerFutureTime > TimeHelper.nowMillis() })
    }
    if (showPlaylist) {
        AudioPlaylistPage(audioPlaylistVM, onDismissRequest = { showPlaylist = false })
    }
}
