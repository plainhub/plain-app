package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import androidx.compose.animation.core.AnimationSpec
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun TransformContentState.notifyEnterChanged() {
    scope.launch {
        listOf(
            scope.async { displayWidth.snapTo(displayRatioSize.width) },
            scope.async { displayHeight.snapTo(displayRatioSize.height) },
            scope.async { graphicScaleX.snapTo(fitScale) },
            scope.async { graphicScaleY.snapTo(fitScale) },
            scope.async { offsetX.snapTo(fitOffsetX) },
            scope.async { offsetY.snapTo(fitOffsetY) },
        ).awaitAll()
    }
}

suspend fun TransformContentState.exitTransform(
    animationSpec: AnimationSpec<Float>? = null
) = suspendCoroutine<Unit> { c ->
    val currentAnimateSpec = animationSpec ?: defaultAnimationSpec
    scope.launch {
        listOf(
            scope.async { displayWidth.animateTo(srcSize.width.toFloat(), currentAnimateSpec) },
            scope.async { displayHeight.animateTo(srcSize.height.toFloat(), currentAnimateSpec) },
            scope.async { graphicScaleX.animateTo(1F, currentAnimateSpec) },
            scope.async { graphicScaleY.animateTo(1F, currentAnimateSpec) },
            scope.async { offsetX.animateTo(srcPosition.x, currentAnimateSpec) },
            scope.async { offsetY.animateTo(srcPosition.y, currentAnimateSpec) },
        ).awaitAll()
        onAction = false
        onActionTarget = null
        c.resume(Unit)
    }
}

suspend fun TransformContentState.enterTransform(
    itemState: TransformItemState,
    animationSpec: AnimationSpec<Float>? = null
) = suspendCoroutine<Unit> { c ->
    val currentAnimationSpec = animationSpec ?: defaultAnimationSpec
    this.itemState = itemState
    displayWidth = androidx.compose.animation.core.Animatable(srcSize.width.toFloat())
    displayHeight = androidx.compose.animation.core.Animatable(srcSize.height.toFloat())
    graphicScaleX = androidx.compose.animation.core.Animatable(1F)
    graphicScaleY = androidx.compose.animation.core.Animatable(1F)
    offsetX = androidx.compose.animation.core.Animatable(srcPosition.x)
    offsetY = androidx.compose.animation.core.Animatable(srcPosition.y)
    onActionTarget = true
    onAction = true
    scope.launch {
        reset(currentAnimationSpec)
        c.resume(Unit)
        onActionTarget = null
    }
}

suspend fun TransformContentState.reset(animationSpec: AnimationSpec<Float>? = null) {
    val currentAnimationSpec = animationSpec ?: defaultAnimationSpec
    listOf(
        scope.async { displayWidth.animateTo(displayRatioSize.width, currentAnimationSpec) },
        scope.async { displayHeight.animateTo(displayRatioSize.height, currentAnimationSpec) },
        scope.async { graphicScaleX.animateTo(fitScale, currentAnimationSpec) },
        scope.async { graphicScaleY.animateTo(fitScale, currentAnimationSpec) },
        scope.async { offsetX.animateTo(fitOffsetX, currentAnimationSpec) },
        scope.async { offsetY.animateTo(fitOffsetY, currentAnimationSpec) },
    ).awaitAll()
}
