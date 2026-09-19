package com.ismartcoding.plain.tests

import com.ismartcoding.plain.ui.page.scan.ScanAutoOpenPolicy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScanAutoOpenPolicyTest {

    @Test
    fun singleConfirmedCodeWithNoOthersOpensImmediately() {
        val policy = ScanAutoOpenPolicy()
        assertTrue(policy.allow(confirmedCount = 1, frameCodeCount = 1))
    }

    @Test
    fun unconfirmedCompanionCodeBlocksAutoOpen() {
        val policy = ScanAutoOpenPolicy()
        // two codes in the frame, only one confirmed yet: wait for the other
        assertFalse(policy.allow(confirmedCount = 1, frameCodeCount = 2))
        assertFalse(policy.allow(confirmedCount = 1, frameCodeCount = 2))
        assertFalse(policy.allow(confirmedCount = 1, frameCodeCount = 2))
    }

    @Test
    fun companionThatNeverConfirmsOpensAfterTheCap() {
        val policy = ScanAutoOpenPolicy()
        repeat(ScanAutoOpenPolicy.PENDING_FRAME_CAP) {
            assertFalse(policy.allow(confirmedCount = 1, frameCodeCount = 2))
        }
        // phantom companion that never confirms must not block forever
        assertTrue(policy.allow(confirmedCount = 1, frameCodeCount = 2))
    }

    @Test
    fun singleFrameAfterCompanionResetsTheWait() {
        val policy = ScanAutoOpenPolicy()
        policy.allow(confirmedCount = 1, frameCodeCount = 2)
        policy.allow(confirmedCount = 1, frameCodeCount = 2)
        assertTrue(policy.allow(confirmedCount = 1, frameCodeCount = 1))
        // counter was reset: a new companion blocks from scratch
        assertFalse(policy.allow(confirmedCount = 1, frameCodeCount = 2))
    }

    @Test
    fun multiConfirmedNeverAutoOpens() {
        val policy = ScanAutoOpenPolicy()
        assertFalse(policy.allow(confirmedCount = 2, frameCodeCount = 2))
        assertFalse(policy.allow(confirmedCount = 3, frameCodeCount = 3))
        // transitions from multi reset the pending counter
        assertTrue(policy.allow(confirmedCount = 1, frameCodeCount = 1))
    }

    @Test
    fun resetClearsPendingWait() {
        val policy = ScanAutoOpenPolicy()
        policy.allow(confirmedCount = 1, frameCodeCount = 2)
        policy.allow(confirmedCount = 1, frameCodeCount = 2)
        policy.reset()
        // only cap frames waited after reset, not carried over
        repeat(ScanAutoOpenPolicy.PENDING_FRAME_CAP) {
            assertFalse(policy.allow(confirmedCount = 1, frameCodeCount = 2))
        }
        assertTrue(policy.allow(confirmedCount = 1, frameCodeCount = 2))
    }

    @Test
    fun nothingConfirmedNeverOpens() {
        val policy = ScanAutoOpenPolicy()
        assertFalse(policy.allow(confirmedCount = 0, frameCodeCount = 1))
        assertFalse(policy.allow(confirmedCount = 0, frameCodeCount = 3))
    }
}
