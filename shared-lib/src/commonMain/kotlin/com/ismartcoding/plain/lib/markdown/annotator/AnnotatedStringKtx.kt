package com.ismartcoding.plain.lib.markdown.annotator

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import com.ismartcoding.plain.lib.markdown.annotator.appendAutoLink
import com.ismartcoding.plain.lib.markdown.annotator.appendMarkdownLink
import com.ismartcoding.plain.lib.markdown.annotator.appendMarkdownReference
import com.ismartcoding.plain.lib.markdown.annotator.buildMarkdownAnnotatedString
import com.ismartcoding.plain.lib.markdown.model.MarkdownAnnotator
import com.ismartcoding.plain.lib.markdown.model.ReferenceLinkHandler
import com.ismartcoding.plain.lib.markdown.model.markdownAnnotator
import com.ismartcoding.plain.lib.markdown.utils.MARKDOWN_TAG_IMAGE_URL
import com.ismartcoding.plain.lib.markdown.utils.MARKDOWN_TAG_MATH
import com.ismartcoding.plain.lib.markdown.utils.extractFontColor
import com.ismartcoding.plain.lib.markdown.utils.extractHtmlImgAlt
import com.ismartcoding.plain.lib.markdown.utils.extractHtmlImgSrc
import com.ismartcoding.plain.lib.markdown.utils.findChildOfTypeRecursive
import com.ismartcoding.plain.lib.markdown.utils.isFontCloseTag
import com.ismartcoding.plain.lib.markdown.utils.parseHtmlColor
import com.ismartcoding.plain.lib.markdown.utils.getUnescapedTextInNode
import com.ismartcoding.plain.lib.markdown.utils.resolveImageLink
import com.ismartcoding.plain.lib.markdown.utils.innerList
import com.ismartcoding.plain.lib.markdown.utils.mapAutoLinkToType
import com.ismartcoding.plain.lib.markdown.compose.elements.mathBody
import com.ismartcoding.plain.lib.markdown.utils.extractFontColor
import com.ismartcoding.plain.lib.markdown.utils.extractHtmlImgAlt
import com.ismartcoding.plain.lib.markdown.utils.extractHtmlImgSrc
import com.ismartcoding.plain.lib.markdown.utils.getUnescapedTextInNode
import com.ismartcoding.plain.lib.markdown.utils.innerList
import com.ismartcoding.plain.lib.markdown.utils.isFontCloseTag
import com.ismartcoding.plain.lib.markdown.utils.mapAutoLinkToType
import com.ismartcoding.plain.lib.markdown.utils.resolveImageLink
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

/**
 * Extension function to build an `AnnotatedString` from a Markdown string.
 * This function will parse the Markdown content and apply the given styles to the text.
 *
 * It only supports TEXT and PARAGRAPH nodes.
 *
 * @param style The base text style to apply.
 * @param linkTextSpanStyle The style to apply to link text.
 * @param codeSpanStyle The style to apply to code spans.
 * @param flavour The Markdown flavour descriptor to use (default is GFM).
 * @param annotator An optional annotator for additional processing.
 * @return The constructed `AnnotatedString`.
 */
@Deprecated(
    message = "This function is deprecated. Use the new `annotatorSettings` function to create a settings object.",
    replaceWith = ReplaceWith("buildMarkdownAnnotatedString(style, annotatorSettings, flavour)")
)
fun String.buildMarkdownAnnotatedString(
    style: TextStyle,
    linkTextSpanStyle: SpanStyle = style.toSpanStyle(),
    codeSpanStyle: SpanStyle = style.toSpanStyle(),
    flavour: MarkdownFlavourDescriptor = GFMFlavourDescriptor(),
    annotator: com.ismartcoding.plain.lib.markdown.model.MarkdownAnnotator = markdownAnnotator(),
    referenceLinkHandler: com.ismartcoding.plain.lib.markdown.model.ReferenceLinkHandler? = null,
    linkInteractionListener: LinkInteractionListener? = null,
) = buildMarkdownAnnotatedString(
    style = style,
    annotatorSettings = DefaultAnnotatorSettings(
        linkTextSpanStyle = TextLinkStyles(style = linkTextSpanStyle),
        codeSpanStyle = codeSpanStyle,
        annotator = annotator,
        referenceLinkHandler = referenceLinkHandler,
        linkInteractionListener = linkInteractionListener
    ),
    flavour = flavour
)

/**
 * Extension function to build an `AnnotatedString` from a Markdown string.
 * This function will parse the Markdown content and apply the given styles to the text.
 *
 * It only supports TEXT and PARAGRAPH nodes.
 *
 * @param style The base text style to apply.
 * @param flavour The Markdown flavour descriptor to use (default is GFM).
 * @param annotatorSettings Settings object to adjust different behavior of this annotated string.
 * @return The constructed `AnnotatedString`.
 */
fun String.buildMarkdownAnnotatedString(
    style: TextStyle,
    annotatorSettings: com.ismartcoding.plain.lib.markdown.annotator.AnnotatorSettings,
    flavour: MarkdownFlavourDescriptor = GFMFlavourDescriptor(),
): AnnotatedString {
    val content = this
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(content)
    val textNode = parsedTree.children.firstOrNull { node ->
        node.type == MarkdownTokenTypes.TEXT || node.type == MarkdownElementTypes.PARAGRAPH
    }
    if (textNode == null) return buildAnnotatedString { }
    return content.buildMarkdownAnnotatedString(
        textNode = textNode,
        style = style,
        annotatorSettings = annotatorSettings
    )
}

/**
 * Extension function to build an `AnnotatedString` from a Markdown string.
 * This function will parse the Markdown content and apply the given styles to the text.
 *
 * It only supports TEXT and PARAGRAPH nodes.
 *
 * @param textNode The AST node representing the text.
 * @param style The base text style to apply.
 * @param annotatorSettings Settings object to adjust different behavior of this annotated string.
 * @return The constructed `AnnotatedString`.
 */
fun String.buildMarkdownAnnotatedString(
    textNode: ASTNode,
    style: TextStyle,
    annotatorSettings: com.ismartcoding.plain.lib.markdown.annotator.AnnotatorSettings,
): AnnotatedString = buildAnnotatedString {
    pushStyle(style.toSpanStyle())
    buildMarkdownAnnotatedString(this@buildMarkdownAnnotatedString, textNode, annotatorSettings)
    pop()
}

/**
 * Appends a Markdown link to the `AnnotatedString.Builder`.
 *
 * @param content The content string.
 * @param node The AST node representing the link.
 * @param annotatorSettings Settings object to adjust different behavior of this annotated string.
 */
fun AnnotatedString.Builder.appendMarkdownLink(
    content: String,
    node: ASTNode,
    annotatorSettings: com.ismartcoding.plain.lib.markdown.annotator.AnnotatorSettings,
) {
    val linkText = node.findChildOfType(MarkdownElementTypes.LINK_TEXT)?.children?.innerList()
    if (linkText == null) {
        append(node.getUnescapedTextInNode(content))
        return
    }
    val text = linkText.firstOrNull()?.getUnescapedTextInNode(content)
    val destination = node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getUnescapedTextInNode(content)
    val linkLabel = node.findChildOfType(MarkdownElementTypes.LINK_LABEL)?.getUnescapedTextInNode(content)
    val annotation = destination ?: linkLabel

    if (annotation != null) {
        if (text != null) annotatorSettings.referenceLinkHandler?.store(text, annotation)
        withLink(LinkAnnotation.Url(annotation, annotatorSettings.linkTextSpanStyle, annotatorSettings.linkInteractionListener)) {
            buildMarkdownAnnotatedString(content, linkText.mapAutoLinkToType(), annotatorSettings)
        }
    } else {
        buildMarkdownAnnotatedString(content, linkText, annotatorSettings)
    }
}

/**
 * Appends a Markdown reference to the `AnnotatedString.Builder`.
 *
 * @param content The content string.
 * @param node The AST node representing the link.
 * @param annotatorSettings Settings object to adjust different behavior of this annotated string.
 */
fun AnnotatedString.Builder.appendMarkdownReference(
    content: String,
    node: ASTNode,
    annotatorSettings: com.ismartcoding.plain.lib.markdown.annotator.AnnotatorSettings,
) {
    val full = node.type == MarkdownElementTypes.FULL_REFERENCE_LINK
    val labelNode = node.findChildOfType(MarkdownElementTypes.LINK_LABEL)
    val linkText = if (full) {
        node.findChildOfType(MarkdownElementTypes.LINK_TEXT)?.children?.innerList()
    } else {
        labelNode?.children?.innerList()
    }

    if (linkText == null || labelNode == null) {
        append(node.getUnescapedTextInNode(content))
        return
    }

    val label = labelNode.getUnescapedTextInNode(content)
    val url = annotatorSettings.referenceLinkHandler?.find(label)?.takeIf { it.isNotEmpty() }

    if (url != null) {
        withLink(LinkAnnotation.Url(url, annotatorSettings.linkTextSpanStyle, annotatorSettings.linkInteractionListener)) {
            buildMarkdownAnnotatedString(content, linkText.mapAutoLinkToType(), annotatorSettings)
        }
    } else {
        // if no reference is found, reference links are rendered as their individual components
        val linkText = node.findChildOfType(MarkdownElementTypes.LINK_TEXT)
        if (linkText != null) {
            buildMarkdownAnnotatedString(content, linkText, annotatorSettings)
        }
        buildMarkdownAnnotatedString(content, labelNode, annotatorSettings)
    }
}

/**
 * Appends an auto-detected link to the `AnnotatedString.Builder`.
 *
 * @param content The content string.
 * @param node The AST node representing the auto link.
 * @param annotatorSettings The style to apply to the link text.
 */
fun AnnotatedString.Builder.appendAutoLink(
    content: String,
    node: ASTNode,
    annotatorSettings: com.ismartcoding.plain.lib.markdown.annotator.AnnotatorSettings,
) {
    val targetNode = node.children.firstOrNull {
        it.type.name == MarkdownElementTypes.AUTOLINK.name
    } ?: node
    val destination = targetNode.getUnescapedTextInNode(content)

    annotatorSettings.referenceLinkHandler?.store(destination, destination)
    withLink(LinkAnnotation.Url(destination, annotatorSettings.linkTextSpanStyle, linkInteractionListener = annotatorSettings.linkInteractionListener)) {
        append(destination)
    }
}

/**
 * Builds an [AnnotatedString] with the contents of the given Markdown [ASTNode] node.
 *
 * This method automatically constructs the string with child components like:
 * - Paragraph
 * - Image
 * - Strong
 * - ...
 */
fun AnnotatedString.Builder.buildMarkdownAnnotatedString(
    content: String,
    node: ASTNode,
    annotatorSettings: com.ismartcoding.plain.lib.markdown.annotator.AnnotatorSettings,
) = buildMarkdownAnnotatedString(
    content = content,
    children = node.children,
    annotatorSettings = annotatorSettings,
)

/**
 * Builds an [AnnotatedString] with the contents of the given Markdown [ASTNode] node.
 *
 * This method automatically constructs the string with child components like:
 * - Paragraph
 * - Image
 * - Strong
 * - ...
 */
fun AnnotatedString.Builder.buildMarkdownAnnotatedString(
    content: String,
    children: List<ASTNode>,
    annotatorSettings: com.ismartcoding.plain.lib.markdown.annotator.AnnotatorSettings,
) {
    val annotate = annotatorSettings.annotator.annotate
    val eolAsNewLine = annotatorSettings.annotator.config.eolAsNewLine
    var skipIfNext: Any? = null
    // Tracks unclosed `<font color="…">` pushes so we can balance the
    // `AnnotatedString.Builder` style stack even when the closing `</font>`
    // is missing (user-authored content). Each push must be matched by a pop.
    var fontOpenCount = 0
    children.forEach { child ->
        if (skipIfNext == null || skipIfNext != child.type) {
            if (annotate == null || !annotate(content, child)) {
                val parentType = child.parent?.type

                when (child.type) {
                    // Element types
                    MarkdownElementTypes.PARAGRAPH -> buildMarkdownAnnotatedString(content = content, node = child, annotatorSettings = annotatorSettings)

                    MarkdownElementTypes.IMAGE -> child.resolveImageLink(content, annotatorSettings.referenceLinkHandler)?.let { imageUrl ->
                        appendInlineContent("${MARKDOWN_TAG_IMAGE_URL}_$imageUrl", imageUrl)
                    }

                    // Inline `$…$` math nodes nested inside a paragraph. These
                    // are small enough to live in the inline flow, so route
                    // them through an inline-content placeholder that
                    // `MarkdownText` will swap for `MarkdownMath` at draw time.
                    //
                    // `BLOCK_MATH` is intentionally NOT handled here — the
                    // `PARAGRAPH` dispatcher in `MarkdownExtension.kt` detects
                    // paragraph children that contain `BLOCK_MATH` and splits
                    // the paragraph so the math escapes the inline flow
                    // entirely (rendered as a full-width block via
                    // `RenderMathNode`).
                    GFMElementTypes.INLINE_MATH -> {
                        val latex = child.mathBody(content)
                        if (!latex.isNullOrEmpty()) {
                            appendInlineContent("${MARKDOWN_TAG_MATH}_$latex", latex)
                        }
                    }

                    // Inline `<img src="..." />` HTML tags reach us as `HTML_TAG` tokens
                    // nested inside a paragraph. Without this branch the tag is silently
                    // dropped by the `else` fallback below, and `app://` / `fid:` /
                    // https references typed as raw HTML never make it to the image
                    // pipeline. Route them through the same inline-content placeholder
                    // used by `MarkdownElementTypes.IMAGE` so `MarkdownInlineImageWithSize`
                    // picks them up unchanged.
                    //
                    // `<font color="…">…</font>` tags (emitted by the editor colour
                    // picker) are handled here too: the opening tag pushes a
                    // `SpanStyle(color = …)` and the closing tag pops it, so the text
                    // between inherits the colour. The KMP migration dropped the old
                    // Markwon `FontTagHandler`; this restores that behaviour.
                    MarkdownTokenTypes.HTML_TAG -> {
                        val src = child.extractHtmlImgSrc(content)
                        if (!src.isNullOrEmpty()) {
                            // Carry the alt text through `alternateText` so the inline
                            // renderer can use it for `contentDescription` without a
                            // second regex pass.
                            val alt = child.extractHtmlImgAlt(content) ?: src
                            appendInlineContent("${MARKDOWN_TAG_IMAGE_URL}_$src", alt)
                        } else {
                            val fontColor = child.extractFontColor(content)
                            if (fontColor != null) {
                                val color = parseHtmlColor(fontColor)
                                if (color != null) {
                                    pushStyle(SpanStyle(color = color))
                                    fontOpenCount++
                                }
                            } else if (child.isFontCloseTag(content) && fontOpenCount > 0) {
                                pop()
                                fontOpenCount--
                            }
                        }
                    }

                    MarkdownElementTypes.EMPH -> {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        buildMarkdownAnnotatedString(content, child, annotatorSettings)
                        pop()
                    }

                    MarkdownElementTypes.STRONG -> {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        buildMarkdownAnnotatedString(content, child, annotatorSettings)
                        pop()
                    }

                    GFMElementTypes.STRIKETHROUGH -> {
                        pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                        buildMarkdownAnnotatedString(content, child, annotatorSettings)
                        pop()
                    }

                    MarkdownElementTypes.CODE_SPAN -> {
                        append(' ')
                        pushStyle(annotatorSettings.codeSpanStyle)
                        buildMarkdownAnnotatedString(content, child.children.innerList(), annotatorSettings)
                        pop()
                        append(' ')
                    }

                    MarkdownElementTypes.AUTOLINK -> appendAutoLink(content, child, annotatorSettings)
                    MarkdownElementTypes.INLINE_LINK -> appendMarkdownLink(content, child, annotatorSettings)
                    MarkdownElementTypes.SHORT_REFERENCE_LINK -> appendMarkdownReference(content, child, annotatorSettings)
                    MarkdownElementTypes.FULL_REFERENCE_LINK -> appendMarkdownReference(content, child, annotatorSettings)

                    // Token Types
                    MarkdownTokenTypes.TEXT -> append(child.getUnescapedTextInNode(content))
                    GFMTokenTypes.GFM_AUTOLINK -> if (child.parent == MarkdownElementTypes.LINK_TEXT) {
                        append(child.getUnescapedTextInNode(content))
                    } else appendAutoLink(content, child, annotatorSettings)

                    GFMTokenTypes.DOLLAR -> append('$')

                    MarkdownTokenTypes.SINGLE_QUOTE -> append('\'')
                    MarkdownTokenTypes.DOUBLE_QUOTE -> append('\"')
                    MarkdownTokenTypes.LPAREN -> append('(')
                    MarkdownTokenTypes.RPAREN -> append(')')
                    MarkdownTokenTypes.LBRACKET -> append('[')
                    MarkdownTokenTypes.RBRACKET -> append(']')
                    MarkdownTokenTypes.LT -> append('<')
                    MarkdownTokenTypes.GT -> append('>')
                    MarkdownTokenTypes.COLON -> append(':')
                    MarkdownTokenTypes.EXCLAMATION_MARK -> append('!')
                    MarkdownTokenTypes.BACKTICK -> append('`')
                    MarkdownTokenTypes.HARD_LINE_BREAK -> {
                        append('\n')
                        skipIfNext = MarkdownTokenTypes.EOL
                    }

                    MarkdownTokenTypes.EMPH -> {
                        if (parentType != MarkdownElementTypes.EMPH && parentType != MarkdownElementTypes.STRONG) {
                            append(child.getTextInNode(content))
                        }
                    }

                    MarkdownTokenTypes.EOL -> if (eolAsNewLine) append('\n') else append(' ')
                    MarkdownTokenTypes.WHITE_SPACE -> if (length > 0) append(' ')
                    MarkdownTokenTypes.BLOCK_QUOTE -> {
                        skipIfNext = MarkdownTokenTypes.WHITE_SPACE
                    }

                    else -> {
                        // `~` is not a specific `MarkdownTokenTypes`
                        if (child.type.name == "~" && parentType != GFMElementTypes.STRIKETHROUGH) {
                            append(child.getTextInNode(content))
                        }
                    }
                }
            }
        } else {
            skipIfNext = null
        }
    }
    // Balance any unclosed `<font>` pushes so the builder's push/pop stack
    // stays consistent and `buildAnnotatedString` doesn't throw on malformed
    // user-authored content (e.g. a missing `</font>`).
    repeat(fontOpenCount) { pop() }
}

// ── Inline-math fallback extractor ──────────────────────────────────────

/**
 * Regex that matches a single `$…$` inline math run while deliberately
 * rejecting `$$…$$` block math. The surrounding lookaround assertions
 * (`(?<!\$)` / `(?!\$)`) require the opening `$` not to be preceded by
 * another `$` and the closing `$` not to be followed by another `$`,
 * which means:
 *
 *  - `$E = mc^2$`       → MATCHES (inline math)
 *  - `$$\int_0^1 dx$$`  → never matches, no matter how it is split
 *                        across lines or wrapped in spaces
 *  - `price $100`       → no match (lone `$`, needs a pair)
 *  - `$a$b$c$`          → only `$a$` and `$c$` match; `b` is plain text
 *
 * The body character class `[^$\n]+?` forbids literal `$` *inside* the
 * formula so we don't swallow a later block-math opening delimiter,
 * and forbids `\n` so a run cannot span paragraphs (matching what the
 * GFM lexer would do for inline math). The `+?` non-greedy quantifier
 * stops at the *first* closing `$` it finds, keeping each run tight.
 *
 * This is intentionally a *fallback*: the GFM lexer (via
 * `GFMElementTypes.INLINE_MATH`) already handles whitespace-separated
 * cases natively. We only reach this codepath when the lexer produced
 * plain `TEXT` / `DOLLAR` tokens instead — e.g. for CJK-adjacent runs
 * written without a separating space, for leading/trailing punctuation,
 * or after `.trimIndent()` has collapsed the boundary between a
 * paragraph label and a `$`-prefixed formula.
 */
private val INLINE_MATH_FALLBACK_REGEX: Regex by lazy {
    Regex("""(?<!\$)\$([^$\n]+?)\$(?!\$)""")
}

/**
 * Return a copy of the receiver with any raw `$…$` runs that the GFM
 * lexer missed promoted to inline-content placeholders.
 *
 * Behavior guarantees (this is what keeps the change safe):
 *
 *  1. **BLOCK_MATH is never touched.** The regex rejects runs whose
 *     delimiter is `$$`, and we additionally skip any character range
 *     that already carries a `MARKDOWN_MATH_` annotation (which is how
 *     the native INLINE_MATH / BLOCK_MATH paths book-keep their
 *     placeholders). So a paragraph that the splitter in
 *     `MarkdownExtension.kt` has already carved up keeps its block
 *     math exactly as it was.
 *  2. **Existing spans are preserved.** For every character range that
 *     is *not* a fallback match we copy the corresponding
 *     `AnnotatedString.subSequence`, which carries forward every
 *     `SpanStyle`, `LinkAnnotation`, `StringAnnotation`, etc. that the
 *     caller had already pushed. This means bold / italic / link /
 *     strikethrough / `<font color>` styling all remains intact across
 *     the fallback boundary.
 *  3. **Idempotent.** Calling the function twice is a no-op on the
 *     second pass, because the first pass converts every `$…$` run
 *     into an inline-content placeholder (annotated with
 *     `MARKDOWN_MATH_`) and the second pass skips those annotated
 *     ranges.
 *
 * Callers should invoke this after the main annotator loop has run
 * (i.e. on a fully-populated `AnnotatedString`) and before
 * `buildMathInlineContent` scans for `MARKDOWN_MATH_` tags, so the
 * newly-emitted placeholders are picked up in the same map-building
 * step. In the current architecture that invocation point is inside
 * `MarkdownText`'s `derivedStateOf` block.
 */
fun AnnotatedString.injectInlineMathFallbacks(): AnnotatedString {
    val original = this@injectInlineMathFallbacks
    val originalLength = original.length
    if (originalLength == 0) return this

    // 1. Discover character ranges that are already claimed by a
    //    MARKDOWN_MATH_ annotation (native INLINE_MATH / any future
    //    extension). We will not inject a fallback inside those ranges
    //    because the native path already owns the content there.
    val nativeRanges: List<IntRange> = original.getStringAnnotations(0, originalLength)
        .filter { it.item.startsWith("${MARKDOWN_TAG_MATH}_") }
        .map { it.start until it.end }
        .sortedBy { it.first }

    fun isRangeNative(start: Int, endExclusive: Int): Boolean {
        // `endExclusive` is the index *after* the last character of the
        // match, which matches how `IntRange` is constructed below.
        for (r in nativeRanges) {
            if (start < r.last + 1 && endExclusive > r.first) return true
        }
        return false
    }

    // 2. Find all candidate fallback matches. We run the regex *against
    //    the final text* the annotator emitted, not the original markdown
    //    source, because entity replacement / link-unrolling may have
    //    shifted character offsets relative to the input file. Matching
    //    on the built text guarantees the subSequence copies below use
    //    consistent indices.
    val matches = INLINE_MATH_FALLBACK_REGEX.findAll(original.text).toList()
    if (matches.isEmpty() && nativeRanges.isEmpty()) return this

    // 3. Splice the result: non-matching stretches come from
    //    `subSequence` (preserving styles/annotations verbatim), and
    //    each fallback match is rewritten as the same inline-content
    //    triple the native INLINE_MATH branch would have emitted:
    //
    //        id        = MARKDOWN_MATH_<latex with delimiters>
    //        alternate = latex  (what the renderer shows if Latex fails)
    //
    //    The outer `$` delimiters are preserved so `MarkdownMath`'s
    //    `startsWith("$$")` / single-`$` detector keeps working — it
    //    treats the payload exactly like a natively-parsed formula.
    val out = androidx.compose.ui.text.buildAnnotatedString {
        var cursor = 0
        for (m in matches) {
            val start = m.range.first
            val end = m.range.last + 1  // exclusive
            if (isRangeNative(start, end)) continue
            if (start > cursor) {
                append(original.subSequence(cursor, start))
            }
            val latexWithDelimiters = m.value  // already `$body$`
            appendInlineContent(
                id = "${MARKDOWN_TAG_MATH}_$latexWithDelimiters",
                alternateText = latexWithDelimiters,
            )
            cursor = end
        }
        // NOTE: explicitly use `originalLength` here, NOT the builder's
        // ever-growing `length` property — in Compose's
        // `buildAnnotatedString { }` lambda, `length` resolves to the
        // builder's current length, NOT the input's length. Using the
        // builder's length here (before the tail is appended) makes the
        // comparison false when `cursor` exactly equals the builder's
        // size, which silently drops any post-match content like CJK
        // suffix text (this was Bug C).
        if (cursor < originalLength) {
            append(original.subSequence(cursor, originalLength))
        }
    }

    // 4. We always return `out`: even if `out.length == originalLength`
    //    (which is the typical case — alternateText equals the matched
    //    `$…$` run), the annotations differ so identity-equality isn't
    //    useful. Fast-pathing `return this` only happened when there
    //    were zero matches AND zero native tags — that case was handled
    //    at the top of the function.
    return out
}
