package com.ismartcoding.plain.ui.page.audioplayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.chevron_left
import com.ismartcoding.plain.i18n.expand_more
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.audioIsPlayingFlow
import com.ismartcoding.plain.platform.audioJustPlayWithNotificationCheck
import com.ismartcoding.plain.platform.audioPause
import com.ismartcoding.plain.platform.audioPlayerProgress
import com.ismartcoding.plain.platform.audioPlay
import com.ismartcoding.plain.platform.audioSeekTo
import com.ismartcoding.plain.platform.audioSkipToNext
import com.ismartcoding.plain.platform.audioSkipToPrevious
import com.ismartcoding.plain.platform.exitImmersiveFullscreen
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.platform.playlistAudioFromPath
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerPage(audioPlaylistVM: AudioPlaylistViewModel, onDismissRequest: () -> Unit) {
    DisposableEffect(Unit) {
        onDispose { exitImmersiveFullscreen() }
    }

    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }
    val isPlaying by audioIsPlayingFlow().collectAsState()
    val playMode by TempData.audioPlayMode.collectAsState()
    val playbackSpeed by TempData.audioPlaybackSpeed.collectAsState()
    var showPlaylist by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var isTimerActive by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(PlayerView.COVER) }
    val currentPlayingPath = audioPlaylistVM.selectedPath

    // Pager pages mirror the play queue; fall back to the single playing
    // track when the queue is empty (e.g. player opened from chat).
    var fallbackItem by remember { mutableStateOf<DPlaylistAudio?>(null) }
    LaunchedEffect(currentPlayingPath.value) {
        fallbackItem = null
        if (!isDragging) progress = audioPlayerProgress() / 1000f
        if (audioPlaylistVM.playlistItems.value.isEmpty() && currentPlayingPath.value.isNotEmpty()) {
            fallbackItem = withIO { playlistAudioFromPath(currentPlayingPath.value) }
        }
    }
    val pages = audioPlaylistVM.playlistItems.value.ifEmpty { listOfNotNull(fallbackItem) }

    val playingIndex = pages.indexOfFirst { it.path == currentPlayingPath.value }
    val initialIndex = playingIndex.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { pages.size.coerceAtLeast(1) })

    // Swiping to a page plays that track.
    LaunchedEffect(pagerState, pages) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            val item = pages.getOrNull(page) ?: return@collect
            if (item.path != currentPlayingPath.value) {
                audioJustPlayWithNotificationCheck(item)
            }
        }
    }

    // Track changes from outside the pager (skip buttons, auto advance,
    // shuffle) move the pager to the playing page.
    LaunchedEffect(currentPlayingPath.value, pages) {
        val index = pages.indexOfFirst { it.path == currentPlayingPath.value }
        if (index >= 0 && pagerState.currentPage != index && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(index)
        }
    }

    LaunchedEffect(isPlaying, isDragging) {
        if (isPlaying && !isDragging) {
            while (true) {
                progress = audioPlayerProgress() / 1000f
                delay(250)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            isTimerActive = TempData.audioSleepTimerFutureTime > TimeHelper.nowMillis()
            delay(1000)
        }
    }

    val currentItem = pages.getOrNull(playingIndex) ?: pages.getOrNull(initialIndex)
    val duration = currentItem?.duration?.toFloat() ?: 0f

    // Full-height sheet reaching the top of the screen; inset padding and
    // the drag corner animation are handled by PModalBottomSheet.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun collapse() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
    }
    PModalBottomSheet(
        onDismissRequest = onDismissRequest,
        fullHeight = true,
        // In the lyrics view the sheet must not be draggable: back returns
        // to the cover view instead of dismissing.
        sheetGesturesEnabled = viewMode == PlayerView.COVER,
        sheetState = sheetState,
    ) {
        PBackHandler(enabled = viewMode == PlayerView.LYRICS) {
            viewMode = PlayerView.COVER
        }
        Column(
            // The status bar stays visible (edge-to-edge window), so keep the
            // header below it and the controls above the navigation bar.
            modifier = Modifier.fillMaxSize().navigationBarsPadding().statusBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(start = 8.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val inLyrics = viewMode == PlayerView.LYRICS
                IconButton(
                    onClick = { if (inLyrics) viewMode = PlayerView.COVER else collapse() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(if (inLyrics) Res.drawable.chevron_left else Res.drawable.expand_more),
                        contentDescription = if (inLyrics) "Back" else "Collapse",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                beyondViewportPageCount = 1,
            ) { page ->
                val item = pages.getOrNull(page) ?: return@HorizontalPager
                AudioPlayerTrackPage(
                    item = item,
                    progressMs = (progress * 1000).toLong(),
                    isPlaying = isPlaying,
                    viewMode = viewMode,
                    onViewModeChange = { viewMode = it },
                    onSeek = {
                        // Update the UI position synchronously: the 250ms
                        // poll (absent while paused) would leave the highlight
                        // on the previous line for a beat.
                        progress = it / 1000f
                        audioSeekTo(it)
                    },
                )
            }

            // Playback controls only on the cover view.
            AudioPlayerBottomControls(
                visible = viewMode == PlayerView.COVER,
                progress = progress,
                duration = duration,
                isPlaying = isPlaying,
                onScrub = { isDragging = true; progress = minOf(it, duration) },
                onScrubFinished = {
                    if (duration > 0 && progress >= 0) audioSeekTo((progress * 1000).toLong())
                    isDragging = false
                },
                playMode = playMode,
                playbackSpeed = playbackSpeed,
                isTimerActive = isTimerActive,
                onSleepTimer = { showSleepTimer = true },
                onPlaylist = { showPlaylist = true },
                onPlayPrevious = { audioSkipToPrevious() },
                onPlayPause = { if (isPlaying) audioPause() else audioPlay() },
                onPlayNext = { audioSkipToNext() },
                scope = scope,
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
