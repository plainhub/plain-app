package com.ismartcoding.plain.lib.extensions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [String.getSummary] — strips Markdown image syntax and
 * `<img>` HTML, replaces them with the 🖼 emoji, then collapses newlines
 * and leading whitespace. Used by feed entry previews / notification bodies.
 */
class StringSummaryTest {

    @Test
    fun emptyString() {
        assertEquals("", "".getSummary())
    }

    @Test
    fun plainTextUnchanged() {
        assertEquals("hello world", "hello world".getSummary())
    }

    @Test
    fun plainTextWithNewlinesHasNewlinesRemoved() {
        assertEquals("helloworld", "hello\nworld".getSummary())
    }

    @Test
    fun leadingWhitespaceStripped() {
        assertEquals("hello", "   hello".getSummary())
        assertEquals("hello", "\n\nhello".getSummary())
    }

    @Test
    fun markdownInlineImageReplaced() {
        assertEquals("🖼", "![alt](url.png)".getSummary())
    }

    @Test
    fun markdownImageWithEmptyAltReplaced() {
        assertEquals("🖼", "![](url.png)".getSummary())
    }

    @Test
    fun markdownImageReferenceStyleReplaced() {
        // Reference-style: ![alt][id]
        assertEquals("🖼", "![alt][id]".getSummary())
    }

    @Test
    fun htmlImgTagReplaced() {
        assertEquals("🖼", "<img src='x.png'>".getSummary())
        assertEquals("🖼", "<IMG SRC='x.png'>".getSummary())
    }

    @Test
    fun htmlImgTagWithAttributesReplaced() {
        assertEquals("🖼", "<img src=\"x.png\" alt=\"hi\" class=\"thumb\">".getSummary())
    }

    @Test
    fun mixedImageSyntaxAllReplaced() {
        val input = "![a](1.png) and <img src='2.png'> and ![b](3.png)"
        val result = input.getSummary()
        assertEquals("🖼 and 🖼 and 🖼", result)
    }

    @Test
    fun textAndImageInterleaved() {
        val input = "see ![pic](x.png) here"
        assertEquals("see 🖼 here", input.getSummary())
    }

    @Test
    fun newlinesAroundImagesCollapsed() {
        val input = "before\n![pic](x.png)\nafter"
        assertEquals("before🖼after", input.getSummary())
    }

    @Test
    fun chineseContentSupported() {
        assertEquals("看🖼图", "看\n![图](x.png)\n图".getSummary())
    }

    @Test
    fun doesNotMatchNonImageMarkdownLinks() {
        // Plain markdown link (no leading !) should NOT be replaced.
        val result = "[click](https://example.com)".getSummary()
        assertTrue(result.contains("[click]"), "non-image link should be preserved: $result")
        assertFalse(result.contains("🖼"))
    }
}
