package com.ismartcoding.plain.ui.page.dlna

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ismartcoding.plain.features.dlna.DlnaPlaybackState
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.platform.VideoPlayerSurface
import com.ismartcoding.plain.platform.enterPipMode
import com.ismartcoding.plain.platform.exitImmersiveFullscreen
import com.ismartcoding.plain.platform.hasPipMode
import com.ismartcoding.plain.platform.keepScreenOn
import com.ismartcoding.plain.platform.rememberVideoPlayerController
import com.ismartcoding.plain.platform.setSystemBarsVisible
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoControlsBar
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoOverlayScaffold
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoPlayerEvent
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun DlnaReceiverVideoPlayerContent(onExit: () -> Unit) {
    val mediaUri by DlnaRendererState.mediaUri.collectAsState()
    val mediaTitle by DlnaRendererState.mediaTitle.collectAsState()
    val remotePlaybackState by DlnaRendererState.playbackState.collectAsState()
    val seekTargetMs by DlnaRendererState.seekTargetMs.collectAsState()

    val controller = rememberVideoPlayerController(claimAudioSession = false)
    val state = remember { VideoState().apply { isFullscreenMode = true } }
    var showControls by remember { mutableStateOf(true) }
    var controlsSeed by remember { mutableIntStateOf(0) }

    LaunchedEffect(controller) {
        state.initData(controller)
        controller.setEventListener { event ->
            when (event) {
                is VideoPlayerEvent.StateChanged -> {
                    if (event.duration > 0L) {
                        state.totalTime = event.duration
                    }
                    state.isPlaying = event.isPlaying
                    state.updateTime()
                }
                VideoPlayerEvent.PositionDiscontinuity -> {
                    state.isSeeking = false
                }
                VideoPlayerEvent.FirstFrameRendered -> {}
            }
        }
    }
    LaunchedEffect(mediaUri) {
        if (mediaUri.isNotEmpty()) {
            controller.pause()
            controller.setMediaItem(mediaUri)
            controller.prepare()
        }
    }
    LaunchedEffect(remotePlaybackState) {
        when (remotePlaybackState) {
            DlnaPlaybackState.PLAYING -> controller.play()
            DlnaPlaybackState.PAUSED -> controller.pause()
            DlnaPlaybackState.STOPPED -> controller.stopAndRewind()
            else -> {}
        }
    }
    LaunchedEffect(seekTargetMs) {
        seekTargetMs?.let { state.seekTo(it) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            state.updateTime()
            DlnaRendererState.currentPositionMs.value = state.currentTime
            DlnaRendererState.durationMs.value = state.totalTime
            delay(500)
        }
    }
    LaunchedEffect(showControls, controlsSeed) {
        if (showControls) { delay(4.seconds); showControls = false }
    }
    // Tie system bar visibility to the control overlay: bars are visible
    // exactly when the top bar is visible, so statusBarsPadding() stays stable
    // and the top bar is never pushed up against the screen edge when the
    // status bar disappears. Edge-to-edge is preserved (setSystemBarsVisible
    // never flips DecorFitsSystemWindows), so the video surface never relayouts.
    LaunchedEffect(showControls, state.isFullscreenMode) {
        setSystemBarsVisible(showControls || !state.isFullscreenMode)
    }
    keepScreenOn(enabled = true)
    DisposableEffect(Unit) {
        onDispose {
            keepScreenOn(enabled = false)
            exitImmersiveFullscreen()
            controller.release()
        }
    }

    val onPlayPause = {
        val wasPlaying = state.isPlaying
        state.togglePlay()
        DlnaRendererState.playbackState.value =
            if (wasPlaying) DlnaPlaybackState.PAUSED else DlnaPlaybackState.PLAYING
    }

    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
            .clickable(indication = null, interactionSource = interactionSource) {
                showControls = !showControls; if (showControls) controlsSeed++
            },
    ) {
        VideoPlayerSurface(
            modifier = Modifier.fillMaxSize(),
            controller = controller,
            videoState = null,
        )
        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
            VideoOverlayScaffold(
                title = mediaTitle,
                positionMs = state.currentTime,
                durationMs = state.totalTime,
                onSeek = { ratio ->
                    state.seekTo((ratio * state.totalTime.coerceAtLeast(1L)).toLong())
                },
                onExit = onExit,
                onMore = null,
                trailing = {
                    DlnaDownloadIconButton(modifier = Modifier.fillMaxSize())
                },
            ) {
                VideoControlsBar(
                    isPlaying = state.isPlaying,
                    isMuted = state.isMuted,
                    isFullscreen = state.isFullscreenMode,
                    playbackSpeed = state.speed,
                    showPip = hasPipMode(),
                    onSpeedChange = { state.changeSpeed(it) },
                    onMuteToggle = { state.toggleMute() },
                    onPlayPause = onPlayPause,
                    onPip = { enterPipMode(state) },
                    onFullscreenToggle = { state.isFullscreenMode = !state.isFullscreenMode },
                )
            }
        }
    }
}
