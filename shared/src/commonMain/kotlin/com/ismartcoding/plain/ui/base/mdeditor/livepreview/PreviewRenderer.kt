package com.ismartcoding.plain.ui.base.mdeditor.livepreview

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle

/**
 * Renders markdown fully as preview text (no source lines) for non-editable
 * preview rows, reusing the transformation's line renderer. Also produces a
 * display→raw offset map so a tap on the preview can place the caret in the
 * raw text.
 */

class OffsetSegment(val outStart: Int, val outEnd: Int, val rawStart: Int, val rawEnd: Int)

class PreviewRender(val annotated: AnnotatedString, val segments: List<OffsetSegment>) {
    /** Maps an offset in the rendered preview to the raw markdown offset. */
    fun mapToRaw(offset: Int): Int {
        if (segments.isEmpty()) return offset.coerceAtLeast(0)
        val s = segments.lastOrNull { offset >= it.outStart } ?: segments.first()
        if (offset >= s.outEnd || s.outEnd == s.outStart) return s.rawEnd
        val ratio = (offset - s.outStart).toFloat() / (s.outEnd - s.outStart)
        return (s.rawStart + ((s.rawEnd - s.rawStart) * ratio).toInt()).coerceIn(s.rawStart, s.rawEnd)
    }
}

fun renderPreview(src: CharSequence, styles: LivePreviewStyles): PreviewRender {
    val transformation = MarkdownLivePreviewTransformation(styles, IntRange.EMPTY)
    val edits = transformation.planEdits(src, IntRange.EMPTY)
    val sb = StringBuilder()
    val spans = ArrayList<AnnotatedString.Range<SpanStyle>>()
    val segments = ArrayList<OffsetSegment>()
    var srcPos = 0
    for (e in edits) {
        if (e.start > srcPos) {
            segments.add(OffsetSegment(sb.length, sb.length + (e.start - srcPos), srcPos, e.start))
            sb.append(src, srcPos, e.start)
            srcPos = e.start
        }
        val outStart = sb.length
        sb.append(e.text)
        for (s in e.spans) {
            spans.add(AnnotatedString.Range(s.style, outStart + s.start, outStart + s.end))
        }
        segments.add(OffsetSegment(outStart, sb.length, srcPos, srcPos + e.origLen))
        srcPos += e.origLen
    }
    if (srcPos < src.length) {
        segments.add(OffsetSegment(sb.length, sb.length + (src.length - srcPos), srcPos, src.length))
        sb.append(src, srcPos, src.length)
    }
    return PreviewRender(AnnotatedString(sb.toString(), spans), segments)
}
