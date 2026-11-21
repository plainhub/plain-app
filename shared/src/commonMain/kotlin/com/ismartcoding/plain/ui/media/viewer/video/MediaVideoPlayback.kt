package com.ismartcoding.plain.ui.media.viewer.video

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.ismartcoding.plain.platform.media.getVideoMediaId
import com.ismartcoding.plain.models.PlaybackType
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.platform.media.rememberVideoPlayerController
import com.ismartcoding.plain.ui.state.launchSafe
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import com.ismartcoding.plain.models.Video as MediaVideo

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberMediaVideoController(
    pagerState: PagerState,
    videoState: VideoState,
    page: Int,
    model: PreviewItem,
    frameReady: Boolean,
    onFirstFrameRendered: () -> Unit,
): VideoPlayerController {
    val controller = rememberVideoPlayerController()
    var mediaId by remember(model.path, model.mediaId) { mutableStateOf(model.mediaId) }
    val selected = pagerState.settledPage == page
    var playRecorded by remember(selected, videoState.isPreviewerOpen) { mutableStateOf(false) }
    val duration = (model.data as? MediaVideo)?.durationMs ?: 0L
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    controller.setEventListener { event ->
        if (event == VideoPlayerEvent.FirstFrameRendered) {
            onFirstFrameRendered()
        } else if (videoState.controller === controller && pagerState.settledPage == page) {
            when (event) {
                is VideoPlayerEvent.StateChanged -> {
                    if (event.durationMs > 0L) videoState.totalTime = event.durationMs
                    if (event.isPlaying && !playRecorded && mediaId.isNotEmpty()) {
                        playRecorded = true
                        model.source.history.recordPlay(mediaId, PlaybackType.VIDEO, controller.currentPositionMs)
                    }
                    videoState.updateTime()
                }
                VideoPlayerEvent.PositionDiscontinuity -> videoState.isSeeking = false
                else -> Unit
            }
        }
    }

    LaunchedEffect(controller) {
        launchSafe {
            prepareVideoPlayback(controller, model.resourcePath, model.tlsCertificateSha256) {
                if (mediaId.isEmpty() && model.source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) {
                    mediaId = getVideoMediaId(model.path).orEmpty()
                }
                val saved = if (mediaId.isNotEmpty()) {
                    model.source.history.find(mediaId, PlaybackType.VIDEO, Clock.System.now() - 30.days)?.positionMs
                } else null
                saved?.takeIf { it > 0 && (duration <= 0 || it < duration - 1000) } ?: 0L
            }
        }
    }

    DisposableEffect(controller) {
        onDispose { controller.release() }
    }

    val active = selected && videoState.isPreviewerOpen && frameReady
    DisposableEffect(controller, active, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && videoState.isPreviewerOpen && pagerState.settledPage == page) {
                videoState.initData(controller)
                videoState.totalTime = controller.durationMs.takeIf { it > 0 } ?: duration
                videoState.currentTime = controller.currentPositionMs
                controller.requestAudioFocus()
                controller.play()
            } else if (event == Lifecycle.Event.ON_PAUSE && !videoState.enablePip) {
                controller.pause()
                controller.abandonAudioFocus()
            }
        }
        if (active) lifecycle.addObserver(observer)
        onDispose {
            if (active) lifecycle.removeObserver(observer)
            if (active && videoState.controller === controller) {
                val position = controller.currentPositionMs
                controller.pause()
                controller.abandonAudioFocus()
                if (mediaId.isNotEmpty()) model.source.history.saveProgress(mediaId, PlaybackType.VIDEO, position)
                if (pagerState.settledPage != page && duration > 0 && position >= duration - 1000) controller.seekTo(0)
                videoState.controller = null
                videoState.isPlaying = false
                videoState.isSeeking = false
            }
        }
    }
    return controller
}

internal suspend fun prepareVideoPlayback(
    controller: VideoPlayerController,
    path: String,
    tlsCertificateSha256: String?,
    resumePosition: suspend () -> Long,
) {
    val position = resumePosition()
    currentCoroutineContext().ensureActive()
    controller.setMediaItem(path, tlsCertificateSha256, position)
    currentCoroutineContext().ensureActive()
    controller.prepare()
}
