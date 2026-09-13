package com.ismartcoding.plain.lib

/** One raw recognition segment with song-timeline timestamps in milliseconds. */
data class RawSegment(val timeMs: Long, val endMs: Long, val text: String)

/**
 * Helpers for chunked transcription: long audio is decoded and recognized in
 * fixed windows with a small tail overlap, then the per-chunk results are
 * shifted back onto the song timeline and de-duplicated.
 */
object TranscribeMerge {
    fun shift(segments: List<RawSegment>, chunkStartMs: Long): List<RawSegment> {
        return segments.map { RawSegment(it.timeMs + chunkStartMs, it.endMs + chunkStartMs, it.text) }
    }

    /** Drops segments that begin inside the previous chunk's overlap window. */
    fun dropOverlap(segments: List<RawSegment>, overlapStartMs: Long): List<RawSegment> {
        if (overlapStartMs <= 0) return segments
        return segments.filter { it.timeMs >= overlapStartMs }
    }

    fun toLrcLines(segments: List<RawSegment>): List<LrcParser.LrcLine> {
        return segments
            .filter { it.text.isNotBlank() }
            .map { LrcParser.LrcLine(it.timeMs, it.text.trim()) }
    }
}
