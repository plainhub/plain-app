package com.ismartcoding.plain.ui.page.scan

/**
 * Decides when a confirmed single code may auto-open its result. If the frame also
 * contains other codes that are not confirmed yet, opening waits a bounded number of
 * frames so a true multi-code scene freezes with tags instead of popping a result
 * sheet for whichever code happened to confirm first.
 */
class ScanAutoOpenPolicy(private val pendingFrameCap: Int = PENDING_FRAME_CAP) {
    private var pendingFrames = 0

    /** True if the single confirmed code may auto-open its result now. */
    fun allow(confirmedCount: Int, frameCodeCount: Int): Boolean {
        if (confirmedCount != 1) {
            pendingFrames = 0
            return false
        }
        if (frameCodeCount > 1) {
            pendingFrames++
            return pendingFrames > pendingFrameCap
        }
        pendingFrames = 0
        return true
    }

    fun reset() {
        pendingFrames = 0
    }

    companion object {
        const val PENDING_FRAME_CAP = 3
    }
}
