package com.ismartcoding.plain.lib.html2md

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [MDConverter] — pure-Kotlin HTML→Markdown conversion used by
 * feed entry processing (`RssItem.toDFeedEntry` -> `MDConverter().convert(...)`).
 *
 * Coverage targets the HTML patterns RSS/Atom feeds actually emit: paragraphs,
 * inline emphasis, links, headers, lists, code blocks, blockquotes, images,
 * tables, and combination/escaping edge cases.
 */
class MDConverterTest {

    private val converter = MDConverter()

    private fun convert(html: String): String = converter.convert(html)

    @Test
    fun emptyInput() {
        assertEquals("", convert(""))
    }

    @Test
    fun paragraph() {
        assertEquals("hello world", convert("<p>hello world</p>"))
    }

    @Test
    fun multipleParagraphs() {
        val md = convert("<p>first</p><p>second</p>")
        assertTrue(md.contains("first"))
        assertTrue(md.contains("second"))
        // Two paragraphs are separated by a blank line in Markdown.
        assertTrue(md.contains("\n\n"))
    }

    @Test
    fun bold() {
        assertEquals("**hi**", convert("<b>hi</b>"))
        assertEquals("**hi**", convert("<strong>hi</strong>"))
    }

    @Test
    fun italic() {
        assertEquals("_hi_", convert("<i>hi</i>"))
        assertEquals("_hi_", convert("<em>hi</em>"))
    }

    @Test
    fun link() {
        val md = convert("<a href=\"https://example.com\">click</a>")
        assertTrue(md.contains("[click]"), "expected markdown link text, got: $md")
        assertTrue(md.contains("(https://example.com)"), "expected link URL, got: $md")
    }

    @Test
    fun image() {
        val md = convert("<img src=\"x.png\" alt=\"alt text\">")
        assertTrue(md.contains("![alt text]"), "expected image markdown, got: $md")
        assertTrue(md.contains("(x.png)"), "expected image URL, got: $md")
    }

    @Test
    fun headerLevels() {
        // Default Options uses HeadingStyle.SETEXT for h1/h2 (underline with
        // = or -) and atx-style for h3-h6.
        assertEquals("hi\n==", convert("<h1>hi</h1>").trim())
        assertEquals("hi\n--", convert("<h2>hi</h2>").trim())
        assertEquals("### hi", convert("<h3>hi</h3>").trim())
        assertEquals("#### hi", convert("<h4>hi</h4>").trim())
        assertEquals("##### hi", convert("<h5>hi</h5>").trim())
        assertEquals("###### hi", convert("<h6>hi</h6>").trim())
    }

    @Test
    fun unorderedList() {
        val md = convert("<ul><li>one</li><li>two</li></ul>")
        // Default bullet prefix is `*` + three spaces.
        assertTrue(md.contains("*   one"), "expected list item, got: $md")
        assertTrue(md.contains("*   two"))
    }

    @Test
    fun orderedList() {
        val md = convert("<ol><li>first</li><li>second</li></ol>")
        // Default ordered prefix is `N.` + two spaces.
        assertTrue(md.contains("1.  first"), "expected numbered item, got: $md")
        assertTrue(md.contains("2.  second"))
    }

    @Test
    fun blockquote() {
        val md = convert("<blockquote>quoted text</blockquote>")
        assertTrue(md.contains("> quoted text"), "expected blockquote, got: $md")
    }

    @Test
    fun inlineCode() {
        val md = convert("<code>val x = 1</code>")
        assertTrue(md.contains("`val x = 1`"), "expected inline code, got: $md")
    }

    @Test
    fun preBlock() {
        val md = convert("<pre><code>line one\nline two</code></pre>")
        // Pre blocks are emitted as fenced code blocks.
        assertTrue(md.contains("line one"))
        assertTrue(md.contains("line two"))
    }

    @Test
    fun lineBreak() {
        val md = convert("<p>line one<br>line two</p>")
        assertTrue(md.contains("line one"))
        assertTrue(md.contains("line two"))
    }

    @Test
    fun horizontalRule() {
        val md = convert("<hr>")
        // Default Options.hr is "* * *" (not "---").
        assertTrue(md.contains("* * *"), "expected '* * *' horizontal rule, got: $md")
    }

    @Test
    fun nestedFormatting() {
        val md = convert("<p><b>bold <i>and italic</i></b></p>")
        assertTrue(md.contains("**bold"))
        assertTrue(md.contains("and italic"))
    }

    @Test
    fun linkInsideParagraph() {
        val md = convert("<p>see <a href=\"https://example.com\">docs</a> for info</p>")
        assertTrue(md.contains("[docs]"))
        assertTrue(md.contains("(https://example.com)"))
        assertTrue(md.contains("see"))
        assertTrue(md.contains("for info"))
    }

    @Test
    fun entitiesDecodedBeforeConversion() {
        val md = convert("<p>&amp; &lt; &gt; &quot;</p>")
        // Entities should be decoded to literal chars then escaped for MD.
        assertTrue(md.contains("&"))
        assertTrue(md.contains("<") || md.contains("\\<"))
    }

    @Test
    fun leadingTrailingWhitespaceStripped() {
        val md = convert("<p>hi</p>\n\n\n")
        assertEquals("hi", md)
    }

    @Test
    fun nonAsciiContentPreserved() {
        val md = convert("<p>中文内容</p>")
        assertEquals("中文内容", md)
    }

    @Test
    fun divTreatedAsBlock() {
        val md = convert("<div>line one</div><div>line two</div>")
        assertTrue(md.contains("line one"))
        assertTrue(md.contains("line two"))
    }

    @Test
    fun spanTreatedAsInline() {
        val md = convert("<p>before <span>middle</span> after</p>")
        // span is inline — text should all be on one line.
        assertTrue(md.contains("before"))
        assertTrue(md.contains("middle"))
        assertTrue(md.contains("after"))
        assertFalse(md.contains("\n\n"), "inline span should not introduce paragraph breaks: $md")
    }

    private fun assertFalse(actual: Boolean, message: String) {
        kotlin.test.assertFalse(actual, message)
    }
}
