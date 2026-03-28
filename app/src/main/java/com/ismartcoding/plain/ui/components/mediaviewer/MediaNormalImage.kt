package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.util.UnstableApi
import coil3.imageLoader
import com.ismartcoding.lib.extensions.isUrl
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.DEFAULT_CROSS_FADE_ANIMATE_SPEC
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bSize by remember { mutableStateOf(IntSize(0, 0)) }
    val bRatio by remember { derivedStateOf { bSize.width.toFloat() / bSize.height.toFloat() } }
    var oSize by remember { mutableStateOf(IntSize(0, 0)) }
    val oRatio by remember { derivedStateOf { oSize.width.toFloat() / oSize.height.toFloat() } }
    var widthFixed by remember { mutableStateOf(false) }
    val superSize by remember { derivedStateOf { oSize.height > bSize.height && oSize.width > bSize.width } }
    val uSize by remember {
        derivedStateOf {
            if (oRatio > bRatio) {
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
        val maxScale = when {
            superSize -> oSize.width.toFloat() / uSize.width.toFloat()
            widthFixed -> bSize.height.toFloat() / uSize.height.toFloat()
            else -> bSize.width.toFloat() / uSize.width.toFloat()
        }
        onSizeChange(SizeChangeContent(defaultSize = uSize, containerSize = bSize, maxScale = maxScale))
    }

    var imageSpecified by remember { mutableStateOf(false) }
    val viewerAlpha = remember { Animatable(0F) }
    fun goMounted() {
        scope.launch { viewerAlpha.animateTo(1F, DEFAULT_CROSS_FADE_ANIMATE_SPEC); onMounted() }
    }

    var painter by remember { mutableStateOf<Painter?>(null) }
    if (model.path.isUrl()) {
        painter = rememberCoilImagePainter(model.path)
        var isMounted by remember { mutableStateOf(false) }
        imageSpecified = painter!!.intrinsicSize.isSpecified
        LaunchedEffect(key1 = painter!!.intrinsicSize) {
            if (imageSpecified) {
                oSize = IntSize(painter!!.intrinsicSize.width.toInt(), painter!!.intrinsicSize.height.toInt())
                model.intrinsicSize = oSize
                model.size = context.imageLoader.diskCache?.openSnapshot(model.path)?.data?.toFile()?.length() ?: 0L
                if (!isMounted) { isMounted = true; goMounted() }
            }
        }
    } else {
        imageSpecified = true
        if (model.intrinsicSize == IntSize.Zero) model.initImageAsync()
        oSize = model.intrinsicSize
        goMounted()
    }

    Box(
        modifier = modifier.fillMaxSize()
            .graphicsLayer { clip = boundClip; alpha = viewerAlpha.value }
            .onSizeChanged { bSize = it }
            .pointerInput(Unit) { detectTapGestures(onLongPress = gesture.onLongPress) }
            .pointerInput(key1 = imageSpecified) {
                if (imageSpecified) detectTransformGestures(
                    onTap = gesture.onTap, onDoubleTap = gesture.onDoubleTap,
                    gestureStart = gesture.gestureStart, gestureEnd = gesture.gestureEnd, onGesture = gesture.onGesture,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val imageModifier = Modifier.graphicsLayer {
            if (imageSpecified) { scaleX = scale; scaleY = scale; translationX = offsetX; translationY = offsetY; rotationZ = rotation }
        }
        MediaNormalImageContent(imageModifier = imageModifier, painter = painter, model = model, uSize = uSize)
    }
}
