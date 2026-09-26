package com.ismartcoding.plain.services

import android.accessibilityservice.AccessibilityService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Locks the pure streamed-touch gesture decisions (plain-cast port):
 * edge-zone detection, edge snap, edge-swipe → system action classification,
 * tap-slop boundary and the exact threshold constants. A regression here
 * silently breaks swipe-up-home / edge-back / notification pull-down or
 * turns taps into drags — all invisible without a device.
 */
class TouchEdgeGestureTest {
    private val w = 1000
    private val h = 2000

    @Test
    fun constantsAreLocked() {
        assertEquals(0.03f, TouchEdgeGesture.ASSIST_FRACTION)
        assertEquals(0.12f, TouchEdgeGesture.TRIGGER_FRACTION)
        assertEquals(28f, TouchEdgeGesture.TAP_SLOP_PX)
    }

    @Test
    fun edgesDetectedInsideAssistZone() {
        assertEquals(TouchEdgeGesture.Edge.LEFT, TouchEdgeGesture.edgeOf(0f, 1000f, w, h))
        assertEquals(TouchEdgeGesture.Edge.LEFT, TouchEdgeGesture.edgeOf(29f, 1000f, w, h))
        assertEquals(TouchEdgeGesture.Edge.RIGHT, TouchEdgeGesture.edgeOf(971f, 1000f, w, h))
        assertEquals(TouchEdgeGesture.Edge.TOP, TouchEdgeGesture.edgeOf(500f, 59f, w, h))
        assertEquals(TouchEdgeGesture.Edge.BOTTOM, TouchEdgeGesture.edgeOf(500f, 1941f, w, h))
    }

    @Test
    fun centerIsNotAnEdge() {
        assertEquals(TouchEdgeGesture.Edge.NONE, TouchEdgeGesture.edgeOf(500f, 1000f, w, h))
        // exactly at the 3% boundary (30px) is outside the zone: strictly-less rule
        assertEquals(TouchEdgeGesture.Edge.NONE, TouchEdgeGesture.edgeOf(30f, 1000f, w, h))
    }

    @Test
    fun degenerateScreenSizeHasNoEdge() {
        assertEquals(TouchEdgeGesture.Edge.NONE, TouchEdgeGesture.edgeOf(0f, 0f, 0, 0))
    }

    @Test
    fun snapToEdgeClampsInsideZone() {
        assertEquals(0f, TouchEdgeGesture.snapToEdge(29f, w.toFloat()))
        assertEquals(w.toFloat(), TouchEdgeGesture.snapToEdge(971f, w.toFloat()))
        // outside the zone: untouched
        assertEquals(500f, TouchEdgeGesture.snapToEdge(500f, w.toFloat()))
        assertEquals(30f, TouchEdgeGesture.snapToEdge(30f, w.toFloat()))
    }

    @Test
    fun bottomUpSwipesToHome() {
        val dy = -h * TouchEdgeGesture.TRIGGER_FRACTION - 1
        assertEquals(
            AccessibilityService.GLOBAL_ACTION_HOME,
            TouchEdgeGesture.actionFor(TouchEdgeGesture.Edge.BOTTOM, 0f, dy, w, h),
        )
    }

    @Test
    fun topDownSwipesToNotifications() {
        val dy = h * TouchEdgeGesture.TRIGGER_FRACTION + 1
        assertEquals(
            AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS,
            TouchEdgeGesture.actionFor(TouchEdgeGesture.Edge.TOP, 0f, dy, w, h),
        )
    }

    @Test
    fun leftAndRightInwardSwipesGoBack() {
        val dx = w * TouchEdgeGesture.TRIGGER_FRACTION + 1
        assertEquals(
            AccessibilityService.GLOBAL_ACTION_BACK,
            TouchEdgeGesture.actionFor(TouchEdgeGesture.Edge.LEFT, dx, 0f, w, h),
        )
        assertEquals(
            AccessibilityService.GLOBAL_ACTION_BACK,
            TouchEdgeGesture.actionFor(TouchEdgeGesture.Edge.RIGHT, -dx, 0f, w, h),
        )
    }

    @Test
    fun belowTriggerOrOutwardSwipeFiresNothing() {
        val dy = -h * TouchEdgeGesture.TRIGGER_FRACTION + 1
        assertEquals(0, TouchEdgeGesture.actionFor(TouchEdgeGesture.Edge.BOTTOM, 0f, dy, w, h))
        // outward on a bottom edge (downward) is not a home swipe
        assertEquals(0, TouchEdgeGesture.actionFor(TouchEdgeGesture.Edge.BOTTOM, 0f, 500f, w, h))
        assertEquals(0, TouchEdgeGesture.actionFor(TouchEdgeGesture.Edge.NONE, 500f, -500f, w, h))
    }

    @Test
    fun tapSlopBoundary() {
        assertFalse(TouchEdgeGesture.isBeyondTapSlop(0f, 0f))
        // exactly at slop radius (28,0) → squared 784 is not strictly beyond
        assertFalse(TouchEdgeGesture.isBeyondTapSlop(28f, 0f))
        assertTrue(TouchEdgeGesture.isBeyondTapSlop(29f, 0f))
        assertTrue(TouchEdgeGesture.isBeyondTapSlop(20f, 20f))
    }
}
