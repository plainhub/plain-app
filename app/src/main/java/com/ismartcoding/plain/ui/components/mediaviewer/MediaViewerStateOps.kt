package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val defaultAnimateSpec: AnimationSpec<Float> = SpringSpec()

/**
 * 设置回初始值
 */
suspend fun MediaViewerState.reset(animationSpec: AnimationSpec<Float> = defaultAnimateSpec) {
    coroutineScope {
        launch {
            rotation.animateTo(DEFAULT_ROTATION, animationSpec)
            resetTimeStamp = System.currentTimeMillis()
        }
        launch {
            offsetX.animateTo(DEFAULT_OFFSET_X, animationSpec)
            resetTimeStamp = System.currentTimeMillis()
        }
        launch {
            offsetY.animateTo(DEFAULT_OFFSET_Y, animationSpec)
            resetTimeStamp = System.currentTimeMillis()
        }
        launch {
            scale.animateTo(DEFAULT_SCALE, animationSpec)
            resetTimeStamp = System.currentTimeMillis()
        }
    }
}

/**
 * 放大到最大
 */
private suspend fun MediaViewerState.scaleToMax(
    offset: Offset,
    animationSpec: AnimationSpec<Float>? = null,
) {
    val currentAnimateSpec = animationSpec ?: defaultAnimateSpec
    var bcx = (containerSize.width / 2 - offset.x) * maxScale
    val boundX = getBound(defaultSize.width.toFloat() * maxScale, containerSize.width.toFloat())
    bcx = limitToBound(bcx, boundX)
    var bcy = (containerSize.height / 2 - offset.y) * maxScale
    val boundY = getBound(defaultSize.height.toFloat() * maxScale, containerSize.height.toFloat())
    bcy = limitToBound(bcy, boundY)
    coroutineScope {
        launch { scale.animateTo(maxScale, currentAnimateSpec) }
        launch { offsetX.animateTo(bcx, currentAnimateSpec) }
        launch { offsetY.animateTo(bcy, currentAnimateSpec) }
    }
}

/**
 * 放大或缩小
 */
suspend fun MediaViewerState.toggleScale(
    offset: Offset,
    animationSpec: AnimationSpec<Float> = defaultAnimateSpec,
) {
    if (scale.value != 1F) {
        reset(animationSpec)
    } else {
        scaleToMax(offset, animationSpec)
    }
}

/**
 * 修正offsetX,offsetY的位置
 */
suspend fun MediaViewerState.fixToBound() {
    val boundX = getBound(defaultSize.width.toFloat() * scale.value, containerSize.width.toFloat())
    val boundY = getBound(defaultSize.height.toFloat() * scale.value, containerSize.height.toFloat())
    val limitX = limitToBound(offsetX.value, boundX)
    val limitY = limitToBound(offsetY.value, boundY)
    offsetX.snapTo(limitX)
    offsetY.snapTo(limitY)
}
