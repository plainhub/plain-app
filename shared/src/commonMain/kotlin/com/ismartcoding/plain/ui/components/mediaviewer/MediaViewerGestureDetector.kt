package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.util.fastAny
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs

suspend fun PointerInputScope.detectTransformGestures(
    panZoomLock: Boolean = false,
    gestureStart: () -> Unit = {},
    gestureEnd: (Boolean) -> Unit = {},
    onTap: (Offset) -> Unit = {},
    onDoubleTap: (Offset) -> Unit = {},
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float, event: PointerEvent) -> Boolean,
) {
    val tapClassifier = GestureTapClassifier()
    val tapScope = MainScope()
    var pendingTap: Job? = null
    try {
        awaitEachGesture {
            var rotation = 0f
            var zoom = 1f
            var pan = Offset.Zero
            var pastTouchSlop = false
            val touchSlop = viewConfiguration.touchSlop
            var lockedToPanZoom = false

            val down = awaitFirstDown(requireUnconsumed = false)
            var releasedEvent: PointerEvent? = null
            var maxPointerCount = 1
            var canceled = false

            gestureStart()
            do {
                val event = awaitPointerEvent()
                if (event.changes.size > maxPointerCount) maxPointerCount = event.changes.size
                if (event.type == PointerEventType.Release) releasedEvent = event
                canceled = event.changes.fastAny { it.isConsumed }
                if (!canceled) {
                    val zoomChange = event.calculateZoom()
                    val rotationChange = event.calculateRotation()
                    val panChange = event.calculatePan()

                    if (!pastTouchSlop) {
                        zoom *= zoomChange
                        rotation += rotationChange
                        pan += panChange
                        val centroidSize = event.calculateCentroidSize(useCurrent = false)
                        val zoomMotion = abs(1 - zoom) * centroidSize
                        val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                        val panMotion = pan.getDistance()
                        if (zoomMotion > touchSlop || rotationMotion > touchSlop || panMotion > touchSlop) {
                            pastTouchSlop = true
                            lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                        }
                    }
                    if (pastTouchSlop) {
                        val centroid = event.calculateCentroid(useCurrent = false)
                        val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                        if (effectiveRotation != 0f || zoomChange != 1f || panChange != Offset.Zero) {
                            if (!onGesture(centroid, panChange, zoomChange, effectiveRotation, event)) break
                        }
                    }
                }
            } while (!canceled && event.changes.fastAny { it.pressed })

            val release = releasedEvent
            if (release != null && release.changes.isNotEmpty()) {
                val up = release.changes.first()
                when (tapClassifier.classifyRelease(
                    upUptimeMillis = up.uptimeMillis,
                    pressDurationMillis = up.uptimeMillis - down.uptimeMillis,
                    longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis,
                    maxPointerCount = maxPointerCount,
                    passedTouchSlop = pastTouchSlop,
                    canceled = canceled,
                )) {
                    GestureTapClassifier.Decision.TAP ->
                        pendingTap = tapScope.launch { delay(DOUBLE_TAP_WINDOW_MILLIS); onTap(up.position) }
                    GestureTapClassifier.Decision.DOUBLE_TAP -> {
                        pendingTap?.cancel()
                        pendingTap = null
                        onDoubleTap(up.position)
                    }
                    GestureTapClassifier.Decision.IGNORED -> Unit
                }
            }
            gestureEnd(pastTouchSlop)
        }
    } finally {
        tapScope.cancel()
    }
}
