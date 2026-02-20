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
                // Wait for the first finger down (not requiring unconsumed, so parent can also receive event)
                val firstDown = awaitFirstDown(requireUnconsumed = false)

                // Record initial position and direction
                var vStartOffset: Offset? = null
                var vOrientationDown: Boolean? = null
                // Mark whether vertical drag is activated
                var dragActivated = false

                // Initialize transform state (same as original logic)
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

                    // Only allow drag-to-close when viewer scale is 1
                    if (mediaViewerState?.scale?.value == 1F) {
                        vStartOffset = firstDown.position
                        dragActivated = true
                        // Disable viewer gesture input when entering drag-to-close to avoid conflict
                        mediaViewerState?.allowGestureInput = false
                    }
                }

                do {
                    val event = awaitPointerEvent()

                    // --- Key fix: detect multi-touch ---
                    // When two or more fingers are detected, immediately abort vertical drag,
                    // reset container position and restore viewer gesture input,
                    // let inner two-finger zoom/pan gestures take over.
                    if (event.changes.size > 1) {
                        if (dragActivated) {
                            // Smoothly restore container to initial state
                            scope.launch {
                                uiAlpha.animateTo(1F, DEFAULT_SOFT_ANIMATION_SPEC)
                            }
                            scope.launch {
                                viewerContainerState?.reset(DEFAULT_SOFT_ANIMATION_SPEC)
                            }
                            dragActivated = false
                            vStartOffset = null
                            vOrientationDown = null
                        }
                        // Restore viewer gesture input, let inner detectTransformGestures handle two-finger operation
                        mediaViewerState?.allowGestureInput = true
                        // Exit this gesture loop, do not consume event, let inner handler take over
                        break
                    }

                    val change = event.changes.firstOrNull() ?: break

                    when {
                        // Finger move: handle vertical drag logic
                        dragActivated && event.type == PointerEventType.Move -> {
                            if (vStartOffset == null || viewerContainerState == null) continue

                            val dragAmountY = change.position.y - change.previousPosition.y
                            if (vOrientationDown == null) vOrientationDown = dragAmountY > 0

                            if (vOrientationDown == true || verticalDragType == VerticalDragType.UpAndDown) {
                                val offsetY = change.position.y - vStartOffset!!.y
                                val offsetX = change.position.x - vStartOffset!!.x
                                val containerHeight = viewerContainerState!!.containerSize.height
                                val scale = (containerHeight - offsetY.absoluteValue).div(containerHeight)
                                scope.launch {
                                    uiAlpha.snapTo(scale)
                                    viewerContainerState?.offsetX?.snapTo(offsetX)
                                    viewerContainerState?.offsetY?.snapTo(offsetY)
                                    viewerContainerState?.scale?.snapTo(scale)
                                }
                            } else {
                                // Not downward drag, restore gesture input, avoid UI lag
                                mediaViewerState?.allowGestureInput = true
                                dragActivated = false
                            }
                        }

                        // Finger release: determine whether to close or reset
                        dragActivated && event.type == PointerEventType.Release -> {
                            vStartOffset = null
                            vOrientationDown = null
                            dragActivated = false
                            mediaViewerState?.allowGestureInput = true

                            if ((viewerContainerState?.scale?.value ?: 1F) < scaleToCloseMinValue) {
                                scope.launch {
                                    if (getKey != null && canTransformOut) {
                                        val key = getKey!!.invoke(pagerState.currentPage)
                                        val transformItem = findTransformItem(key)
                                        // If item is in view, execute transform close, otherwise shrink close
                                        if (transformItem != null) {
                                            dragDownClose()
                                        } else {
                                            viewerContainerShrinkDown()
                                        }
                                    } else {
                                        viewerContainerShrinkDown()
                                    }
                                    // Restore UI after animation ends
                                    uiAlpha.snapTo(1F)
                                }
                            } else {
                                scope.launch {
                                    uiAlpha.animateTo(1F, DEFAULT_SOFT_ANIMATION_SPEC)
                                }
                                scope.launch {
                                    viewerContainerState?.reset(DEFAULT_SOFT_ANIMATION_SPEC)
                                }
                            }
                            break
                        }
                    }
                } while (event.changes.fastAny { it.pressed })
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