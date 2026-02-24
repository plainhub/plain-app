package com.ismartcoding.plain.ui.components.mediaviewer.previewer

import androidx.compose.animation.core.MutableTransitionState
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// Default scale threshold for drag-to-close
const val DEFAULT_SCALE_TO_CLOSE_MIN_VALUE = 0.9F

enum class VerticalDragType {
    // No vertical gesture
    None,

    // Only enable downward drag gesture
    Down,

    // Support both upward and downward drag gestures
    UpAndDown,
    ;
}

/**
 * Adds vertical drag capability
 */
@OptIn(ExperimentalFoundationApi::class)
open class PreviewerVerticalDragState(
    scope: CoroutineScope = MainScope(),
    verticalDragType: VerticalDragType = VerticalDragType.None,
    scaleToCloseMinValue: Float = DEFAULT_SCALE_TO_CLOSE_MIN_VALUE,
    pagerState: PagerState,
) : PreviewerTransformState(scope, pagerState) {

    /**
     * Shrink and close viewer container
     */
    private suspend fun viewerContainerShrinkDown() {
        stateCloseStart()
        viewerContainerState?.cancelOpenTransform()
        listOf(
            scope.async {
                // Hide content after exit
                viewerContainerState?.transformContentAlpha?.snapTo(0F)
            },
            scope.async {
                // Animate UI to hide
                uiAlpha.animateTo(0F, DEFAULT_SOFT_ANIMATION_SPEC)
            },
            scope.async {
                animateContainerVisibleState = MutableTransitionState(false)
            }
        ).awaitAll()
        ticket.awaitNextTicket()
        stateCloseEnd() // put this line before reset will fix the UI animation bug
        transformState?.setExitState()
    }

    /**
     * Respond to downward drag to close
     */
    private suspend fun dragDownClose() {
        // Refresh transform position
        transformState?.notifyEnterChanged()
        // Close loading
        viewerContainerState?.showLoading = false
        // Wait for next frame to ensure transform position is refreshed
        ticket.awaitNextTicket()
        // Copy container position to transform
        viewerContainerState?.copyViewerContainerStateToTransformState()
        // Reset container
        viewerContainerState?.resetImmediately()
        // Switch to transform
        transformSnapToViewer(false)
        // Wait for next frame
        ticket.awaitNextTicket()
        // Execute transform close
        closeTransform()
        // Remove loading restriction
        viewerContainerState?.showLoading = true
    }

    /**
     * Set up drag-to-close gesture.
     * Use custom awaitEachGesture instead of detectVerticalDragGestures,
     * so that when multi-touch is detected, vertical drag is immediately aborted,
     * restoring image viewer's two-finger zoom/pan gestures,
     * ensuring two-finger operation does not degrade to single-sided movement.
     * @param pointerInputScope PointerInputScope
     */
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
                    getKey?.apply {
                        findTransformItem(invoke(pagerState.currentPage))?.apply {
                            transformItemState = this
                        }
                    }
                    if (canTransformOut) {
                        transformState?.setEnterState()
                    } else {
                        transformState?.setExitState()
                    }
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
                            dragActivated = false
                            vStartOffset = null
                            vOrientationDown = null
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
                                    // Horizontal — unblock pager and stop vertical handling
                                    pagerUserScrollEnabled = true
                                    mediaViewerState?.allowGestureInput = true
                                    dragActivated = false
                                    vStartOffset = null
                                    continue
                                }
                                // Vertical — block pager
                                pagerUserScrollEnabled = false
                                vOrientationDown = dy > 0
                            }

                            if (vOrientationDown == true || verticalDragType == VerticalDragType.UpAndDown) {
                                val containerHeight = viewerContainerState!!.containerSize.height
                                val scale = (containerHeight - dy.absoluteValue).div(containerHeight)
                                scope.launch {
                                    uiAlpha.snapTo(scale)
                                    viewerContainerState?.offsetX?.snapTo(dx)
                                    viewerContainerState?.offsetY?.snapTo(dy)
                                    viewerContainerState?.scale?.snapTo(scale)
                                }
                                change.consume()
                            } else {
                                pagerUserScrollEnabled = true
                                mediaViewerState?.allowGestureInput = true
                                dragActivated = false
                                vStartOffset = null
                            }
                        }

                        dragActivated && event.type == PointerEventType.Release -> {
                            pagerUserScrollEnabled = true
                            mediaViewerState?.allowGestureInput = true
                            vStartOffset = null
                            vOrientationDown = null
                            dragActivated = false

                            if ((viewerContainerState?.scale?.value ?: 1F) < scaleToCloseMinValue) {
                                scope.launch {
                                    if (getKey != null && canTransformOut) {
                                        val key = getKey!!.invoke(pagerState.currentPage)
                                        val transformItem = findTransformItem(key)
                                        if (transformItem != null) {
                                            dragDownClose()
                                        } else {
                                            viewerContainerShrinkDown()
                                        }
                                    } else {
                                        viewerContainerShrinkDown()
                                    }
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

                pagerUserScrollEnabled = true
                mediaViewerState?.allowGestureInput = true
            }
        }
    }

    /**
     * Type of vertical gesture enabled
     */
    var verticalDragType by mutableStateOf(verticalDragType)

    /**
     * Scale threshold for drag-to-close. When scale is less than this value, close; otherwise, reset.
     */
    private var scaleToCloseMinValue by mutableFloatStateOf(scaleToCloseMinValue)

}