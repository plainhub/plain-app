package com.ismartcoding.plain.ui.page.scan

import com.ismartcoding.plain.platform.ScannedCode

/**
 * Confirms codes across consecutive camera frames so a single misdecoded frame never
 * surfaces to the user, keeps tag order stable by first detection and tolerates one
 * dropped frame before a code is forgotten.
 */
class ScanCodeTracker(
    private val requiredFrames: Int = CONFIRM_FRAMES,
    private val missedFramesTolerance: Int = MISSED_FRAMES_TOLERANCE,
) {
    private class Entry(
        var seen: Int,
        var missed: Int,
        val firstIndex: Int,
        var latest: ScannedCode,
    )

    private val entries = LinkedHashMap<String, Entry>()
    private var nextIndex = 0

    /** Feed one frame's detections; returns the codes currently confirmed and visible. */
    fun accept(frameCodes: List<ScannedCode>): List<ScannedCode> {
        if (frameCodes.isEmpty()) {
            decayMissing(emptySet())
            return confirmed()
        }
        val present = HashSet<String>(frameCodes.size * 2)
        for (code in frameCodes) {
            present.add(code.text)
            val entry = entries[code.text]
            if (entry == null) {
                entries[code.text] = Entry(1, 0, nextIndex++, code)
            } else {
                entry.seen++
                entry.missed = 0
                entry.latest = code
            }
        }
        decayMissing(present)
        return confirmed()
    }

    fun reset() {
        entries.clear()
    }

    private fun decayMissing(present: Set<String>) {
        val iterator = entries.entries.iterator()
        while (iterator.hasNext()) {
            val (text, entry) = iterator.next()
            if (text in present) continue
            entry.missed++
            if (entry.missed > missedFramesTolerance) iterator.remove()
        }
    }

    private fun confirmed(): List<ScannedCode> {
        if (entries.isEmpty()) return emptyList()
        return entries.values
            .filter { it.missed == 0 && it.seen >= requiredFrames }
            .sortedBy { it.firstIndex }
            .map { it.latest }
    }

    companion object {
        const val CONFIRM_FRAMES = 2
        const val MISSED_FRAMES_TOLERANCE = 1
    }
}
