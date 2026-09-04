package com.ismartcoding.plain.lib.extensions

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownTitleTest {

    @Test
    fun `uses first h1 text as title`() {
        assertEquals("My Title", "# My Title\n\nbody text".getMarkdownTitle())
    }

    @Test
    fun `skips h2 and finds h1 anywhere`() {
        assertEquals("Real", "## Sub\nsome text\n# Real\nmore".getMarkdownTitle())
    }

    @Test
    fun `h1 requires space after hash`() {
        assertEquals("#tagabc", "#tag\nabc".getMarkdownTitle())
    }

    @Test
    fun `falls back to first 50 chars without h1`() {
        val body = (1..100).joinToString("") { "x" }
        assertEquals(body.take(50), body.getMarkdownTitle())
    }

    @Test
    fun `fallback replaces images with emoji before cutting`() {
        val body = "![alt](https://example.com/" + "x".repeat(80) + ") tail text"
        assertEquals("🖼 tail text", body.getMarkdownTitle())
    }

    @Test
    fun `replaces reference images and html img tags`() {
        assertEquals("🖼", "![alt][id]".getMarkdownTitle())
        assertEquals("🖼", "<img src='x.png'>".getMarkdownTitle())
        assertEquals("🖼", "<IMG SRC='x.png'>".getMarkdownTitle())
        assertEquals("see🖼here", "see\n![图](x.png)\nhere".getMarkdownTitle())
    }

    @Test
    fun `fallback removes newlines`() {
        assertEquals("ab", "a\nb".getMarkdownTitle())
    }

    @Test
    fun `empty content yields empty title`() {
        assertEquals("", "".getMarkdownTitle())
    }
}
