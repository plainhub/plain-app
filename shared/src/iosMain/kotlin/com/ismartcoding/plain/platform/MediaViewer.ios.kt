package com.ismartcoding.plain.platform

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import com.ismartcoding.plain.lib.extensions.isUrl
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_CROSS_FADE_ANIMATE_SPEC
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_OFFSET_X
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_OFFSET_Y
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_ROTATION
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_SCALE
import com.ismartcoding.plain.ui.components.mediaviewer.GestureScope
import com.ismartcoding.plain.ui.components.mediaviewer.MediaViewerState
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import com.ismartcoding.plain.ui.components.mediaviewer.RawGesture
import com.ismartcoding.plain.ui.components.mediaviewer.SizeChangeContent
import com.ismartcoding.plain.ui.components.mediaviewer.detectTransformGestures
import com.ismartcoding.plain.ui.components.mediaviewer.rememberMediaViewerGesture
import com.ismartcoding.plain.ui.components.mediaviewer.video.MediaVideo
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewer(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    videoState: VideoState,
    page: Int,
    model: Any?,
    state: MediaViewerState,
    gesture: GestureScope,
    boundClip: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val (rawGesture, sizeChange) = rememberMediaViewerGesture(state, gesture, scope)

    Box(modifier = modifier) {
        val onMounted: () -> Unit = { scope.launch { state.mountedFlow.emit(true) } }

        when (model) {
            is PreviewItem -> {
                if (model.isVideo()) {
                    MediaVideo(
                        pagerState = pagerState,
                        videoState = videoState,
                        page = page,
                        model = model,
                        scale = state.scale.value,
                        offsetX = state.offsetX.value,
                        offsetY = state.offsetY.value,
                        rotation = state.rotation.value,
                        gesture = rawGesture,
                        onMounted = onMounted,
                        onSizeChange = sizeChange,
                        boundClip = boundClip,
                    )
                } else {
                    MediaNormalImage(
                        model = model,
                        scale = state.scale.value,
                        offsetX = state.offsetX.value,
                        offsetY = state.offsetY.value,
                        rotation = state.rotation.value,
                        gesture = rawGesture,
                        onSizeChange = sizeChange,
                        onMounted = onMounted,
                        boundClip = boundClip,
                    )
                }
            }
        }
    }
}

@Composable
fun MediaNormalImage(
    modifier: Modifier = Modifier,
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
    var bSize by remember { mutableStateOf(IntSize(0, 0)) }
    val bRatio by remember { derivedStateOf { bSize.width.toFloat() / bSize.height.toFloat() } }
    var oSize by remember { mutableStateOf(IntSize(0, 0)) }
    val oRatio by remember { derivedStateOf { oSize.width.toFloat() / oSize.height.toFloat() } }
    var widthFixed by remember { mutableStateOf(false) }
    val superSize by remember { derivedStateOf { oSize.height > bSize.height && oSize.width > bSize.width } }
    val uSize by remember {
        derivedStateOf {
            if (bSize == IntSize.Zero) {
                IntSize.Zero
            } else if (oRatio > bRatio) {
                val uW = bSize.width; widthFixed = true
                IntSize(uW, (uW / oRatio).toInt())
            } else {
                val uH = bSize.height; widthFixed = false
                IntSize((uH * oRatio).toInt(), uH)
            }
        }
    }
    val rSize by remember { derivedStateOf { IntSize((uSize.width * scale).toInt(), (uSize.height * scale).toInt()) } }

    LaunchedEffect(key1 = oSize, key2 = bSize, key3 = rSize) {
        if (uSize != IntSize.Zero) {
            val maxScale = when {
                superSize -> oSize.width.toFloat() / uSize.width.toFloat()
                widthFixed -> bSize.height.toFloat() / uSize.height.toFloat()
                else -> bSize.width.toFloat() / uSize.width.toFloat()
            }
            onSizeChange(SizeChangeContent(defaultSize = uSize, containerSize = bSize, maxScale = maxScale))
        }
    }

    val viewerAlpha = remember { Animatable(0F) }
    fun goMounted() {
        scope.launch { viewerAlpha.animateTo(1F, DEFAULT_CROSS_FADE_ANIMATE_SPEC); onMounted() }
    }

    var imageSpecified by remember { mutableStateOf(false) }
    if (model.path.isUrl()) {
        imageSpecified = true
    } else {
        imageSpecified = true
        if (model.intrinsicSize == IntSize.Zero) model.initImageAsync()
        oSize = model.intrinsicSize
        LaunchedEffect(model.intrinsicSize) {
            if (model.intrinsicSize != IntSize.Zero) {
                oSize = model.intrinsicSize
                goMounted()
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
            .graphicsLayer { clip = boundClip; alpha = viewerAlpha.value }
            .onSizeChanged { bSize = it }
            .pointerInput(Unit) { detectTapGestures(onLongPress = gesture.onLongPress) }
            .pointerInput(key1 = imageSpecified) {
                if (imageSpecified) detectTransformGestures(
                    onTap = gesture.onTap,
                    onDoubleTap = gesture.onDoubleTap,
                    gestureStart = gesture.gestureStart,
                    gestureEnd = gesture.gestureEnd,
                    onGesture = gesture.onGesture,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val imageModifier = Modifier.graphicsLayer {
            if (imageSpecified && uSize != IntSize.Zero) {
                scaleX = scale; scaleY = scale
                translationX = offsetX; translationY = offsetY
                rotationZ = rotation
            }
        }
        if (uSize != IntSize.Zero) {
            val isSvg = model.path.endsWith(".svg", true)
            AsyncImage(
                model = model.path,
                contentDescription = model.path,
                contentScale = ContentScale.Fit,
                modifier = imageModifier
                    .let { if (isSvg) it.background(Color.White) else it }
                    .size(
                        LocalDensity.current.run { uSize.width.toDp() },
                        LocalDensity.current.run { uSize.height.toDp() },
                    ),
            )
        }
    }
}
