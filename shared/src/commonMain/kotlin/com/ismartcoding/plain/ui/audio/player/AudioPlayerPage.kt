package com.ismartcoding.plain.ui.audio.player

import com.ismartcoding.plain.features.media.MediaClient

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
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.features.audio.AudioSleepTimer
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.platform.media.audioIsPlayingFlow
import com.ismartcoding.plain.platform.media.audioJustPlayWithNotificationCheck
import com.ismartcoding.plain.platform.media.audioPause
import com.ismartcoding.plain.platform.media.audioPlay
import com.ismartcoding.plain.platform.media.audioPlayerProgress
import com.ismartcoding.plain.platform.media.audioSeekTo
import com.ismartcoding.plain.platform.media.audioSkipToNext
import com.ismartcoding.plain.platform.media.audioSkipToPrevious
import com.ismartcoding.plain.ui.platform.system.exitImmersiveFullscreen
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.chevron_left as ui_drawable_chevron_left
import com.ismartcoding.plain.ui.resources.expand_more as ui_drawable_expand_more
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerPage(audioQueueVM: AudioQueueViewModel, onDismissRequest: () -> Unit) {
    DisposableEffect(Unit) {
        onDispose { exitImmersiveFullscreen() }
    }

    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }
    val isPlaying by audioIsPlayingFlow().collectAsState()
    val playMode = if (audioQueueVM.source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) Prefs.audioPlayMode.collectAsState().value else audioQueueVM.playMode.value
    val playbackSpeed = if (audioQueueVM.source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) Prefs.audioPlaybackSpeed.collectAsState().value else audioQueueVM.playbackSpeed.floatValue
    var showQueue by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    val sleepTimerEndsAt by AudioSleepTimer.endsAt
    var viewMode by remember { mutableStateOf(PlayerView.COVER) }
    val currentPlayingPath = audioQueueVM.selectedPath

    var fallbackItem by remember { mutableStateOf<AudioItem?>(null) }
    LaunchedEffect(currentPlayingPath.value, audioQueueVM.queueItems.value) {
        fallbackItem = null
        if (!isDragging) progress = audioPlayerProgress().toFloat()
        if (currentPlayingPath.value.isNotEmpty() && audioQueueVM.queueItems.value.none { if (it.mediaId != null) it.mediaId == audioQueueVM.selectedMediaId.value else it.path == currentPlayingPath.value }) {
            fallbackItem = withIO { audioQueueVM.source.media.audioInfo(currentPlayingPath.value) }.copy(mediaId = audioQueueVM.selectedMediaId.value)
        }
    }
    val pages = if (fallbackItem != null) listOfNotNull(fallbackItem) else audioQueueVM.queueItems.value

    val playingIndex = pages.indexOfFirst { if (it.mediaId != null) it.mediaId == audioQueueVM.selectedMediaId.value else it.path == currentPlayingPath.value }
    val initialIndex = playingIndex.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { pages.size.coerceAtLeast(1) })

    LaunchedEffect(pagerState, pages) {
        snapshotFlow { pagerState.settledPage }.drop(1).collect { page ->
            val item = pages.getOrNull(page) ?: return@collect
            if (currentPlayingPath.value.isNotEmpty() && (if (item.mediaId != null) item.mediaId != audioQueueVM.selectedMediaId.value else item.path != currentPlayingPath.value)) {
                audioJustPlayWithNotificationCheck(item, audioQueueVM.source)
            }
        }
    }

    LaunchedEffect(currentPlayingPath.value, pages) {
        val index = pages.indexOfFirst { if (it.mediaId != null) it.mediaId == audioQueueVM.selectedMediaId.value else it.path == currentPlayingPath.value }
        if (index >= 0 && pagerState.currentPage != index && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(index)
        }
    }

    LaunchedEffect(isPlaying, isDragging) {
        if (isPlaying && !isDragging) {
            while (true) {
                progress = audioPlayerProgress().toFloat()
                delay(250)
            }
        }
    }

    val currentItem = pages.getOrNull(playingIndex) ?: pages.getOrNull(initialIndex)
    val durationMs = currentItem?.durationMs?.toFloat() ?: 0f

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
                        painter = painterResource(if (inLyrics) UiRes.drawable.ui_drawable_chevron_left else UiRes.drawable.ui_drawable_expand_more),
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
                    progressMs = progress.toLong(),
                    isPlaying = isPlaying,
                    viewMode = viewMode,
                    onViewModeChange = { viewMode = it },
                    onSeek = {
                        // Update the UI position synchronously: the 250ms
                        // poll (absent while paused) would leave the highlight
                        // on the previous line for a beat.
                        progress = it.toFloat()
                        audioSeekTo(it)
                    },
                )
            }

            // Playback controls only on the cover view.
            AudioPlayerBottomControls(
                visible = viewMode == PlayerView.COVER,
                progress = progress,
                durationMs = durationMs,
                isPlaying = isPlaying,
                onScrub = { isDragging = true; progress = minOf(it, durationMs) },
                onScrubFinished = {
                    if (durationMs > 0 && progress >= 0) audioSeekTo(progress.toLong())
                    isDragging = false
                },
                playMode = playMode,
                playbackSpeed = playbackSpeed,
                isTimerActive = sleepTimerEndsAt > 0,
                onSleepTimer = { showSleepTimer = true },
                onOpenQueue = { showQueue = true },
                onPlayPrevious = { audioSkipToPrevious() },
                onPlayPause = { if (isPlaying) audioPause() else audioPlay() },
                onPlayNext = { audioSkipToNext() },
                scope = scope,
            )
        }
    }

    if (showSleepTimer) {
        SleepTimerPage(onDismissRequest = { showSleepTimer = false })
    }
    if (showQueue) {
        AudioQueuePage(audioQueueVM, onDismissRequest = { showQueue = false })
    }
}
