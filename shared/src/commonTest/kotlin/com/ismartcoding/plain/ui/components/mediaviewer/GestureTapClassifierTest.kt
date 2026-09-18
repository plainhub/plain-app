package com.ismartcoding.plain.ui.components.mediaviewer

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks the tap/double-tap classification used by detectTransformGestures.
 * All times are injected monotonic uptimes — no wall clock involved.
 *
 * Regression origin: taps on devices whose digitizers report sub-pixel Move
 * jitter (e-ink / Android 12) never fired onTap because the old detector
 * required literally zero Move events and a press under 200ms wall clock.
 * Qualification now follows platform detectTapGestures semantics: single
 * pointer, movement never crossing touch slop, not canceled, press shorter
 * than the long-press timeout.
 */
class GestureTapClassifierTest {
    private val longPressTimeout = 400L

    private fun newClassifier() = GestureTapClassifier(doubleTapWindowMillis = 272)

    private fun GestureTapClassifier.release(
        upUptimeMillis: Long,
        pressDurationMillis: Long = 100,
        maxPointerCount: Int = 1,
        passedTouchSlop: Boolean = false,
        canceled: Boolean = false,
    ) = classifyRelease(
        upUptimeMillis = upUptimeMillis,
        pressDurationMillis = pressDurationMillis,
        longPressTimeoutMillis = longPressTimeout,
        maxPointerCount = maxPointerCount,
        passedTouchSlop = passedTouchSlop,
        canceled = canceled,
    )

    @Test
    fun firstQualifyingReleaseIsATap() {
        assertEquals(GestureTapClassifier.Decision.TAP, newClassifier().release(upUptimeMillis = 10_000))
    }

    @Test
    fun secondReleaseInsideWindowIsADoubleTap() {
        val c = newClassifier()
        assertEquals(GestureTapClassifier.Decision.TAP, c.release(upUptimeMillis = 10_000))
        assertEquals(GestureTapClassifier.Decision.DOUBLE_TAP, c.release(upUptimeMillis = 10_271))
    }

    @Test
    fun doubleTapResetsWindowSoThirdQuickReleaseIsATapAgain() {
        val c = newClassifier()
        c.release(upUptimeMillis = 10_000)
        assertEquals(GestureTapClassifier.Decision.DOUBLE_TAP, c.release(upUptimeMillis = 10_200))
        assertEquals(GestureTapClassifier.Decision.TAP, c.release(upUptimeMillis = 10_300))
    }

    @Test
    fun releaseAtWindowBoundaryStartsANewTap() {
        val c = newClassifier()
        c.release(upUptimeMillis = 10_000)
        assertEquals(GestureTapClassifier.Decision.TAP, c.release(upUptimeMillis = 10_272))
    }

    @Test
    fun jitterBelowTouchSlopStillTaps() {
        // The Android 12 / e-ink regression: Move events exist but never cross
        // touch slop — still a tap.
        assertEquals(GestureTapClassifier.Decision.TAP, newClassifier().release(upUptimeMillis = 10_000, passedTouchSlop = false))
    }

    @Test
    fun releaseAfterCrossingTouchSlopIsIgnoredAndKeepsWindowState() {
        val c = newClassifier()
        c.release(upUptimeMillis = 10_000)
        assertEquals(GestureTapClassifier.Decision.IGNORED, c.release(upUptimeMillis = 10_100, passedTouchSlop = true))
        // The ignored drag does not consume the double-tap window of the tap
        // released before it.
        assertEquals(GestureTapClassifier.Decision.DOUBLE_TAP, c.release(upUptimeMillis = 10_250))
    }

    @Test
    fun multiPointerReleaseIsIgnored() {
        assertEquals(GestureTapClassifier.Decision.IGNORED, newClassifier().release(upUptimeMillis = 10_000, maxPointerCount = 2))
    }

    @Test
    fun canceledReleaseIsIgnored() {
        assertEquals(GestureTapClassifier.Decision.IGNORED, newClassifier().release(upUptimeMillis = 10_000, canceled = true))
    }

    @Test
    fun slowPressBelowLongPressTimeoutStillTaps() {
        // The old detector dropped presses >= 200ms; a deliberate 250ms press
        // is a tap (long press only starts at the 400ms timeout).
        assertEquals(GestureTapClassifier.Decision.TAP, newClassifier().release(upUptimeMillis = 10_000, pressDurationMillis = 250))
        assertEquals(GestureTapClassifier.Decision.TAP, newClassifier().release(upUptimeMillis = 10_000, pressDurationMillis = 399))
    }

    @Test
    fun pressAtOrBeyondLongPressTimeoutIsIgnored() {
        assertEquals(GestureTapClassifier.Decision.IGNORED, newClassifier().release(upUptimeMillis = 10_000, pressDurationMillis = 400))
        assertEquals(GestureTapClassifier.Decision.IGNORED, newClassifier().release(upUptimeMillis = 10_000, pressDurationMillis = 600))
    }
}
