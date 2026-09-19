package com.ismartcoding.plain.tests

import com.ismartcoding.plain.platform.ScannedCode
import com.ismartcoding.plain.ui.page.scan.ScanCodeTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScanCodeTrackerTest {

    private fun code(text: String, x: Float = 0.5f, y: Float = 0.5f) = ScannedCode(text, x, y)

    @Test
    fun singleFrameIsNotConfirmed() {
        val tracker = ScanCodeTracker()
        assertEquals(emptyList(), tracker.accept(listOf(code("a"))))
    }

    @Test
    fun sameCodeTwoFramesConfirmsOnce() {
        val tracker = ScanCodeTracker()
        tracker.accept(listOf(code("a")))
        val confirmed = tracker.accept(listOf(code("a")))
        assertEquals(listOf("a"), confirmed.map { it.text })
    }

    @Test
    fun misdecodedFrameNeverConfirms() {
        val tracker = ScanCodeTracker()
        tracker.accept(listOf(code("garbage-1")))
        tracker.accept(listOf(code("garbage-2")))
        tracker.accept(listOf(code("garbage-3")))
        assertEquals(emptyList(), tracker.accept(listOf(code("garbage-4"))))
    }

    @Test
    fun oneDroppedFrameIsToleratedBeforeForget() {
        val tracker = ScanCodeTracker()
        tracker.accept(listOf(code("a")))
        assertEquals(emptyList(), tracker.accept(emptyList())) // dropped frame: still tracked, not confirmed
        assertEquals(listOf("a"), tracker.accept(listOf(code("a"))).map { it.text })
    }

    @Test
    fun twoDroppedFramesForgetTheCode() {
        val tracker = ScanCodeTracker()
        tracker.accept(listOf(code("a")))
        tracker.accept(listOf(code("a")))
        tracker.accept(emptyList())
        tracker.accept(emptyList())
        // forgotten: needs a fresh two-frame confirmation after reappearing
        assertEquals(emptyList(), tracker.accept(listOf(code("a"))))
        assertEquals(listOf("a"), tracker.accept(listOf(code("a"))).map { it.text })
    }

    @Test
    fun confirmedPositionTracksLatestFrame() {
        val tracker = ScanCodeTracker()
        tracker.accept(listOf(code("a", 0.1f, 0.2f)))
        val confirmed = tracker.accept(listOf(code("a", 0.8f, 0.9f)))
        assertEquals(1, confirmed.size)
        assertEquals(0.8f, confirmed[0].centerX)
        assertEquals(0.9f, confirmed[0].centerY)
    }

    @Test
    fun orderIsStableByFirstDetection() {
        val tracker = ScanCodeTracker()
        tracker.accept(listOf(code("a")))
        tracker.accept(listOf(code("a"), code("b")))
        val confirmed = tracker.accept(listOf(code("b"), code("a")))
        assertEquals(listOf("a", "b"), confirmed.map { it.text })
    }

    @Test
    fun missingCodeDecaysWhileOthersConfirm() {
        val tracker = ScanCodeTracker()
        tracker.accept(listOf(code("a"), code("b")))
        tracker.accept(listOf(code("a"), code("b")))
        // b disappears: a stays confirmed, b stays absent until forgotten
        val onlyA = tracker.accept(listOf(code("a")))
        assertEquals(listOf("a"), onlyA.map { it.text })
        val stillTracked = tracker.accept(listOf(code("a"), code("b")))
        assertTrue(stillTracked.map { it.text }.containsAll(listOf("a")))
    }
}
