package com.ismartcoding.plain.services

import android.accessibilityservice.AccessibilityService

/**
 * Pure decision helpers for streamed-touch gestures (unit-locked in
 * TouchGestureRulesTest):
 *  - tap slop: movement beyond [TAP_SLOP_PX] turns a press into a drag;
 *  - edge gestures: a drag starting near a screen edge is withheld from live
 *    injection (raw injected touches never trigger SystemUI edge navigation)
 *    and classified at UP — an inward swipe fires the matching system action.
 */
internal object TouchEdgeGesture {
    internal enum class Edge { NONE, LEFT, RIGHT, TOP, BOTTOM }

    /** A down within this fraction of a screen edge counts as edge-originated. */
    internal const val ASSIST_FRACTION = 0.03f

    /** Minimum inward displacement (screen fraction) to fire the system action. */
    internal const val TRIGGER_FRACTION = 0.12f

    /** Touch slop in px: beyond this the press is a drag, not a tap/hold. */
    internal const val TAP_SLOP_PX = 28f

    fun edgeOf(x: Float, y: Float, w: Int, h: Int): Edge = when {
        w <= 0 || h <= 0 -> Edge.NONE
        x < w * ASSIST_FRACTION -> Edge.LEFT
        x > w * (1f - ASSIST_FRACTION) -> Edge.RIGHT
        y < h * ASSIST_FRACTION -> Edge.TOP
        y > h * (1f - ASSIST_FRACTION) -> Edge.BOTTOM
        else -> Edge.NONE
    }

    /** Snap a coordinate to the screen edge when inside the assist zone. */
    fun snapToEdge(v: Float, dim: Float): Float {
        val zone = dim * ASSIST_FRACTION
        return when {
            v < zone -> 0f
            v > dim - zone -> dim
            else -> v
        }
    }

    /** System global action for an edge-originated swipe; 0 = not an edge gesture. */
    fun actionFor(edge: Edge, dx: Float, dy: Float, w: Int, h: Int): Int = when (edge) {
        Edge.BOTTOM -> if (dy < -h * TRIGGER_FRACTION) AccessibilityService.GLOBAL_ACTION_HOME else 0
        Edge.TOP -> if (dy > h * TRIGGER_FRACTION) AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS else 0
        Edge.LEFT -> if (dx > w * TRIGGER_FRACTION) AccessibilityService.GLOBAL_ACTION_BACK else 0
        Edge.RIGHT -> if (dx < -w * TRIGGER_FRACTION) AccessibilityService.GLOBAL_ACTION_BACK else 0
        Edge.NONE -> 0
    }

    fun isBeyondTapSlop(dx: Float, dy: Float): Boolean =
        dx * dx + dy * dy > TAP_SLOP_PX * TAP_SLOP_PX
}
