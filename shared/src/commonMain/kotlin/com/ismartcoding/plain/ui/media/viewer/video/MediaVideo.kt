package com.ismartcoding.plain.ui.media.viewer.video

import com.ismartcoding.plain.features.media.MediaClient

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import com.ismartcoding.plain.models.Video as MediaVideo
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.ui.platform.media.VideoPlayerSurface
import com.ismartcoding.plain.ui.media.viewer.DEFAULT_OFFSET_X
import com.ismartcoding.plain.ui.media.viewer.DEFAULT_OFFSET_Y
import com.ismartcoding.plain.ui.media.viewer.DEFAULT_ROTATION
import com.ismartcoding.plain.ui.media.viewer.DEFAULT_SCALE
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.media.viewer.RawGesture
import com.ismartcoding.plain.ui.media.viewer.SizeChangeContent
import com.ismartcoding.plain.ui.media.viewer.detectTransformGestures

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
                is MediaVideo -> model.initAsync(data)
                else -> {
                    val meta = model.source.media.videoInfo(model.path)
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

    var firstFrameRendered by remember { mutableStateOf(false) }
    val controller = rememberMediaVideoController(pagerState, videoState, page, model) {
        firstFrameRendered = true
    }
    LaunchedEffect(firstFrameRendered) {
        if (firstFrameRendered) {
            viewerAlpha.snapTo(1F)
            onMounted()
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
