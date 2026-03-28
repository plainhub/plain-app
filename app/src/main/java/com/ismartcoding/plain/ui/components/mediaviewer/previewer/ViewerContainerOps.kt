package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withContext

internal suspend fun ViewerContainerState.doAwaitOpenTransform() {
    openTransformJob = scope.async {
        doAwaitViewerLoading()
        transformSnapToViewer(true)
    }
    openTransformJob?.await()
    openTransformJob = null
}

internal suspend fun ViewerContainerState.doAwaitViewerLoading() {
    viewerState.mountedFlow.apply {
        withContext(Dispatchers.Default) { takeWhile { !it }.collect() }
    }
}

internal suspend fun ViewerContainerState.doCopyViewerContainerStateToTransformState() {
    transformState.apply {
        val targetScale = scale.value * fitScale
        graphicScaleX.snapTo(targetScale)
        graphicScaleY.snapTo(targetScale)
        val centerOffsetY = (containerSize.height - realSize.height).div(2)
        val centerOffsetX = (containerSize.width - realSize.width).div(2)
        offsetY.snapTo(centerOffsetY + this@doCopyViewerContainerStateToTransformState.offsetY.value)
        offsetX.snapTo(centerOffsetX + this@doCopyViewerContainerStateToTransformState.offsetX.value)
    }
}

internal suspend fun ViewerContainerState.doCopyViewerPosToContent(itemState: TransformItemState) {
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
