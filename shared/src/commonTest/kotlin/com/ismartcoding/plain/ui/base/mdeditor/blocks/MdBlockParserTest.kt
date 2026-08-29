package com.ismartcoding.plain.ui.base.mdeditor.blocks

import kotlin.test.Test
import kotlin.test.assertEquals

class MdBlockParserTest {

    private fun kinds(text: String) = MdBlockParser.parse(text).map { it.kind }

    // ── text lines ───

    @Test
    fun `plain lines become one text block per line`() {
        val blocks = MdBlockParser.parse("hello\nworld")
        assertEquals(listOf(MdBlockKind.TEXT, MdBlockKind.TEXT), blocks.map { it.kind })
        assertEquals("hello", blocks[0].content)
        assertEquals("world", blocks[1].content)
    }

    @Test
    fun `empty text yields a single empty text block`() {
        val blocks = MdBlockParser.parse("")
        assertEquals(1, blocks.size)
        assertEquals(MdBlockKind.TEXT, blocks[0].kind)
        assertEquals("", blocks[0].content)
    }

    @Test
    fun `trailing newline keeps an empty trailing text block`() {
        val blocks = MdBlockParser.parse("a\n")
        assertEquals(listOf("a", ""), blocks.map { it.content })
    }

    // ── code fences ───

    @Test
    fun `closed fence is one code block including fence lines`() {
        val blocks = MdBlockParser.parse("```kotlin\nval a = 1\n```\nafter")
        assertEquals(listOf(MdBlockKind.CODE, MdBlockKind.TEXT), blocks.map { it.kind })
        assertEquals("```kotlin\nval a = 1\n```", blocks[0].content)
        assertEquals("after", blocks[1].content)
    }

    @Test
    fun `unclosed fence absorbs the rest of the document`() {
        val blocks = MdBlockParser.parse("```\ncode\nmore")
        assertEquals(1, blocks.size)
        assertEquals(MdBlockKind.CODE, blocks[0].kind)
        assertEquals("```\ncode\nmore", blocks[0].content)
    }

    @Test
    fun `tilde fences are recognized`() {
        val blocks = MdBlockParser.parse("~~~\nx\n~~~")
        assertEquals(1, blocks.size)
        assertEquals(MdBlockKind.CODE, blocks[0].kind)
    }

    @Test
    fun `fence info strings do not close the fence`() {
        assertEquals(1, MdBlockParser.parse("```\n``` js\n").size)
    }

    // ── math ───

    @Test
    fun `block math with delimiters on their own lines`() {
        val blocks = MdBlockParser.parse("\$\$\nx^2\n\$\$\nend")
        assertEquals(listOf(MdBlockKind.MATH, MdBlockKind.TEXT), blocks.map { it.kind })
        assertEquals("\$\$\nx^2\n\$\$", blocks[0].content)
    }

    @Test
    fun `unclosed math absorbs to end of document`() {
        val blocks = MdBlockParser.parse("\$\$\nx")
        assertEquals(1, blocks.size)
        assertEquals(MdBlockKind.MATH, blocks[0].kind)
    }

    @Test
    fun `single line block math`() {
        assertEquals(listOf(MdBlockKind.MATH), kinds("\$\$x^2\$\$"))
    }

    // ── tables ───

    @Test
    fun `table with separator and rows is one block`() {
        val blocks = MdBlockParser.parse("| a | b |\n|---|---|\n| 1 | 2 |\ntail")
        assertEquals(listOf(MdBlockKind.TABLE, MdBlockKind.TEXT), blocks.map { it.kind })
        assertEquals("| a | b |\n|---|---|\n| 1 | 2 |", blocks[0].content)
    }

    @Test
    fun `aligned separators are accepted`() {
        assertEquals(listOf(MdBlockKind.TABLE), kinds("| a | b |\n|:---:|---:|\n| 1 | 2 |"))
    }

    @Test
    fun `pipe line without separator stays text`() {
        assertEquals(listOf(MdBlockKind.TEXT, MdBlockKind.TEXT), kinds("| a |\n| 1 |"))
    }

    // ── images ───

    @Test
    fun `standalone markdown image is an image block`() {
        assertEquals(listOf(MdBlockKind.IMAGE), kinds("![alt](https://x.com/a.png)"))
    }

    @Test
    fun `standalone html image is an image block`() {
        assertEquals(listOf(MdBlockKind.IMAGE), kinds("""<img src="https://x.com/a.png" width="100" />"""))
    }

    @Test
    fun `inline image inside text stays text`() {
        assertEquals(listOf(MdBlockKind.TEXT), kinds("look ![alt](https://x.com/a.png) here"))
    }

    // ── roundtrip ───

    @Test
    fun `serialize is the inverse of parse`() {
        val docs = listOf(
            "",
            "hello\nworld",
            "# Title\n\n- a\n- [x] b\n",
            "```kotlin\nval a = 1\n```\nafter",
            "\$\$\n\\\\int x dx\n\$\$\n",
            "| a | b |\n|---|---|\n| 1 | 2 |",
            "![alt](u)\ntext\n<img src=\"u\" />",
            "``` \nunclosed fence\nstill code",
        )
        docs.forEach { doc ->
            assertEquals(doc, MdBlockParser.serialize(MdBlockParser.parse(doc)), "roundtrip failed for: $doc")
        }
    }
}
