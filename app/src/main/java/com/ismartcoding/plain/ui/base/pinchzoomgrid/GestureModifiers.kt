package com.ismartcoding.plain.ui.base.pinchzoomgrid

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs

internal fun Modifier.handlePinchGesture(state: PinchZoomGridState): Modifier {
    return this.pointerInput(state) {
        // Based on detectTransformGestures()
        val velocityTracker = VelocityTracker()
        awaitEachGesture {
            var zoom = 1f
            var pastTouchSlop = false
            val touchSlop = viewConfiguration.touchSlop / 4

            val down = awaitFirstDown(requireUnconsumed = false)

            velocityTracker.resetTracking()
            velocityTracker.addPointerInputChange(down)
            var trackId = down.id

            var pinchStarted = false

            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.fastAny { it.isConsumed }

                val trackChange = event.changes.fastFirstOrNull { it.id == trackId }
                    ?: event.changes.firstOrNull()?.also { trackId = it.id }
                if (trackChange != null) {
                    velocityTracker.addPointerInputChange(trackChange)
                }

                if (!canceled) {
                    val zoomChange = event.calculateZoom()

                    if (!pastTouchSlop) {
                        zoom *= zoomChange
                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                        val zoomMotion = abs(1 - zoom) * centroidSize

                        if (zoomMotion > touchSlop) {
                            pastTouchSlop = true
                        }
                    }

                    if (pastTouchSlop) {
                        if (zoomChange != 1f) {
                            if (!pinchStarted) {
                                val centroid = event.calculateCentroid(useCurrent = false)
                                state.onZoomStart(centroid, zoom)
                                pinchStarted = true
                            }
                            state.onZoom(zoomChange)
                        }
                        event.changes.fastForEach {
                            if (it.positionChanged()) {
                                it.consume()
                            }
                        }
                    }
                }
            } while (!canceled && event.changes.fastAny { it.pressed })
            if (pinchStarted) {
                state.onZoomStopped(velocityTracker.calculateVelocity())
            }
        }
    }
}

internal fun Modifier.handleOverZooming(state: PinchZoomGridState): Modifier {
    return this.graphicsLayer {
        val isOverZooming = state.isZooming && state.nextCells == null
        if (!isOverZooming) return@graphicsLayer
        val scale = when (state.zoomType) {
            ZoomType.ZoomIn -> 1f + state.progress * 0.1f
            ZoomType.ZoomOut -> 1f - state.progress * 0.1f
        }
        scaleX = scale
        scaleY = scale
    }
}
