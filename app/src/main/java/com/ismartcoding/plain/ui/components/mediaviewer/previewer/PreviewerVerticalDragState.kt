package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.util.fastAny
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

const val DEFAULT_SCALE_TO_CLOSE_MIN_VALUE = 0.9F

enum class VerticalDragType { None, Down, UpAndDown }

@OptIn(ExperimentalFoundationApi::class)
open class PreviewerVerticalDragState(
    scope: CoroutineScope = MainScope(),
    verticalDragType: VerticalDragType = VerticalDragType.None,
    scaleToCloseMinValue: Float = DEFAULT_SCALE_TO_CLOSE_MIN_VALUE,
    pagerState: PagerState,
) : PreviewerTransformState(scope, pagerState) {

    var verticalDragType by mutableStateOf(verticalDragType)
    private var scaleToCloseMinValue by mutableFloatStateOf(scaleToCloseMinValue)

    suspend fun verticalDrag(pointerInputScope: PointerInputScope) {
        if (verticalDragType == VerticalDragType.None) return
        pointerInputScope.apply {
            awaitEachGesture {
                val firstDown = awaitFirstDown(requireUnconsumed = false)
                var vStartOffset: Offset? = null
                var vOrientationDown: Boolean? = null
                var dragActivated = false
                var directionLocked = false

                if (mediaViewerState != null) {
                    var transformItemState: TransformItemState? = null
                    getKey?.apply { findTransformItem(invoke(pagerState.currentPage))?.apply { transformItemState = this } }
                    if (canTransformOut) transformState?.setEnterState() else transformState?.setExitState()
                    transformState?.itemState = transformItemState
                    if (mediaViewerState?.scale?.value == 1F) {
                        vStartOffset = firstDown.position
                        dragActivated = true
                        mediaViewerState?.allowGestureInput = false
                    }
                }

                do {
                    val event = awaitPointerEvent()
                    if (event.changes.size > 1) {
                        if (dragActivated) {
                            scope.launch { uiAlpha.animateTo(1F, DEFAULT_SOFT_ANIMATION_SPEC) }
                            scope.launch { viewerContainerState?.reset(DEFAULT_SOFT_ANIMATION_SPEC) }
                            dragActivated = false; vStartOffset = null; vOrientationDown = null
                        }
                        pagerUserScrollEnabled = true
                        mediaViewerState?.allowGestureInput = true
                        break
                    }
                    val change = event.changes.firstOrNull() ?: break
                    when {
                        dragActivated && event.type == PointerEventType.Move -> {
                            if (vStartOffset == null || viewerContainerState == null) continue
                            val dx = change.position.x - vStartOffset!!.x
                            val dy = change.position.y - vStartOffset!!.y
                            if (!directionLocked) {
                                if (dx.absoluteValue < viewConfiguration.touchSlop && dy.absoluteValue < viewConfiguration.touchSlop) continue
                                directionLocked = true
                                if (dx.absoluteValue > dy.absoluteValue) {
                                    pagerUserScrollEnabled = true; mediaViewerState?.allowGestureInput = true
                                    dragActivated = false; vStartOffset = null; continue
                                }
                                pagerUserScrollEnabled = false; vOrientationDown = dy > 0
                            }
                            if (vOrientationDown == true || verticalDragType == VerticalDragType.UpAndDown) {
                                val containerHeight = viewerContainerState!!.containerSize.height
                                val scale = (containerHeight - dy.absoluteValue).div(containerHeight)
                                scope.launch {
                                    uiAlpha.snapTo(scale); viewerContainerState?.offsetX?.snapTo(dx)
                                    viewerContainerState?.offsetY?.snapTo(dy); viewerContainerState?.scale?.snapTo(scale)
                                }
                                change.consume()
                            } else {
                                pagerUserScrollEnabled = true; mediaViewerState?.allowGestureInput = true
                                dragActivated = false; vStartOffset = null
                            }
                        }
                        dragActivated && event.type == PointerEventType.Release -> {
                            pagerUserScrollEnabled = true; mediaViewerState?.allowGestureInput = true
                            vStartOffset = null; vOrientationDown = null; dragActivated = false
                            if ((viewerContainerState?.scale?.value ?: 1F) < scaleToCloseMinValue) {
                                scope.launch {
                                    if (getKey != null && canTransformOut) {
                                        val key = getKey!!.invoke(pagerState.currentPage)
                                        if (findTransformItem(key) != null) dragDownClose() else viewerContainerShrinkDown()
                                    } else viewerContainerShrinkDown()
                                    uiAlpha.snapTo(1F)
                                }
                            } else {
                                scope.launch { uiAlpha.animateTo(1F, DEFAULT_SOFT_ANIMATION_SPEC) }
                                scope.launch { viewerContainerState?.reset(DEFAULT_SOFT_ANIMATION_SPEC) }
                            }
                            break
                        }
                    }
                } while (event.changes.fastAny { it.pressed })
                pagerUserScrollEnabled = true; mediaViewerState?.allowGestureInput = true
            }
        }
    }
}
