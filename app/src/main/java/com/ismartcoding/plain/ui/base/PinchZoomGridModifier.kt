package com.ismartcoding.plain.ui.base

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Adds a two-finger pinch gesture to a LazyVerticalGrid that adjusts
 * [cellsPerRow] by ±1 when the accumulated zoom crosses a threshold.
 * Single-finger events are never consumed, so scrolling and drag-select
 * continue to work normally.
 */
fun Modifier.pinchZoomGrid(
    cellsPerRow: MutableState<Int>,
    hapticFeedback: HapticFeedback,
    scope: CoroutineScope,
    onCellsChanged: suspend (Int) -> Unit,
): Modifier = this.pointerInput("pinchZoomGrid") {
    // zoomAccumulator lives inside the gesture loop so it resets per touch session
    var zoomAccumulator = 1f
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var prevDistance = -1f
        var pinchActive = false
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pointers = event.changes.filter { it.pressed }
            if (pointers.size >= 2) {
                pinchActive = true
                val p0 = pointers[0].position
                val p1 = pointers[1].position
                val dx = p0.x - p1.x
                val dy = p0.y - p1.y
                val dist = sqrt(dx * dx + dy * dy)
                if (prevDistance > 0f) {
                    zoomAccumulator *= dist / prevDistance
                    when {
                        zoomAccumulator > 1.35f -> {
                            val newCells = (cellsPerRow.value - 1).coerceAtLeast(2)
                            if (newCells != cellsPerRow.value) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                cellsPerRow.value = newCells
                                scope.launch { onCellsChanged(newCells) }
                            }
                            zoomAccumulator = 1f
                        }
                        zoomAccumulator < 0.65f -> {
                            val newCells = (cellsPerRow.value + 1).coerceAtMost(10)
                            if (newCells != cellsPerRow.value) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                cellsPerRow.value = newCells
                                scope.launch { onCellsChanged(newCells) }
                            }
                            zoomAccumulator = 1f
                        }
                    }
                }
                prevDistance = dist
                // Consume two-finger events to prevent HorizontalPager from
                // interpreting the pinch as a page swipe.
                pointers.forEach { it.consume() }
            } else {
                if (pinchActive) zoomAccumulator = 1f
                pinchActive = false
                prevDistance = -1f
            }
        } while (event.changes.any { it.pressed })
    }
}

/**
 * A fling behavior that multiplies the initial velocity by [velocityMultiplier]
 * before handing off to the spline-based decay, giving a smoother / faster
 * iOS-like fling when the grid has many small cells.
 */
@Composable
fun rememberBoostFlingBehavior(velocityMultiplier: Float): FlingBehavior {
    val decaySpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay()
    return remember(decaySpec, velocityMultiplier) {
        object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                if (abs(initialVelocity) <= 1f) return initialVelocity
                var lastValue = 0f
                var remainingVelocity = initialVelocity
                AnimationState(
                    initialValue = 0f,
                    initialVelocity = initialVelocity * velocityMultiplier,
                ).animateDecay(decaySpec) {
                    val delta = value - lastValue
                    val consumed = scrollBy(delta)
                    lastValue = value
                    remainingVelocity = velocity
                    if (abs(delta - consumed) > 0.5f) cancelAnimation()
                }
                return remainingVelocity
            }
        }
    }
}
