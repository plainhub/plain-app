package com.ismartcoding.plain.ui.base.pinchzoomgrid

import android.content.Context
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import com.ismartcoding.lib.helpers.CoroutinesHelper.coIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

suspend fun PointerInputScope.handlePinchGesture(
    context: Context,
    state: PinchZoomGridState,
    scope: CoroutineScope,
    onTap: (Offset) -> Unit = {},
    onLongPress: (Offset) -> Unit = {}
) {
    val velocityTracker = VelocityTracker()
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        var zoom = 1f
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop / 4

        val down = awaitFirstDown(requireUnconsumed = false)
        val downTime = System.currentTimeMillis()
        var releasedEvent: PointerEvent? = null
        var moveCount = 0

        velocityTracker.resetTracking()
        velocityTracker.addPointerInputChange(down)
        var trackId = down.id

        var pinchStarted = false
        val longPressJob = coIO {
            delay(longPressTimeout)
            if (!pastTouchSlop) {
                scope.launch {
                    onLongPress(down.position)
                }
            }
        }

        do {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Release) {
                releasedEvent = event
            }
            if (event.type == PointerEventType.Move) {
                moveCount++
            }

            val trackChange = event.changes.fastFirstOrNull { it.id == trackId }
                ?: event.changes.firstOrNull()?.also { trackId = it.id }
            if (trackChange != null) {
                velocityTracker.addPointerInputChange(trackChange)
            }

            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize

                    if (zoomMotion > touchSlop) {
                        pastTouchSlop = true
                        longPressJob.cancel()
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

        if (moveCount == 0) {
            releasedEvent?.let { e ->
                if (e.changes.isEmpty()) {
                    return@let
                }
                val dt = System.currentTimeMillis() - downTime
                if (dt < 300) {
                    scope.launch {
                        onTap(e.changes.first().position)
                    }
                }
            }
        }

        longPressJob.cancel()

        if (pinchStarted) {
            state.onZoomStopped(velocityTracker.calculateVelocity())
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
