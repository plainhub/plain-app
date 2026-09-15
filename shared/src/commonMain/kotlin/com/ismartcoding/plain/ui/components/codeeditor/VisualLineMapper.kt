package com.ismartcoding.plain.ui.components.codeeditor

import com.ismartcoding.plain.lib.codeeditor.LineVectorDocument

/**
 * Maps between logical lines and visual rows. Plain lines map 1:1; extremely long lines
 * (byte length > [LineVectorDocument.CHUNK_BYTES]) are split into consecutive display
 * chunks, each occupying its own visual row so a single 10MB line cannot freeze layout.
 *
 * Thread safety: [rebuild] may run on a background dispatcher and publishes an immutable
 * snapshot; lookups on the main thread always see a consistent mapping.
 */
class VisualLineMapper {
    private class Snapshot(
        val chunkedLines: IntArray,
        val extraBefore: IntArray, // extra visual rows contributed before chunkedLines[i]
        val chunkRows: IntArray,   // visual row count of chunkedLines[i]
        val totalExtra: Int,
        val logicalCount: Int,
    )

    private var snap: Snapshot = Snapshot(IntArray(0), IntArray(0), IntArray(0), 0, 0)

    val visualCount: Int get() = snap.logicalCount + snap.totalExtra

    val ready: Boolean get() = initialized

    private var initialized = false

    fun rebuild(doc: LineVectorDocument) {
        val count = doc.lineCount
        var extra = 0
        val lines = ArrayList<Int>(16)
        val extras = ArrayList<Int>(16)
        val rows = ArrayList<Int>(16)
        for (line in 0 until count) {
            val chunks = doc.lineChunkCount(line)
            if (chunks > 1) {
                lines.add(line)
                extras.add(extra)
                rows.add(chunks)
                extra += chunks - 1
            }
        }
        snap = Snapshot(lines.toIntArray(), extras.toIntArray(), rows.toIntArray(), extra, count)
        initialized = true
    }

    fun chunked(line: Int): Boolean {
        val s = snap
        return exactIndex(s.chunkedLines, line) >= 0
    }

    fun logicalToVisual(line: Int): Int {
        val s = snap
        val i = insertIndex(s.chunkedLines, line)
        val extra = if (i < s.extraBefore.size) s.extraBefore[i] else s.totalExtra
        return line + extra
    }

    fun visualToLogical(visual: Int): Int {
        val s = snap
        var v = visual
        for (i in s.chunkedLines.indices) {
            val base = s.chunkedLines[i] + s.extraBefore[i]
            val rows = s.chunkRows[i]
            if (v < base) return v
            if (v < base + rows) return s.chunkedLines[i]
            v -= rows - 1
        }
        return v
    }

    fun rowChunk(visual: Int): Int {
        val s = snap
        var v = visual
        for (i in s.chunkedLines.indices) {
            val base = s.chunkedLines[i] + s.extraBefore[i]
            val rows = s.chunkRows[i]
            if (v < base) return 0
            if (v < base + rows) return v - base
            v -= rows - 1
        }
        return 0
    }

    fun rowLine(visual: Int): Int = visualToLogical(visual)

    private fun exactIndex(arr: IntArray, value: Int): Int {
        var lo = 0
        var hi = arr.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            when {
                arr[mid] == value -> return mid
                arr[mid] < value -> lo = mid + 1
                else -> hi = mid - 1
            }
        }
        return -1
    }

    private fun insertIndex(arr: IntArray, value: Int): Int {
        var lo = 0
        var hi = arr.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (arr[mid] < value) lo = mid + 1 else hi = mid
        }
        return lo
    }
}
