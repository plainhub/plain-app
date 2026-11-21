package com.ismartcoding.plain.ui.media.viewer.video

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ismartcoding.plain.models.PlaybackType
import com.ismartcoding.plain.models.Video as MediaVideo
import com.ismartcoding.plain.platform.media.getVideoMediaId
import com.ismartcoding.plain.ui.components.ToastManager
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.platform.media.rememberVideoPlayerController
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun rememberMediaVideoController(
    pagerState: PagerState,
    videoState: VideoState,
    page: Int,
    model: PreviewItem,
    onFirstFrameRendered: () -> Unit,
): VideoPlayerController {
    val controller = rememberVideoPlayerController()
    var mediaId by remember(model.path, model.mediaId) { mutableStateOf(model.mediaId) }
    val selected = pagerState.settledPage == page
    var playRecorded by remember(selected, videoState.isPreviewerOpen) { mutableStateOf(false) }
    val duration = (model.data as? MediaVideo)?.durationMs ?: 0L
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val start = remember(controller) { VideoPlaybackStart() }

    controller.setEventListener { event ->
        if (event == VideoPlayerEvent.FirstFrameRendered) {
            onFirstFrameRendered()
            start.frameRendered()
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

    val scope = rememberCoroutineScope()
    DisposableEffect(controller) {
        val preparation = scope.prepareVideoPlayback(controller, model.resourcePath, model.tlsCertificateSha256) {
            if (mediaId.isEmpty() && model.pickedMediaType == null && model.source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) {
                mediaId = getVideoMediaId(model.path).orEmpty()
            }
            val id = mediaId
            val saved = if (id.isNotEmpty()) withContext(Dispatchers.IO) {
                model.source.history.find(id, PlaybackType.VIDEO, Clock.System.now() - 30.days)?.positionMs
            } else null
            saved?.takeIf { it > 0 && (duration <= 0 || it < duration - 1000) } ?: 0L
        }
        onDispose {
            preparation.cancel()
            controller.release()
        }
    }

    val active = selected && videoState.isPreviewerOpen
    DisposableEffect(controller, active, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start.resume {
                    if (videoState.isPreviewerOpen && pagerState.settledPage == page &&
                        lifecycle.currentState == Lifecycle.State.RESUMED) {
                        videoState.initData(controller)
                        videoState.totalTime = controller.durationMs.takeIf { it > 0 } ?: duration
                        videoState.currentTime = controller.currentPositionMs
                        controller.requestAudioFocus()
                        controller.play()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    start.cancel()
                    if (videoState.controller === controller && !videoState.enablePip) {
                        controller.pause()
                        controller.abandonAudioFocus()
                    }
                }
                else -> Unit
            }
        }
        if (active) lifecycle.addObserver(observer)
        onDispose {
            if (active) {
                start.cancel()
                lifecycle.removeObserver(observer)
            }
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

internal fun CoroutineScope.prepareVideoPlayback(
    controller: VideoPlayerController,
    path: String,
    tlsCertificateSha256: String?,
    resumePosition: suspend () -> Long,
) = launch(start = CoroutineStart.UNDISPATCHED) {
    try {
        val position = resumePosition()
        currentCoroutineContext().ensureActive()
        controller.setMediaItem(path, tlsCertificateSha256, position)
        currentCoroutineContext().ensureActive()
        controller.prepare()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        ToastManager.showErrorToast(error.message ?: error.toString())
    }
}

internal class VideoPlaybackStart {
    private var frameReady = false
    private var pending: (() -> Unit)? = null

    fun frameRendered() {
        frameReady = true
        startIfReady()
    }

    fun resume(play: () -> Unit) {
        pending = play
        startIfReady()
    }

    fun cancel() {
        pending = null
    }

    private fun startIfReady() {
        if (!frameReady) return
        val play = pending ?: return
        pending = null
        play()
    }
}
