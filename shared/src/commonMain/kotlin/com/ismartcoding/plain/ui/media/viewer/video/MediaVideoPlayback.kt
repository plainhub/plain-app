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
import com.ismartcoding.plain.features.media.MediaPlayHistoryClient
import com.ismartcoding.plain.platform.media.getVideoMediaId
import com.ismartcoding.plain.platform.system.DataType
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
        model.source.history.saveProgress(id, DataType.VIDEO, positionMs)
    }

    controller.setEventListener { event ->
        when (event) {
            is VideoPlayerEvent.StateChanged -> {
                if (!videoState.isPreviewerOpen || pagerState.settledPage != page) return@setEventListener
                if (event.durationMs > 0L) videoState.totalTime = event.durationMs
                val id = mediaId
                val positionMs = controller.currentPositionMs
                if (event.isPlaying && !playRecorded && id.isNotEmpty() && positionMs >= 0) {
                    model.source.history.recordPlay(id, DataType.VIDEO, positionMs)
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
                if (mediaId.isEmpty() && model.source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) mediaId = getVideoMediaId(model.path).orEmpty()
                val savedPos = if (mediaId.isNotEmpty()) {
                    model.source.history.find(mediaId, DataType.VIDEO, Clock.System.now() - 30.days)?.positionMs
                } else null
                if (!videoState.isPreviewerOpen || pagerState.settledPage != page) return@launchSafe
                videoState.initData(controller)
                val expectedTotalMs = (model.data as? MediaVideo)?.durationMs ?: 0L
                if (expectedTotalMs > 0L) videoState.totalTime = expectedTotalMs
                val atEnd = savedPos != null && savedPos > 0 && expectedTotalMs > 0 && savedPos >= expectedTotalMs - 1000
                val positionMs = if (savedPos != null && savedPos > 0 && !atEnd) savedPos else 0L
                videoState.currentTime = positionMs
                if (preparedPath != model.resourcePath) {
                    controller.setMediaItem(model.resourcePath, model.tlsCertificateSha256)
                    controller.prepare()
                    preparedPath = model.resourcePath
                }
                controller.seekTo(positionMs)
                controller.requestAudioFocus()
                wasActive = true
                controller.play()
            } else if (abs(page - pagerState.settledPage) <= 1) {
                if (preparedPath != model.resourcePath) {
                    controller.setMediaItem(model.resourcePath, model.tlsCertificateSha256)
                    controller.prepare()
                    preparedPath = model.resourcePath
                }
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
