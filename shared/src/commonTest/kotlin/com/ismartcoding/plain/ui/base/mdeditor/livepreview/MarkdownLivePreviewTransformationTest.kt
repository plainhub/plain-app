package com.ismartcoding.plain.ui.base.mdeditor.livepreview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownLivePreviewTransformationTest {

    private val styles = LivePreviewStyles(
        headingColor = Color.Black,
        linkColor = Color.Blue,
        codeTextColor = Color.Black,
        codeBackground = Color.Gray,
        quoteColor = Color.DarkGray,
        markerColor = Color.DarkGray,
        mathColor = Color.Magenta,
        highlightBackground = Color.Yellow,
        imageChipColor = Color.White,
        imageChipBackground = Color.Blue,
        monospace = FontFamily.Monospace,
    )

    private fun transformation() = MarkdownLivePreviewTransformation(styles)

    // Simulates applying the planned edits to a buffer, mimicking TextFieldBuffer.replace.
    private fun output(text: String, selectionStart: Int, selectionEnd: Int = selectionStart): String {
        val sb = StringBuilder(text)
        val sourceLines = computeSourceLines(text, selectionStart, selectionEnd)
        for (e in transformation().planEdits(text, sourceLines)) {
            sb.replace(e.start, e.start + e.origLen, e.text)
        }
        return sb.toString()
    }

    // Puts the caret on a trailing dummy line so every markdown line renders.
    private fun rendered(md: String): String {
        val text = "$md\n."
        return output(text, text.length).removeSuffix("\n.")
    }

    private fun spansOf(md: String): List<SpanStyle> {
        val text = "$md\n."
        return transformation().planEdits(text, computeSourceLines(text, text.length, text.length))
            .singleOrNull()?.spans?.map { it.style } ?: emptyList()
    }

    @Test
    fun headingMarkerHidden() {
        assertEquals("Title\nplain", output("# Title\nplain", 20))
        assertEquals("# Title\nplain", output("# Title\nplain", 3))
    }

    @Test
    fun inlineMarkersHidden() {
        assertEquals("a b c", rendered("a **b** c"))
        assertEquals("i", rendered("*i*"))
        assertEquals("bi", rendered("***bi***"))
        assertEquals("s", rendered("~~s~~"))
        assertEquals("t", rendered("[t](http://example.com)"))
        assertEquals("alt", rendered("![alt](img.png)"))
        assertEquals("code", rendered("`code`"))
        assertEquals("u", rendered("<u>u</u>"))
        assertEquals("colored", rendered("<font color=\"#FF0000\">colored</font>"))
        assertEquals("colored", rendered("<font color=\"FF0000FF\">colored</font>"))
    }

    @Test
    fun blockMarkersReplacedOrHidden() {
        assertEquals("• item", rendered("- item"))
        assertEquals("• item", rendered("* item"))
        assertEquals("☑ done", rendered("- [x] done"))
        assertEquals("☐ todo", rendered("- [ ] todo"))
        assertEquals("1. one", rendered("1. one"))
        assertEquals("▎ quoted", rendered("> quoted"))
        assertEquals("────────────", rendered("---"))
    }

    @Test
    fun fenceLinesHiddenContentKept() {
        assertEquals("val a = 1\nafter", output("```kotlin\nval a = 1\n```\nafter", 40))
    }

    @Test
    fun mathBlockDelimitersHiddenContentStyled() {
        assertEquals("\\int x dx\nafter", output("$$\n\\int x dx\n$$\nafter", 40))
        assertEquals("x^2", rendered("\$\$x^2\$\$"))
        assertEquals("$100 and $200", rendered("$100 and $200"))
    }

    @Test
    fun inlineMathHidden() {
        assertEquals("a b c", rendered("a \$b\$ c"))
        assertEquals("a b", rendered("\$a\$ \$b\$"))
    }

    @Test
    fun selectionLinesStayVerbatim() {
        val doc = "# A\n- [x] done\n```\ncode\n```"
        assertEquals("A\n- [x] done\ncode\n", output(doc, 6))
        assertEquals("A\n- [x] done\ncode\n", output(doc, 6, 10))
    }

    @Test
    fun mixedDocumentRenders() {
        val doc = "# Trip\n\n- Paris\n- [x] book hotel\n\n**bold** and *italic*\n\n```\ncode\n```\n\n> tip\nend"
        assertEquals(
            "Trip\n\n• Paris\n☑ book hotel\n\nbold and italic\n\ncode\n\n▎ tip\nend",
            output(doc, doc.length),
        )
    }

    @Test
    fun emptyAndPlainStayIdentity() {
        assertEquals("", output("", 0))
        assertEquals("plain text", output("plain text", 4))
        assertTrue(transformation().planEdits("plain text", 0..0).isEmpty())
    }

    @Test
    fun headingGetsBoldSpan() {
        val spans = spansOf("# Title")
        assertEquals(1, spans.size)
        assertEquals(FontWeight.Bold, spans[0].fontWeight)
    }

    @Test
    fun computeSourceLinesBasics() {
        assertEquals(0..0, computeSourceLines("ab\ncd", 1, 2))
        assertEquals(0..1, computeSourceLines("ab\ncd", 1, 4))
        assertEquals(1..1, computeSourceLines("ab\ncd\nef", 4, 4))
        assertEquals(2..2, computeSourceLines("ab\ncd\nef", 6, 6))
    }
}
