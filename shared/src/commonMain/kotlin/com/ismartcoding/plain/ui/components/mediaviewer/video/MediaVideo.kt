package com.ismartcoding.plain.ui.components.mediaviewer.video

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DVideoPlayProgress
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.platform.getVideoMeta
import com.ismartcoding.plain.platform.rememberVideoPlayerController
import com.ismartcoding.plain.platform.VideoPlayerSurface
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_OFFSET_X
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_OFFSET_Y
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_ROTATION
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_SCALE
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.RawGesture
import com.ismartcoding.plain.ui.components.mediaviewer.SizeChangeContent
import com.ismartcoding.plain.ui.components.mediaviewer.detectTransformGestures
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaVideo(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    videoState: VideoState,
    page: Int,
    model: PreviewItem,
    scale: Float = DEFAULT_SCALE,
    offsetX: Float = DEFAULT_OFFSET_X,
    offsetY: Float = DEFAULT_OFFSET_Y,
    rotation: Float = DEFAULT_ROTATION,
    gesture: RawGesture = RawGesture(),
    onMounted: () -> Unit = {},
    onSizeChange: suspend (SizeChangeContent) -> Unit = {},
    boundClip: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val viewerAlpha = remember { Animatable(0F) }
    var bSize by remember { mutableStateOf(IntSize(0, 0)) }
    var vSize by remember { mutableStateOf(IntSize(0, 0)) }
    val sizing = MediaVideoSizing(bSize, vSize)
    var videoSpecified by remember { mutableStateOf(false) }
    LaunchedEffect(vSize, bSize) {
        if (vSize != IntSize.Zero && bSize != IntSize.Zero) {
            onSizeChange(SizeChangeContent(defaultSize = sizing.displaySize, containerSize = bSize, maxScale = sizing.maxScale))
        }
    }

    LaunchedEffect(model.path) {
        if (model.intrinsicSize == IntSize.Zero) {
            when (val data = model.data) {
                is DVideo -> model.initAsync(data)
                else -> {
                    val meta = getVideoMeta(model.path)
                    if (meta != null) {
                        model.intrinsicSize = if (meta.rotation == 90 || meta.rotation == 270) {
                            IntSize(meta.height, meta.width)
                        } else {
                            IntSize(meta.width, meta.height)
                        }
                    }
                }
            }
        }
        if (model.intrinsicSize != IntSize.Zero) {
            vSize = model.intrinsicSize
            videoSpecified = true
        }
    }

    val controller = rememberVideoPlayerController()
    val progressDao = remember { AppDatabase.instance.videoPlayProgressDao() }
    var firstFrameRendered by remember { mutableStateOf(false) }
    // Track which media item is already prepared on this controller so an
    // already-preloaded (adjacent) video is not reset when it becomes active.
    var preparedPath by remember { mutableStateOf<String?>(null) }

    controller.setEventListener { event ->
        when (event) {
            is VideoPlayerEvent.StateChanged -> {
                if (!videoState.isPreviewerOpen || pagerState.settledPage != page) return@setEventListener
                if (event.duration > 0L) {
                    videoState.totalTime = event.duration
                }
                videoState.isPlaying = event.isPlaying
                videoState.updateTime()
            }
            VideoPlayerEvent.PositionDiscontinuity -> {
                if (!videoState.isPreviewerOpen || pagerState.settledPage != page) return@setEventListener
                videoState.isSeeking = false
            }
            VideoPlayerEvent.FirstFrameRendered -> {
                // Always track per-page mounting (even while this page is only
                // preloaded as an adjacent page) so the page is ready to show
                // without a loading state the moment it becomes the active page.
                firstFrameRendered = true
            }
        }
    }

    LaunchedEffect(firstFrameRendered) {
        if (firstFrameRendered) {
            viewerAlpha.snapTo(1F)
            onMounted()
        }
    }

    LaunchedEffect(controller, pagerState.settledPage, videoState.isPreviewerOpen) {
        if (!videoState.isPreviewerOpen) {
            if (model.mediaId.isNotEmpty() && controller.currentPosition > 0) {
                val mediaId = model.mediaId
                val pos = controller.currentPosition
                TempData.videoPlayProgressMap[mediaId] = pos
                coIO { progressDao.upsert(DVideoPlayProgress(mediaId, pos, TimeHelper.now())) }
            }
            controller.abandonAudioFocus()
            controller.stop()
            preparedPath = null
            return@LaunchedEffect
        }
        val isActive = pagerState.settledPage == page
        if (isActive) {
            videoState.initData(controller)
            val expectedTotalMs = ((model.data as? DVideo)?.duration ?: 0L) * 1000
            if (expectedTotalMs > 0L) {
                videoState.totalTime = expectedTotalMs
            }
            val savedPos = if (model.mediaId.isNotEmpty()) TempData.videoPlayProgressMap[model.mediaId] else null
            val atEnd = savedPos != null && savedPos > 0 && expectedTotalMs > 0 && savedPos >= expectedTotalMs - 1000
            if (savedPos != null && savedPos > 0L && !atEnd) {
                videoState.currentTime = savedPos
            }
            if (preparedPath != model.path) {
                controller.setMediaItem(model.path)
                controller.prepare()
                preparedPath = model.path
            }
            if (savedPos != null && savedPos > 0 && !atEnd) {
                controller.seekTo(savedPos)
            }
            controller.requestAudioFocus()
            controller.play()
        } else if (abs(page - pagerState.settledPage) <= 1) {
            // Preload the adjacent page: prepare its video so it is buffered and
            // its first frame is rendered ahead of time — switching to it then
            // never shows a loading state. Also pause + release audio focus so a
            // previously-active video stops playing as soon as the user swipes away.
            if (preparedPath != model.path) {
                controller.setMediaItem(model.path)
                controller.prepare()
                preparedPath = model.path
            }
            controller.pause()
            controller.abandonAudioFocus()
        } else {
            controller.stop()
            preparedPath = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val mediaId = model.mediaId
            val pos = controller.currentPosition
            if (mediaId.isNotEmpty() && pos > 0) {
                TempData.videoPlayProgressMap[mediaId] = pos
                coIO { progressDao.upsert(DVideoPlayProgress(mediaId, pos, TimeHelper.now())) }
            }
            controller.abandonAudioFocus()
            controller.stop()
            controller.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { clip = boundClip; alpha = viewerAlpha.value }
            .onSizeChanged { bSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation() }
                    if (released == null) {
                        gesture.onLongPress(down.position)
                        videoState.startSpeedBoost()
                        waitForUpOrCancellation()
                        videoState.stopSpeedBoost()
                    }
                }
            }
            .pointerInput(videoSpecified) {
                if (videoSpecified) {
                    detectTransformGestures(
                        onTap = gesture.onTap,
                        onDoubleTap = gesture.onDoubleTap,
                        gestureStart = gesture.gestureStart,
                        gestureEnd = gesture.gestureEnd,
                        onGesture = gesture.onGesture,
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val videoModifier = Modifier.graphicsLayer {
            if (videoSpecified) {
                scaleX = scale; scaleY = scale
                translationX = offsetX; translationY = offsetY
                rotationZ = rotation
            }
        }
        val surfaceModifier = if (videoState.isFullscreenMode) {
            videoModifier
                .align(Alignment.Center)
                .fillMaxSize()
        } else {
            videoModifier
                .align(Alignment.Center)
                .size(
                    LocalDensity.current.run { sizing.displaySize.width.toDp() },
                    LocalDensity.current.run { sizing.displaySize.height.toDp() },
                )
        }
        VideoPlayerSurface(
            modifier = surfaceModifier,
            controller = controller,
            videoState = videoState,
        )
    }
}
