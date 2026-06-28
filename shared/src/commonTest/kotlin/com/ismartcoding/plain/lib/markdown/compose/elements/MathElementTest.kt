package com.ismartcoding.plain.lib.markdown.compose.elements

import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for the `BLOCK_MATH` / `INLINE_MATH` extractor used by
 * [RenderMathNode].
 *
 * The GFM lexer in `org.jetbrains:markdown` 0.7.5 wraps any `$…$` (inline)
 * or `$$…$$` (block) run between a leading and trailing `DOLLAR` token. The
 * extractor ([ASTNode.mathBody]) trims those delimiters and re-emits the
 * body wrapped in the matching `$` / `$$` pair so the downstream
 * `Latex` renderer can pick it up unchanged.
 *
 * These tests cover the realistic permutations a user can type:
 * - The Gauss integral (the canonical example)
 * - Simple inline math (`$a + b = c$`)
 * - Whitespace around the body
 * - Empty body (the `$$$$` case — degenerate but must not throw)
 * - Non-math nodes (a regular paragraph and an image), to assert the
 *   extractor is a no-op rather than throwing for unrelated AST nodes.
 *
 * NOTE: Kotlin string templates interpret `$` as the start of a
 * reference. To embed literal `$` characters in test fixtures and
 * expected values we either (a) build the string with `+`-concatenation
 * using a [dollar] helper, or (b) compare via `Char` arrays. Both keep
 * the lexer input and the expected output explicit and free of escapes.
 */
class MathElementTest {

    /** Single `$` character — never put a `$` in a raw string literal. */
    private val dollar = "$"

    // ── BLOCK_MATH ───────────────────────────────────────────────────────

    @Test fun `gauss integral block math round-trips with delimiters`() {
        val content = dollar + dollar +
            "\n\\int_{-\\infty}^\\infty e^{-x^2} \\, dx = \\sqrt{\\pi}\n" +
            dollar + dollar
        val node = findFirstMathNode(content)
        assertEquals(GFMElementTypes.BLOCK_MATH, node.type)
        // `mathBody` calls `String.trim()` on the inner body, so the
        // surrounding newlines around `\int…` are dropped before the
        // delimiters are re-applied. The expected output is therefore
        // `$$<body>$$` with no leading/trailing whitespace.
        assertEquals(
            dollar + dollar + "\\int_{-\\infty}^\\infty e^{-x^2} \\, dx = \\sqrt{\\pi}" + dollar + dollar,
            node.mathBody(content)
        )
    }

    @Test fun `single-line block math keeps its delimiters`() {
        // A single-line `$$x^2$$` run inside a paragraph is also parsed as
        // BLOCK_MATH (not INLINE_MATH) by the GFM lexer — guard that.
        val content = "text with " + dollar + dollar + "x^2" + dollar + dollar + " inline"
        val node = findFirstMathNode(content)
        assertEquals(GFMElementTypes.BLOCK_MATH, node.type)
        assertEquals(dollar + dollar + "x^2" + dollar + dollar, node.mathBody(content))
    }

    @Test fun `block math body is trimmed of surrounding whitespace`() {
        val content = dollar + dollar + "  \n  x + y  \n  " + dollar + dollar
        val node = findFirstMathNode(content)
        assertEquals(dollar + dollar + "x + y" + dollar + dollar, node.mathBody(content))
    }

    // ── INLINE_MATH ──────────────────────────────────────────────────────

    @Test fun `inline math is wrapped in single dollars`() {
        val content = dollar + "a + b = c" + dollar
        val node = findFirstMathNode(content)
        assertEquals(GFMElementTypes.INLINE_MATH, node.type)
        assertEquals(dollar + "a + b = c" + dollar, node.mathBody(content))
    }

    @Test fun `inline math with surrounding text is still INLINE_MATH`() {
        val content = "Euler's " + dollar + "e^{i\\pi} + 1 = 0" + dollar + " identity"
        val node = findFirstMathNode(content)
        assertEquals(GFMElementTypes.INLINE_MATH, node.type)
        assertEquals(dollar + "e^{i\\pi} + 1 = 0" + dollar, node.mathBody(content))
    }

    // ── edge cases ───────────────────────────────────────────────────────

    @Test fun `whitespace-only body returns delimiters without throwing`() {
        // The GFM lexer rejects a truly empty `$$$$` (it does not produce
        // a math node at all), so the degenerate case the helper must
        // gracefully handle is a whitespace-only body, where `trim()` empties
        // the inner text. The extractor must not throw and must return the
        // delimiter pair back to the caller.
        val content = dollar + dollar + "   \n   " + dollar + dollar
        val node = findFirstMathNode(content)
        val rendered = node.mathBody(content)
        assertNotNull(rendered)
        assertEquals(dollar + dollar + dollar + dollar, rendered)
    }

    @Test fun `non-math AST node returns null`() {
        // `mathBody` is an extension over `ASTNode`, so a caller could hand
        // it any node (e.g. a `PARAGRAPH` or an `IMAGE`). The guard at the
        // top of the function must return null rather than throwing.
        for (content in listOf("hello world", "![alt](https://example.com/a.png)")) {
            val tree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(content)
            val firstNonMath = tree.children.firstOrNull {
                it.type != GFMElementTypes.BLOCK_MATH && it.type != GFMElementTypes.INLINE_MATH
            }
            assertNotNull(firstNonMath, "expected a non-math root child for: $content")
            assertNull(firstNonMath.mathBody(content), "expected null for: $content / ${firstNonMath.type}")
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /**
     * Parse [content] with the same GFM flavour used by the renderer and
     * return the first `BLOCK_MATH` / `INLINE_MATH` AST node. Tests use
     * this rather than `node.children` introspection so they exercise the
     * same lexer the renderer sees.
     */
    private fun findFirstMathNode(content: String): ASTNode {
        val tree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(content)
        val found = findFirstMath(tree.children)
        assertNotNull(found, "expected to find a math node in:\n$content")
        return found
    }

    private fun findFirstMath(nodes: List<ASTNode>): ASTNode? {
        for (n in nodes) {
            if (n.type == GFMElementTypes.BLOCK_MATH || n.type == GFMElementTypes.INLINE_MATH) return n
            val child = findFirstMath(n.children)
            if (child != null) return child
        }
        return null
    }
}
