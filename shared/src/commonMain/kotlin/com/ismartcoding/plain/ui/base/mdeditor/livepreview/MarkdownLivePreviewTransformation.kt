package com.ismartcoding.plain.ui.base.mdeditor.livepreview

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Obsidian-style live preview. The raw markdown text stays the single source of truth;
 * this [OutputTransformation] rewrites the display buffer to hide syntax markers and style
 * content on every line except the lines touched by the selection ("source lines"), which
 * stay verbatim so the caret and IME composition always operate on raw text.
 * Selection/caret mapping between original and transformed text is managed by the framework.
 */

fun computeSourceLines(text: CharSequence, selectionStart: Int, selectionEnd: Int): IntRange {
    val a = lineOf(text, minOf(selectionStart, selectionEnd))
    val b = lineOf(text, maxOf(selectionStart, selectionEnd))
    return a..b
}

private fun lineOf(text: CharSequence, offset: Int): Int {
    var line = 0
    val end = offset.coerceIn(0, text.length)
    for (i in 0 until end) {
        if (text[i] == '\n') line++
    }
    return line
}

internal class SpanSpec(val style: SpanStyle, val start: Int, val end: Int)

internal class LineEdit(val start: Int, val origLen: Int, val text: String, val spans: List<SpanSpec>)

internal class LineRender(
    val text: String,
    val spans: List<SpanSpec>,
    val togglesFence: Boolean,
    val togglesMath: Boolean,
    val hiddenLine: Boolean,
    val transformed: Boolean,
)

class MarkdownLivePreviewTransformation(
    private val styles: LivePreviewStyles,
    // when set, overrides the selection-derived source lines; used by block editors
    // that render an unfocused block fully (IntRange.EMPTY) without a live selection
    private val forcedSourceLines: IntRange? = null,
) : OutputTransformation {

    private data class LineKey(val content: String, val flags: Int)

    private class InlineRule(
        val regex: Regex,
        val contentGroup: Int = 1,
        val style: (MatchResult, LivePreviewStyles) -> SpanStyle?,
    )

    private val cache = HashMap<LineKey, LineRender>()

    override fun TextFieldBuffer.transformOutput() {
        val src = originalText
        if (src.isEmpty()) return
        val sel = selection
        val sourceLines = forcedSourceLines ?: computeSourceLines(src, sel.min, sel.max)
        for (e in planEdits(src, sourceLines)) {
            replace(e.start, e.start + e.origLen, e.text)
            for (s in e.spans) {
                addStyle(s.style, e.start + s.start, e.start + s.end)
            }
        }
    }

    internal fun planEdits(src: CharSequence, sourceLines: IntRange): List<LineEdit> {
        val edits = ArrayList<LineEdit>()
        var lineStart = 0
        var lineIdx = 0
        var shift = 0
        var inFence = false
        var inMath = false
        while (lineStart < src.length) {
            val nl = src.indexOf('\n', lineStart)
            val lineEnd = if (nl == -1) src.length else nl
            val content = src.substring(lineStart, lineEnd)
            val key = LineKey(
                content,
                (if (lineIdx in sourceLines) 1 else 0) or (if (inFence) 2 else 0) or (if (inMath) 4 else 0),
            )
            val entry = cache.getOrPutWithCap(key) { render(content, lineIdx in sourceLines, inFence, inMath) }
            inFence = if (entry.togglesFence) !inFence else inFence
            inMath = if (entry.togglesMath) !inMath else inMath
            if (entry.hiddenLine) {
                val len = content.length + if (nl != -1) 1 else 0
                edits.add(LineEdit(lineStart + shift, len, "", emptyList()))
                shift -= len
            } else if (entry.transformed) {
                edits.add(LineEdit(lineStart + shift, content.length, entry.text, entry.spans))
                shift += entry.text.length - content.length
            }
            lineStart = if (nl == -1) src.length else nl + 1
            lineIdx++
        }
        return edits
    }

    // The text as displayed after transformation, for callers like the line-number gutter
    // that need a string matching the onTextLayout result.
    fun displayText(src: CharSequence, sourceLines: IntRange): String {
        val edits = planEdits(src, sourceLines)
        if (edits.isEmpty()) return src.toString()
        return StringBuilder(src).also { b ->
            for (e in edits) b.replaceRange(e.start, e.start + e.origLen, e.text)
        }.toString()
    }

    private fun render(content: String, isSource: Boolean, inFence: Boolean, inMath: Boolean): LineRender {
        val lead = content.trimStart(' ', '\t')
        if (lead.startsWith("```") || lead.startsWith("~~~")) {
            return if (isSource) LineRender(content, emptyList(), togglesFence = true, togglesMath = false, hiddenLine = false, transformed = false)
            else LineRender("", emptyList(), togglesFence = true, togglesMath = false, hiddenLine = true, transformed = true)
        }
        if (lead == "$$") {
            return if (isSource) LineRender(content, emptyList(), togglesFence = false, togglesMath = true, hiddenLine = false, transformed = false)
            else LineRender("", emptyList(), togglesFence = false, togglesMath = true, hiddenLine = true, transformed = true)
        }
        if (inFence) {
            if (isSource) return LineRender(content, emptyList(), false, false, false, false)
            return LineRender(content, listOf(SpanSpec(codeStyle(), 0, content.length)), false, false, false, true)
        }
        if (inMath) {
            if (isSource) return LineRender(content, emptyList(), false, false, false, false)
            return LineRender(content, listOf(SpanSpec(mathStyle(), 0, content.length)), false, false, false, true)
        }
        if (isSource) return LineRender(content, emptyList(), false, false, false, false)

        val sb = StringBuilder(content.length)
        val spans = ArrayList<SpanSpec>()

        fun emit(s: String, style: SpanStyle? = null) {
            if (s.isEmpty()) return
            val start = sb.length
            sb.append(s)
            if (style != null) spans.add(SpanSpec(style, start, sb.length))
        }

        fun emitInline(s: String, base: SpanStyle? = null) {
            var i = 0
            while (i < s.length) {
                var bestM: MatchResult? = null
                var bestR: InlineRule? = null
                for (r in INLINE_RULES) {
                    val m = r.regex.find(s, i) ?: continue
                    if (bestM == null || m.range.first < bestM.range.first) {
                        bestM = m
                        bestR = r
                    }
                }
                val m = bestM ?: break
                val rule = bestR!!
                val g = m.groups[rule.contentGroup]!!
                emit(s.substring(i, m.range.first), base)
                emit(g.value, rule.style(m, styles) ?: base)
                i = m.range.last + 1
            }
            emit(s.substring(i), base)
        }

        val markerStyle = SpanStyle(color = styles.markerColor)
        val heading = HEADING.find(content)
        val quote = QUOTE.find(content)
        val task = TASK.find(content)
        val bullet = BULLET.find(content)
        val ordered = ORDERED.find(content)
        val blockMath = BLOCK_MATH.find(content)
        when {
            heading != null -> emit(heading.groupValues[2], headingStyle(heading.groupValues[1].length))

            blockMath != null -> emit(blockMath.groupValues[1], mathStyle())

            quote != null -> {
                emit("▎", markerStyle)
                emit(quote.groupValues[1])
                emitInline(quote.groupValues[2], SpanStyle(color = styles.quoteColor))
            }

            task != null -> {
                val box = if (task.groupValues[2].equals("x", ignoreCase = true)) "☑" else "☐"
                emit("$box ", markerStyle)
                emitInline(task.groupValues[3])
            }

            bullet != null -> {
                emit("•", markerStyle)
                emit(bullet.groupValues[2])
                emitInline(bullet.groupValues[3])
            }

            ordered != null -> {
                emit(ordered.groupValues[1] + ordered.groupValues[2], markerStyle)
                emit(ordered.groupValues[3])
                emitInline(ordered.groupValues[4])
            }

            HR.matches(content) -> emit(HR_GLYPH, markerStyle)

            else -> emitInline(content)
        }

        val rendered = sb.toString()
        return LineRender(rendered, spans, false, false, false, rendered != content || spans.isNotEmpty())
    }

    private fun mathStyle() = SpanStyle(fontStyle = FontStyle.Italic, color = styles.mathColor)

    private fun codeStyle() = SpanStyle(
        fontFamily = styles.monospace,
        background = styles.codeBackground,
        color = styles.codeTextColor,
    )

    private fun headingStyle(level: Int): SpanStyle {
        val size = when (level) {
            1 -> 28f
            2 -> 24f
            3 -> 20f
            4 -> 18f
            5 -> 17f
            else -> 16f
        }
        return SpanStyle(fontSize = size.sp, fontWeight = FontWeight.Bold, color = styles.headingColor)
    }

    private fun <K, V> MutableMap<K, V>.getOrPutWithCap(key: K, defaultValue: () -> V): V {
        get(key)?.let { return it }
        if (size > 4096) clear()
        val value = defaultValue()
        put(key, value)
        return value
    }

    companion object {
        private val HEADING = Regex("^(#{1,6})[ \\t]+(.*)$")
        private val QUOTE = Regex("^>([ \\t]?)(.*)$")
        private val TASK = Regex("^([-*+])[ \\t]+\\[([ xX])][ \\t]*(.*)$")
        private val BULLET = Regex("^([-*+])([ \\t]+)(.*)$")
        private val ORDERED = Regex("^(\\d{1,9})([.)])([ \\t]+)(.*)$")
        private val HR = Regex("^ {0,3}([-*_])[ \\t]*(?:\\1[ \\t]*){2,}$")
        private const val HR_GLYPH = "────────────"
        private val BLOCK_MATH = Regex("^\\$\\$(.+)\\$\\$$")

        // CSS color order: #RGB, #RRGGBB or #RRGGBBAA
        private fun parseHexColor(value: String): Color? {
            val hex = value.removePrefix("#")
            fun nibble(i: Int) = hex[i].digitToIntOrNull(16)
            fun byte(i: Int) = nibble(i)?.let { (it shl 4) or (nibble(i + 1) ?: return null) }
            return when (hex.length) {
                3 -> {
                    val r = nibble(0) ?: return null
                    val g = nibble(1) ?: return null
                    val b = nibble(2) ?: return null
                    Color(r * 17, g * 17, b * 17, 255)
                }

                6 -> {
                    val r = byte(0) ?: return null
                    val g = byte(2) ?: return null
                    val b = byte(4) ?: return null
                    Color(r, g, b, 255)
                }

                8 -> {
                    val r = byte(0) ?: return null
                    val g = byte(2) ?: return null
                    val b = byte(4) ?: return null
                    val a = byte(6) ?: return null
                    Color(r, g, b, a)
                }

                else -> null
            }
        }

        private val INLINE_RULES = listOf(
            InlineRule(Regex("!\\[([^\\]\\n]*)]\\([^)\\n]*\\)")) { _, s ->
                SpanStyle(color = s.imageChipColor, background = s.imageChipBackground)
            },
            InlineRule(Regex("\\[([^\\]\\n]+)]\\([^)\\n]*\\)")) { _, s ->
                SpanStyle(color = s.linkColor, textDecoration = TextDecoration.Underline)
            },
            InlineRule(Regex("<font color=\"(#?[0-9a-fA-F]{3,8})\">(.+?)</font>"), contentGroup = 2) { m, s ->
                parseHexColor(m.groupValues[1])?.let { SpanStyle(color = it) } ?: SpanStyle(color = s.markerColor)
            },
            InlineRule(Regex("<u>(.+?)</u>")) { _, _ ->
                SpanStyle(textDecoration = TextDecoration.Underline)
            },
            InlineRule(Regex("`([^`\\n]+)`")) { _, s ->
                SpanStyle(fontFamily = s.monospace, background = s.codeBackground, color = s.codeTextColor)
            },
            InlineRule(Regex("\\*\\*\\*([^*\\n]+)\\*\\*\\*")) { _, _ ->
                SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
            },
            InlineRule(Regex("\\*\\*([^*\\n]+)\\*\\*")) { _, _ ->
                SpanStyle(fontWeight = FontWeight.Bold)
            },
            InlineRule(Regex("\\*([^*\\n]+)\\*")) { _, _ ->
                SpanStyle(fontStyle = FontStyle.Italic)
            },
            InlineRule(Regex("~~([^~\\n]+)~~")) { _, _ ->
                SpanStyle(textDecoration = TextDecoration.LineThrough)
            },
            InlineRule(Regex("==([^=\\n]+)==")) { _, s ->
                SpanStyle(background = s.highlightBackground)
            },
            // inline math: $x$ — content must not start/end with whitespace to avoid "$100 and $200" false positives
            InlineRule(Regex("\\$([^\\s$](?:[^$\\n]*[^\\s$])?)\\$")) { _, s ->
                SpanStyle(fontStyle = FontStyle.Italic, color = s.mathColor)
            },
        )
    }
}
