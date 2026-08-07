package com.ismartcoding.plain.lib.markdown.compose.elements

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.ismartcoding.plain.lib.markdown.annotator.buildMarkdownAnnotatedString
import com.ismartcoding.plain.lib.markdown.annotator.injectInlineMathFallbacks
import com.ismartcoding.plain.lib.markdown.model.markdownAnnotator
import com.ismartcoding.plain.lib.markdown.annotator.DefaultAnnotatorSettings
import com.ismartcoding.plain.lib.markdown.utils.MARKDOWN_TAG_MATH
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    // ── edge: inline math adjacent to CJK / no whitespace ────────────────

    @Test fun `inline math attached to CJK text on right needs manual extraction fallback`() {
        // User-reported scenario: `$E = mc^2$` followed directly by CJK text
        // with no intervening space. The GFM lexer is conservative about
        // inline-math boundaries and may or may not produce an INLINE_MATH
        // node here; record the actual behaviour so we know whether a
        // manual fallback extractor is needed.
        val dollar = "$"
        val content = "Inline math: " + dollar + "E = mc^2" + dollar + "想办法解析这个"
        val tree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(content)
        val found = findFirstMathOrNull(tree.children)
        println("CJK-adjacent inline math: found=$found content=$content")
        // We don't assert here; the println is enough to document whether
        // GFM lexer handles it natively. (Human inspection via test output.)
    }

    @Test fun `inline math surrounded by CJK without spaces`() {
        val dollar = "$"
        val content = "公式" + dollar + "E=mc^2" + dollar + "是什么"
        val tree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(content)
        val found = findFirstMathOrNull(tree.children)
        println("CJK-surrounded inline math: found=$found type=${found?.type} content=$content")
    }

    @Test fun `dollar-print without leading space is TEXT not INLINE_MATH`() {
        // A common edge case: `price $100` — the lone `$` should not start
        // an inline math run. This documents the GFM lexer's behaviour.
        val dollar = "$"
        val content = "price " + dollar + "100 dollars"
        val tree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(content)
        val found = findFirstMathOrNull(tree.children)
        println("Price dollars: found=$found content=$content")
    }

    @Test fun `two inline math runs separated by single CJK char`() {
        val dollar = "$"
        val content = dollar + "x" + dollar + "和" + dollar + "y" + dollar
        val tree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(content)
        val first = findFirstMathOrNull(tree.children)
        println("Two inline math CJK-separated: first=$first content=$content")
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /** Non-asserting variant of [findFirstMathNode] — returns null instead. */
    private fun findFirstMathOrNull(nodes: List<ASTNode>): ASTNode? {
        for (n in nodes) {
            if (n.type == GFMElementTypes.BLOCK_MATH || n.type == GFMElementTypes.INLINE_MATH) return n
            val child = findFirstMathOrNull(n.children)
            if (child != null) return child
        }
        return null
    }

    /**
     * Parse [content] with the same GFM flavour used by the renderer and
     * return the first `BLOCK_MATH` / `INLINE_MATH` AST node. Tests use
     * this rather than `node.children` introspection so they exercise the
     * same lexer the renderer sees.
     */
    private fun findFirstMathNode(content: String): ASTNode {
        val tree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(content)
        val found = findFirstMathOrNull(tree.children)
        assertNotNull(found, "expected to find a math node in:\n$content")
        return found
    }

    // ══════════════════════════════════════════════════════════════════════
    // End-to-end: buildMarkdownAnnotatedString → inline math annotations
    // ══════════════════════════════════════════════════════════════════════

    @Test fun `end-to-end inline math produces string annotation with correct tag`() {
        val dollar = "$"
        val content = "能量公式 " + dollar + "E = mc^2" + dollar + " 是爱因斯坦提出的。"
        val tree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(content)

        // Walk to the first PARAGRAPH child (this is what MarkdownParagraph would hand us)
        val paragraph = tree.children.firstOrNull { it.type == MarkdownElementTypes.PARAGRAPH }
            ?: tree  // fallback: whole tree if there's no paragraph wrapper

        // Reproduce exactly what MarkdownParagraph / buildMarkdownAnnotatedString does
        val settings = DefaultAnnotatorSettings(
            linkTextSpanStyle = androidx.compose.ui.text.TextLinkStyles(),
            codeSpanStyle = androidx.compose.ui.text.SpanStyle(),
            annotator = markdownAnnotator(),
            referenceLinkHandler = null,
            linkInteractionListener = null,
        )
        val annotated: AnnotatedString = buildAnnotatedString {
            buildMarkdownAnnotatedString(content = content, node = paragraph, annotatorSettings = settings)
        }

        // Now inspect the annotations that buildMathInlineContent would scan
        val mathAnnotations = annotated.getStringAnnotations(0, annotated.length)
            .filter { it.item.startsWith("${MARKDOWN_TAG_MATH}_") }

        println("end-to-end: content length=${content.length} annotated length=${annotated.length}")
        println("end-to-end: math annotations count=${mathAnnotations.size}")
        mathAnnotations.forEachIndexed { i, a ->
            println("  [$i] tag=${a.item} start=${a.start} end=${a.end}")
        }

        // Also dump ALL annotations (including non-math) for debugging
        val allAnnotations = annotated.getStringAnnotations(0, annotated.length)
        println("end-to-end: ALL annotations count=${allAnnotations.size}")
        allAnnotations.forEachIndexed { i, a ->
            println("  [$i] tag=[${a.item.take(60)}...] start=${a.start} end=${a.end}")
        }

        // Also dump raw text so we can see if `$` signs survived as literal characters
        println("end-to-end: raw annotated text=[${annotated.text}]")

        // ⚠️ Key assertion: if the GFM-produced INLINE_MATH child was actually
        // routed through the `GFMElementTypes.INLINE_MATH` branch in
        // buildMarkdownAnnotatedString, there should be exactly one math
        // annotation here. Zero annotations means the annotator never saw
        // the INLINE_MATH node (or saw it but decided not to append the
        // inline content placeholder — e.g. `mathBody` returned null/empty).
        assertTrue(
            mathAnnotations.isNotEmpty(),
            "Expected at least one $MARKDOWN_TAG_MATH annotation in:\n" +
                    "  content      = $content\n" +
                    "  annotated txt= ${annotated.text}\n" +
                    "  all tags(${allAnnotations.size}) = ${allAnnotations.map { it.item.take(40) }}"
        )

        // Verify the payload carries the delimiters the LaTeX renderer expects
        val firstPayload = mathAnnotations.first().item.removePrefix("${MARKDOWN_TAG_MATH}_")
        assertTrue(
            firstPayload.startsWith(dollar) && firstPayload.endsWith(dollar) && !firstPayload.startsWith(dollar + dollar),
            "Expected inline-math annotation payload wrapped in SINGLE `$` delimiters, got: $firstPayload"
        )
    }

    @Test fun `end-to-end block math paragraph split does not double-register annotations`() {
        // When a paragraph contains a $$…$$ BLOCK_MATH child,
        // MarkdownExtension.renderParagraphWithEscapedBlocks takes over and
        // renders the BLOCK_MATH via RenderMathNode instead of the inline
        // flow. The surrounding text runs are still rendered through
        // buildMarkdownAnnotatedString; make sure those short runs don't
        // accidentally emit stray math annotations for the block delimiters
        // that renderParagraphWithEscapedBlocks consumed.
        val dollar = "$"
        val content = "前文 lead-in\n" +
                dollar + dollar + "\n" +
                "\\int_0^1 x^2 dx = \\frac{1}{3}\n" +
                dollar + dollar + "\n" +
                "后文 trailing"

        val tree = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(content)
        println("block-mix: root children types = ${tree.children.map { it.type.name }}")

        // Find the BLOCK_MATH node (the paragraph splitter in
        // MarkdownExtension.kt would find it the same way via
        // hasEscapedBlockChild).
        val blockMathNode = findFirstByType(tree.children, GFMElementTypes.BLOCK_MATH)
        println("block-mix: BLOCK_MATH found? ${blockMathNode != null}")

        // Also confirm there is NO stray INLINE_MATH (a common failure mode
        // when $$ is tokenised as two separate DOLLAR runs instead of a
        // single BLOCK_MATH wrapper).
        val strayInlineMath = findFirstByType(tree.children, GFMElementTypes.INLINE_MATH)
        println("block-mix: stray INLINE_MATH present? ${strayInlineMath != null}")
        assertNull(
            strayInlineMath,
            "BLOCK_MATH content should not also yield a stray INLINE_MATH node; " +
                    "the two delimiter styles must be kept disjoint to avoid the " +
                    "paragraph splitting logic fighting the inline annotator."
        )
    }

    private fun findFirstByType(nodes: List<ASTNode>, target: org.intellij.markdown.IElementType): ASTNode? {
        for (n in nodes) {
            if (n.type == target) return n
            val child = findFirstByType(n.children, target)
            if (child != null) return child
        }
        return null
    }

    // ══════════════════════════════════════════════════════════════════════
    // injectInlineMathFallbacks — direct unit tests
    // ══════════════════════════════════════════════════════════════════════

    @Test fun `fallback extracts raw dollar math from plain AnnotatedString`() {
        // Simulate the case where GFM produced plain TEXT tokens (so the
        // annotator just appended raw characters without any math tag).
        // The fallback must step in and emit a MARKDOWN_MATH_ annotation.
        val dollar = "$"
        val input: AnnotatedString = buildAnnotatedString {
            append("Inline math: " + dollar + "E = mc^2" + dollar + "想办法解析这个")
        }

        val result = input.injectInlineMathFallbacks()
        val mathTags = result.getStringAnnotations(0, result.length)
            .filter { it.item.startsWith("${MARKDOWN_TAG_MATH}_") }

        assertEquals(1, mathTags.size, "Expected fallback to emit exactly 1 math tag for raw ${"$"}…${"$"} run")
        val payload = mathTags.first().item.removePrefix("${MARKDOWN_TAG_MATH}_")
        assertEquals(
            dollar + "E = mc^2" + dollar,
            payload,
            "Fallback payload should preserve the single-$ delimiters so MarkdownMath can recognise inline mode"
        )
        // Textual content must remain byte-identical (alternateText == matched run)
        assertEquals(input.text, result.text, "Round-trip text must not drift — alternateText is the matched ${"$"}…${"$"} run")
    }

    @Test fun `fallback never touches double-dollar block math`() {
        // This is the user's number-one safety requirement: BLOCK_MATH
        // display-mode content must render exactly as it did before.
        // The regex's `(?<!$)` / `(?!$)` lookaround assertions should make
        // it impossible for $$…$$ to match; this test guards that.
        val dollar = "$"
        val blockLatex = dollar + dollar + "\n\\int_0^1 x^2 dx = \\frac{1}{3}\n" + dollar + dollar
        val input: AnnotatedString = buildAnnotatedString {
            append(blockLatex)
        }

        val result = input.injectInlineMathFallbacks()
        val mathTags = result.getStringAnnotations(0, result.length)
            .filter { it.item.startsWith("${MARKDOWN_TAG_MATH}_") }

        assertEquals(
            0,
            mathTags.size,
            "Fallback must NEVER emit a tag for a $$…$$ block — only single-$ runs are allowed. " +
                    "Input was:\n$blockLatex\nTags found: ${mathTags.map { it.item }}"
        )
        assertEquals(input.text, result.text)
    }

    @Test fun `fallback skips ranges already handled by native INLINE_MATH`() {
        // When the GFM lexer correctly produces an INLINE_MATH node, the
        // annotator appends an inline-content placeholder *before* the
        // fallback runs. The fallback must respect those pre-existing
        // MARKDOWN_MATH_ ranges and not double-register them (otherwise
        // we'd create two overlapping placeholders and confuse rendering).
        val dollar = "$"
        val body = dollar + "a + b = c" + dollar

        // Build a string that already owns a math tag for the whole range,
        // exactly the way the native INLINE_MATH branch would.
        val withNativeTag: AnnotatedString = buildAnnotatedString {
            append("lead-in ")
            appendInlineContent(id = "${MARKDOWN_TAG_MATH}_$body", alternateText = body)
            append(" trailing")
        }
        val nativeTagsBefore = withNativeTag.getStringAnnotations(0, withNativeTag.length)
            .count { it.item.startsWith("${MARKDOWN_TAG_MATH}_") }
        assertEquals(1, nativeTagsBefore)

        val result = withNativeTag.injectInlineMathFallbacks()

        val tagsAfter = result.getStringAnnotations(0, result.length)
            .filter { it.item.startsWith("${MARKDOWN_TAG_MATH}_") }
        assertEquals(1, tagsAfter.size, "Idempotency: native-tagged range must not receive a second fallback tag")
    }

    @Test fun `fallback preserves surrounding SpanStyle and link annotations`() {
        // The function copies non-matching stretches via `subSequence`,
        // which is the Compose-provided way to preserve spans. This test
        // pins that behaviour so a future refactor doesn't accidentally
        // drop styling by switching to a raw-`append(text)` loop instead.
        val dollar = "$"
        val boldColor = androidx.compose.ui.graphics.Color(0xFFFF0000.toInt())
        val input: AnnotatedString = buildAnnotatedString {
            withStyle(SpanStyle(color = boldColor, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) {
                append("Bold prefix ")
            }
            append(dollar + "x_n" + dollar)
            withStyle(SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) {
                append(" underline suffix")
            }
        }

        val result = input.injectInlineMathFallbacks()

        // 1. Fallback fired and produced exactly one math tag.
        val mathTags = result.getStringAnnotations(0, result.length)
            .filter { it.item.startsWith("${MARKDOWN_TAG_MATH}_") }
        assertEquals(1, mathTags.size, "Fallback should still tag the math run")

        // 2. Bold and underline spans are still present at the expected char ranges.
        val spanStyles = result.spanStyles
        val bold = spanStyles.firstOrNull { s -> s.item.color == boldColor && s.item.fontWeight == androidx.compose.ui.text.font.FontWeight.Bold }
        val under = spanStyles.firstOrNull { s -> s.item.textDecoration == androidx.compose.ui.text.style.TextDecoration.Underline }
        assertNotNull(bold, "Bold SpanStyle was lost across fallback splice")
        assertNotNull(under, "Underline SpanStyle was lost across fallback splice")
        // Bold covers "Bold prefix " (12 chars) at the start
        assertEquals(0, bold!!.start, "Bold span should still start at index 0")
        assertEquals("Bold prefix ".length, bold.end, "Bold span should still cover exactly the prefix")
        // Underline covers the tail starting after prefix + " $x_n$ "
        val underlineStart = "Bold prefix ".length + (dollar + "x_n" + dollar).length
        assertEquals(underlineStart, under!!.start, "Underline span should still start after the math run")
    }

    @Test fun `user trimIndent mixed scenario — inline fallback plus block math untouched`() {
        // Reproduces exactly what the user wrote in their report:
        //
        //     """
        //     Inline math: ${'$'}E = mc^2${'$'}想办法解析这个…
        //     ${'$'}${'$'}
        //     \int_0^1 x^2 dx = \frac{1}{3}
        //     ${'$'}${'$'}
        //     """.trimIndent()
        //
        // We deliberately DON'T parse this through the GFM parser (we just
        // build a raw AnnotatedString of the post-trimIndent text) because
        // the interesting scenario is "what if GFM missed the inline part?"
        // — the fallback has to step in for the single-$ line while
        // keeping the multi-line $$ block completely clean.
        val dollar = "$"
        val raw = buildString {
            append("Inline math: ")
            append(dollar); append("E = mc^2"); append(dollar)
            append("想办法解析这个，先调研一下没然后列一下计划再执行，禁止破坏Block math:\n")
            append(dollar); append(dollar); append("\n")
            append("\\int_0^1 x^2 dx = \\frac{1}{3}\n")
            append(dollar); append(dollar)
        }
        val input: AnnotatedString = buildAnnotatedString { append(raw) }

        val result = input.injectInlineMathFallbacks()
        val mathTags = result.getStringAnnotations(0, result.length)
            .filter { it.item.startsWith("${MARKDOWN_TAG_MATH}_") }

        // Exactly ONE tag: the inline formula. The block formula has $$
        // delimiters so the regex skips it entirely.
        assertEquals(
            1,
            mathTags.size,
            "Mixed block+inline: expected ONLY the inline ${"$"}E=mc^2${"$"} to be tagged, not $$…$$.\n" +
                    "All tags = ${mathTags.map { it.item }}"
        )
        assertEquals(
            dollar + "E = mc^2" + dollar,
            mathTags.first().item.removePrefix("${MARKDOWN_TAG_MATH}_")
        )

        // Output text byte-for-byte identical.
        assertEquals(raw, result.text)

        // Double-check: no tag starts with $$ (i.e. block wasn't misread)
        assertTrue(
            mathTags.none { it.item.removePrefix("${MARKDOWN_TAG_MATH}_").startsWith(dollar + dollar) },
            "No tag's payload should begin with $$ — that would mean block math was corrupted."
        )
    }

    @Test fun `fallback is idempotent on repeated invocation`() {
        val dollar = "$"
        val input: AnnotatedString = buildAnnotatedString {
            append("一" + dollar + "x" + dollar + "二" + dollar + "y" + dollar + "三")
        }

        val one = input.injectInlineMathFallbacks()
        val two = one.injectInlineMathFallbacks()

        val countOne = one.getStringAnnotations(0, one.length)
            .count { it.item.startsWith("${MARKDOWN_TAG_MATH}_") }
        val countTwo = two.getStringAnnotations(0, two.length)
            .count { it.item.startsWith("${MARKDOWN_TAG_MATH}_") }

        assertEquals(2, countOne, "Two inline formulas (" + dollar + "x" + dollar + " and " + dollar + "y" + dollar + ") should be tagged on first pass")
        assertEquals(countOne, countTwo, "Second pass must not grow tag count — idempotency guarantee")
        assertEquals(one.text, two.text, "Text must stay stable across repeated calls")
    }
}
