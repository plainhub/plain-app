package com.ismartcoding.plain.lib.markdown.utils

import androidx.compose.ui.graphics.Color
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the shared HTML attribute extraction helpers in [HtmlImgTag]
 * (`<img>` and `<font color="…">` tags) plus the [parseHtmlColor] parser.
 *
 * The regex covers the realistic attribute permutations a user is likely to type
 * inside markdown source: double-quoted, single-quoted, and (rarely) unquoted
 * `src` values; `alt` in either quote style; attribute ordering; and a handful
 * of malformed inputs that must NOT throw and must NOT match.
 */
class HtmlImgTagTest {

    // ── src extraction (regex-only) ──────────────────────────────────────

    @Test fun `double-quoted src is extracted`() {
        val match = HTML_IMG_TAG_REGEX.find("""<img src="app://note-images/1000002285.jpg" />""")!!
        assertEquals("app://note-images/1000002285.jpg", match.groupValues.drop(2).first { it.isNotEmpty() })
    }

    @Test fun `single-quoted src is extracted`() {
        val match = HTML_IMG_TAG_REGEX.find("""<img src='app://note-images/abc.png'/>""")!!
        assertEquals("app://note-images/abc.png", match.groupValues.drop(2).first { it.isNotEmpty() })
    }

    @Test fun `unquoted src is extracted`() {
        val match = HTML_IMG_TAG_REGEX.find("""<img src=app://note-images/abc.png />""")!!
        assertEquals("app://note-images/abc.png", match.groupValues.drop(2).first { it.isNotEmpty() })
    }

    @Test fun `https src is extracted`() {
        val match = HTML_IMG_TAG_REGEX.find("""<img src="https://example.com/x.png" alt="hi"/>""")!!
        assertEquals("https://example.com/x.png", match.groupValues.drop(2).first { it.isNotEmpty() })
    }

    @Test fun `src with attributes before is extracted`() {
        val raw = """<img class="foo" id="x" src="app://a/b.jpg" alt="b"/>"""
        val match = HTML_IMG_TAG_REGEX.find(raw)!!
        assertEquals("app://a/b.jpg", match.groupValues.drop(2).first { it.isNotEmpty() })
    }

    @Test fun `src with attributes after is extracted`() {
        val raw = """<img src="app://a/b.jpg" width="100" height="50" alt="b"/>"""
        val match = HTML_IMG_TAG_REGEX.find(raw)!!
        assertEquals("app://a/b.jpg", match.groupValues.drop(2).first { it.isNotEmpty() })
    }

    @Test fun `case-insensitive tag and attribute names`() {
        val raw = """<IMG SRC="app://a/b.jpg" ALT="x"/>"""
        val match = HTML_IMG_TAG_REGEX.find(raw)
        assertNotNull(match)
        assertEquals("app://a/b.jpg", match.groupValues.drop(2).first { it.isNotEmpty() })
    }

    @Test fun `non-img HTML tag does not match`() {
        assertNull(HTML_IMG_TAG_REGEX.find("""<br/>"""))
        assertNull(HTML_IMG_TAG_REGEX.find("""<a src="x">link</a>"""))
        assertNull(HTML_IMG_TAG_REGEX.find("""<div src="x"></div>"""))
    }

    @Test fun `img tag without src does not match src regex`() {
        // The regex requires `src=`, so an `<img>` with no src produces null.
        // (The `else` branch in buildMarkdownAnnotatedString relies on this.)
        assertNull(HTML_IMG_TAG_REGEX.find("""<img alt="oops" />"""))
    }

    @Test fun `empty src is detected as empty by extractHtmlImgSrc`() {
        // Match succeeds, but the captured `src` is empty — `extractHtmlImgSrc`
        // must return null in that case so the caller's `!src.isNullOrEmpty()`
        // guard cleanly skips the placeholder.
        val content = """prefix <img src="" alt="empty"/> suffix"""
        val node = findFirstHtmlTagNode(content)
        assertNull(node.extractHtmlImgSrc(content))
    }

    // ── alt extraction (regex-only) ──────────────────────────────────────

    @Test fun `double-quoted alt is extracted`() {
        val match = HTML_IMG_ALT_REGEX.find("""<img src="x" alt="hello world"/>""")!!
        assertEquals("hello world", match.groupValues.drop(2).first { it.isNotEmpty() })
    }

    @Test fun `single-quoted alt is extracted`() {
        val match = HTML_IMG_ALT_REGEX.find("""<img src='x' alt='hello world'/>""")!!
        assertEquals("hello world", match.groupValues.drop(2).first { it.isNotEmpty() })
    }

    @Test fun `missing alt does not match`() {
        assertNull(HTML_IMG_ALT_REGEX.find("""<img src="x" />"""))
    }

    // ── end-to-end against a real AST node ───────────────────────────────

    @Test fun `extractHtmlImgSrc finds the node's src via AST`() {
        val content = """Look <img src="app://note-images/1000002285.jpg" alt="photo"/> here"""
        val node = findFirstHtmlTagNode(content)
        assertEquals("app://note-images/1000002285.jpg", node.extractHtmlImgSrc(content))
        assertEquals("photo", node.extractHtmlImgAlt(content))
    }

    @Test fun `extractHtmlImgSrc returns null for non-img HTML tokens`() {
        // `<br/>` and `<span></span>` are HTML_TAG tokens too, but must return null.
        for (content in listOf("""before<br/>after""", """<span>x</span>""")) {
            val node = findFirstHtmlTagNode(content)
            assertNull(node.extractHtmlImgSrc(content), "expected null for: $content")
        }
    }

    @Test fun `extractHtmlImgSrc returns null for img without src`() {
        // `<img alt="oops"/>` on its own line is parsed as an HTML_BLOCK by
        // GFM (not a nested HTML_TAG), so the assertion is that the helper
        // is robust when handed any node — it returns null rather than
        // throwing if the raw text has no `src` attribute.
        val content = """prefix <img alt="oops"/> suffix"""
        val node = findFirstHtmlTagNode(content)
        assertNull(node.extractHtmlImgSrc(content))
    }

    @Test fun `extractHtmlImgAlt is null when alt attribute is missing`() {
        val content = """prefix <img src="app://x.png"/> suffix"""
        val node = findFirstHtmlTagNode(content)
        assertEquals("app://x.png", node.extractHtmlImgSrc(content))
        assertNull(node.extractHtmlImgAlt(content))
    }

    // ── <font color="…"> extraction (regex-only) ──────────────────────────

    @Test fun `font opening tag with double-quoted color is matched by regex`() {
        val match = HTML_FONT_OPEN_TAG_REGEX.find("""<font color="#FF0000">""")!!
        assertEquals("#FF0000", match.groupValues.drop(2).first { it.isNotEmpty() })
    }

    @Test fun `font opening tag with single-quoted color is matched by regex`() {
        val match = HTML_FONT_OPEN_TAG_REGEX.find("""<font color='red'>""")!!
        assertEquals("red", match.groupValues.drop(2).first { it.isNotEmpty() })
    }

    @Test fun `font opening tag with unquoted color is matched by regex`() {
        val match = HTML_FONT_OPEN_TAG_REGEX.find("""<font color=blue>""")!!
        assertEquals("blue", match.groupValues.drop(2).first { it.isNotEmpty() })
    }

    @Test fun `font closing tag is matched by regex`() {
        assertTrue(HTML_FONT_CLOSE_TAG_REGEX.matches("</font>"))
        assertTrue(HTML_FONT_CLOSE_TAG_REGEX.matches("</FONT >"))
        assertFalse(HTML_FONT_CLOSE_TAG_REGEX.matches("</fonta>"))
    }

    @Test fun `non-font tags do not match the font regex`() {
        assertNull(HTML_FONT_OPEN_TAG_REGEX.find("""<div color="red">"""))
        assertNull(HTML_FONT_OPEN_TAG_REGEX.find("""<span>"""))
    }

    @Test fun `font tag without color does not match the font regex`() {
        assertNull(HTML_FONT_OPEN_TAG_REGEX.find("""<font face="Arial">"""))
    }

    // ── <font color="…"> extraction via AST ───────────────────────────────

    @Test fun `extractFontColor finds color attribute via AST`() {
        val content = """text <font color="#00FF00">green</font> tail"""
        val openNode = findFirstHtmlTagMatching(content) { it.extractFontColor(content) != null }
        assertEquals("#00FF00", openNode.extractFontColor(content))
    }

    @Test fun `extractFontColor returns null for closing font tag`() {
        val content = """<font color="red">x</font>"""
        // The first HTML_TAG with a color is the opening tag; find the closing tag node.
        val nodes = findAllHtmlTagNodes(content)
        val closeNode = nodes.first { it.isFontCloseTag(content) }
        assertNull(closeNode.extractFontColor(content))
    }

    @Test fun `extractFontColor returns null for non-font HTML tokens`() {
        for (content in listOf("""before<br/>after""", """<span>x</span>""")) {
            val node = findFirstHtmlTagNode(content)
            assertNull(node.extractFontColor(content), "expected null for: $content")
        }
    }

    @Test fun `isFontCloseTag distinguishes closing from opening`() {
        val content = """<font color="red">x</font>"""
        val nodes = findAllHtmlTagNodes(content)
        assertEquals(2, nodes.size)
        assertFalse(nodes[0].isFontCloseTag(content))
        assertTrue(nodes[1].isFontCloseTag(content))
    }

    // ── parseHtmlColor ───────────────────────────────────────────────────

    @Test fun `parseHtmlColor parses RRGGBB hex`() {
        assertEquals(Color(0xFFFF0000), parseHtmlColor("#FF0000"))
        assertEquals(Color(0xFF00FF00), parseHtmlColor("#00FF00"))
    }

    @Test fun `parseHtmlColor parses RRGGBB without hash`() {
        assertEquals(Color(0xFFFF0000), parseHtmlColor("FF0000"))
    }

    @Test fun `parseHtmlColor parses AARRGGBB hex`() {
        assertEquals(Color(0xFFFF0000), parseHtmlColor("#FFFF0000"))
        assertEquals(Color(0x80FF0000), parseHtmlColor("#80FF0000"))
    }

    @Test fun `parseHtmlColor parses RGB short form`() {
        assertEquals(Color(0xFFFF0000), parseHtmlColor("#F00"))
        assertEquals(Color(0xFFFFFFFF), parseHtmlColor("#FFF"))
    }

    @Test fun `parseHtmlColor parses ARGB short form`() {
        assertEquals(Color(0xFFFF0000), parseHtmlColor("#FF00"))
    }

    @Test fun `parseHtmlColor parses named colours case-insensitively`() {
        assertEquals(Color(0xFFFF0000), parseHtmlColor("red"))
        assertEquals(Color(0xFFFF0000), parseHtmlColor("RED"))
        assertEquals(Color(0xFF0000FF), parseHtmlColor("Blue"))
        assertEquals(Color(0x00000000), parseHtmlColor("transparent"))
    }

    @Test fun `parseHtmlColor returns null for unrecognised values`() {
        assertNull(parseHtmlColor(""))
        assertNull(parseHtmlColor("notacolor"))
        assertNull(parseHtmlColor("#GGG"))
        assertNull(parseHtmlColor("#12345"))
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /**
     * Parse [content] with the same GFM flavour used by the renderer and return
     * the first `HTML_TAG` AST node. Tests use this instead of `node.children`
     * introspection so they exercise the same lexer the renderer sees.
     */
    private fun findFirstHtmlTagNode(content: String): ASTNode {
        val parsed = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(content)
        val found = findFirstHtmlTag(parsed.children)
        assertNotNull(found, "expected to find an HTML_TAG node in: $content")
        return found
    }

    private fun findFirstHtmlTag(nodes: List<ASTNode>): ASTNode? {
        for (n in nodes) {
            if (n.type == MarkdownTokenTypes.HTML_TAG) return n
            val child = findFirstHtmlTag(n.children)
            if (child != null) return child
        }
        return null
    }

    private fun findAllHtmlTagNodes(content: String): List<ASTNode> {
        val parsed = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(content)
        val result = mutableListOf<ASTNode>()
        collectHtmlTags(parsed.children, result)
        return result
    }

    private fun collectHtmlTags(nodes: List<ASTNode>, out: MutableList<ASTNode>) {
        for (n in nodes) {
            if (n.type == MarkdownTokenTypes.HTML_TAG) out.add(n)
            collectHtmlTags(n.children, out)
        }
    }

    private fun findFirstHtmlTagMatching(content: String, predicate: (ASTNode) -> Boolean): ASTNode {
        val nodes = findAllHtmlTagNodes(content)
        return nodes.first(predicate)
    }
}
