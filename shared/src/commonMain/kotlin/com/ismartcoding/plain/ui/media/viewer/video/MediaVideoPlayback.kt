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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import com.ismartcoding.plain.platform.media.getVideoMediaId
import com.ismartcoding.plain.models.PlaybackType
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.platform.media.rememberVideoPlayerController
import com.ismartcoding.plain.ui.state.launchSafe
import kotlin.math.abs
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
    onFirstFrameRendered: () -> Unit,
): VideoPlayerController {
    val controller = rememberVideoPlayerController()
    var mediaId by remember(model.path, model.mediaId) { mutableStateOf(model.mediaId) }
    var wasActive by remember { mutableStateOf(false) }
    var playRecorded by remember { mutableStateOf(false) }
    var preparedPath by remember { mutableStateOf<String?>(null) }

    fun saveProgress() {
        val id = mediaId
        val positionMs = controller.currentPositionMs
        if (id.isEmpty() || positionMs < 0) return
        model.source.history.saveProgress(id, PlaybackType.VIDEO, positionMs)
    }

    suspend fun prepareVideo() {
        if (preparedPath == model.resourcePath) return
        controller.setMediaItem(model.resourcePath, model.tlsCertificateSha256)
        currentCoroutineContext().ensureActive()
        controller.prepare()
        preparedPath = model.resourcePath
    }

    controller.setEventListener { event ->
        when (event) {
            is VideoPlayerEvent.StateChanged -> {
                if (!videoState.isPreviewerOpen || pagerState.settledPage != page) return@setEventListener
                if (event.durationMs > 0L) videoState.totalTime = event.durationMs
                val id = mediaId
                val positionMs = controller.currentPositionMs
                if (event.isPlaying && !playRecorded && id.isNotEmpty() && positionMs >= 0) {
                    model.source.history.recordPlay(id, PlaybackType.VIDEO, positionMs)
                    playRecorded = true
                }
                videoState.isPlaying = event.isPlaying
                videoState.updateTime()
            }
            VideoPlayerEvent.PositionDiscontinuity -> {
                if (!videoState.isPreviewerOpen || pagerState.settledPage != page) return@setEventListener
                videoState.isSeeking = false
            }
            VideoPlayerEvent.FirstFrameRendered -> onFirstFrameRendered()
        }
    }

    LaunchedEffect(controller, pagerState.settledPage, videoState.isPreviewerOpen) {
        launchSafe {
            val isActive = videoState.isPreviewerOpen && pagerState.settledPage == page
            if (wasActive && !isActive) saveProgress()
            if (!isActive) {
                playRecorded = false
                wasActive = false
            }
            if (!videoState.isPreviewerOpen) {
                controller.abandonAudioFocus()
                controller.stop()
                preparedPath = null
                return@launchSafe
            }
            if (isActive) {
                val expectedTotalMs = (model.data as? MediaVideo)?.durationMs ?: 0L
                startVideoPlayback(controller, prepare = ::prepareVideo, resumePosition = {
                    if (mediaId.isEmpty() && model.source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) {
                        mediaId = getVideoMediaId(model.path).orEmpty()
                    }
                    val saved = if (mediaId.isNotEmpty()) {
                        model.source.history.find(mediaId, PlaybackType.VIDEO, Clock.System.now() - 30.days)?.positionMs
                    } else null
                    saved?.takeIf { it > 0 && (expectedTotalMs <= 0 || it < expectedTotalMs - 1000) } ?: 0L
                }, current = { videoState.isPreviewerOpen && pagerState.settledPage == page }) { position ->
                    videoState.initData(controller)
                    if (expectedTotalMs > 0L) videoState.totalTime = expectedTotalMs
                    videoState.currentTime = position
                    wasActive = true
                }
            } else if (abs(page - pagerState.settledPage) <= 1) {
                prepareVideo()
                controller.pause()
                controller.abandonAudioFocus()
            } else {
                controller.stop()
                preparedPath = null
            }
        }
    }

    DisposableEffect(controller) {
        onDispose {
            if (wasActive) saveProgress()
            controller.abandonAudioFocus()
            controller.stop()
            controller.release()
        }
    }
    return controller
}

internal suspend fun startVideoPlayback(
    controller: VideoPlayerController,
    prepare: suspend () -> Unit,
    resumePosition: suspend () -> Long,
    current: () -> Boolean,
    onStart: (Long) -> Unit,
) = coroutineScope {
    val history = async { resumePosition() }
    prepare()
    val position = history.await()
    ensureActive()
    if (!current()) return@coroutineScope
    onStart(position)
    controller.seekTo(position)
    controller.requestAudioFocus()
    controller.play()
}
