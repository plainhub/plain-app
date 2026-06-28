package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import com.ismartcoding.plain.ui.components.mediaviewer.MediaViewerState
import com.ismartcoding.plain.ui.components.mediaviewer.rememberViewerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withContext

internal class ViewerContainerState(
    var scope: CoroutineScope = MainScope(),
    var transformState: TransformContentState = TransformContentState(),
    var viewerState: MediaViewerState = MediaViewerState(),
) {
    internal var transformContentAlpha = Animatable(0F)
    internal var viewerContainerAlpha = Animatable(1F)
    internal var showLoading by mutableStateOf(true)
    internal var openTransformJob: Deferred<Unit>? = null
    var containerSize: IntSize by mutableStateOf(IntSize.Zero)
    var offsetX = Animatable(0F)
    var offsetY = Animatable(0F)
    var scale = Animatable(1F)

    internal fun cancelOpenTransform() {
        openTransformJob?.cancel()
        openTransformJob = null
    }

    internal suspend fun transformSnapToViewer(isViewer: Boolean) {
        if (isViewer) {
            transformContentAlpha.snapTo(0F)
            viewerContainerAlpha.snapTo(1F)
        } else {
            transformContentAlpha.snapTo(1F)
            viewerContainerAlpha.snapTo(0F)
        }
    }

    suspend fun reset(animationSpec: AnimationSpec<Float> = DEFAULT_SOFT_ANIMATION_SPEC) {
        scope.apply {
            listOf(
                async { offsetX.animateTo(0F, animationSpec) }, async { offsetY.animateTo(0F, animationSpec) },
                async { scale.animateTo(1F, animationSpec) }).awaitAll()
        }
    }

    suspend fun resetImmediately() {
        offsetX.snapTo(0F)
        offsetY.snapTo(0F)
        scale.snapTo(1F)
    }

    suspend fun awaitOpenTransform() {
        openTransformJob = scope.async {
            doAwaitViewerLoading()
            transformSnapToViewer(true)
        }
        openTransformJob?.await()
        openTransformJob = null
    }

    suspend fun doAwaitViewerLoading() {
        viewerState.mountedFlow.apply {
            withContext(Dispatchers.Default) { takeWhile { !it }.collect() }
        }
    }

    suspend fun copyViewerContainerStateToTransformState() {
        transformState.apply {
            val targetScale = scale.value * fitScale
            graphicScaleX.snapTo(targetScale)
            graphicScaleY.snapTo(targetScale)
            val centerOffsetY = (containerSize.height - realSize.height).div(2)
            val centerOffsetX = (containerSize.width - realSize.width).div(2)
            offsetY.snapTo(centerOffsetY + offsetY.value)
            offsetX.snapTo(centerOffsetX + offsetX.value)
        }
    }

    suspend fun copyViewerPosToContent(itemState: TransformItemState) {
        transformState.apply {
            this@apply.itemState = itemState
            containerSize = viewerState.containerSize
            val scale = viewerState.scale
            val offsetX = viewerState.offsetX
            val offsetY = viewerState.offsetY
            val rw = fitSize.width * scale.value
            val rh = fitSize.height * scale.value
            val goOffsetX = (containerSize.width - rw).div(2) + offsetX.value
            val goOffsetY = (containerSize.height - rh).div(2) + offsetY.value
            val fixScale = fitScale * scale.value
            graphicScaleX.snapTo(fixScale)
            graphicScaleY.snapTo(fixScale)
            displayWidth.snapTo(displayRatioSize.width)
            displayHeight.snapTo(displayRatioSize.height)
            this@apply.offsetX.snapTo(goOffsetX)
            this@apply.offsetY.snapTo(goOffsetY)
        }
    }


    companion object {
        val Saver: Saver<ViewerContainerState, *> = mapSaver(
            save = { mapOf<String, Any>(it::offsetX.name to it.offsetX.value, it::offsetY.name to it.offsetY.value, it::scale.name to it.scale.value) },
            restore = {
                ViewerContainerState().also { s ->
                    s.offsetX = Animatable(it[s::offsetX.name] as Float)
                    s.offsetY = Animatable(it[s::offsetY.name] as Float)
                    s.scale = Animatable(it[s::scale.name] as Float)
                }
            }
        )
    }
}
