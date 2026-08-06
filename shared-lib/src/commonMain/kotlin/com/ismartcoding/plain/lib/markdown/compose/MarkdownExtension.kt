package com.ismartcoding.plain.lib.markdown.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ismartcoding.plain.lib.markdown.compose.components.MarkdownComponentModel
import com.ismartcoding.plain.lib.markdown.compose.components.MarkdownComponents
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownImage
import com.ismartcoding.plain.lib.markdown.compose.elements.MarkdownParagraph
import com.ismartcoding.plain.lib.markdown.compose.elements.RenderMathNode
import com.ismartcoding.plain.lib.markdown.model.MarkdownTypography
import com.ismartcoding.plain.lib.markdown.utils.extractHtmlImgAlt
import com.ismartcoding.plain.lib.markdown.utils.extractHtmlImgSrc
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownElementTypes.ATX_1
import org.intellij.markdown.MarkdownElementTypes.ATX_2
import org.intellij.markdown.MarkdownElementTypes.ATX_3
import org.intellij.markdown.MarkdownElementTypes.ATX_4
import org.intellij.markdown.MarkdownElementTypes.ATX_5
import org.intellij.markdown.MarkdownElementTypes.ATX_6
import org.intellij.markdown.MarkdownElementTypes.BLOCK_QUOTE
import org.intellij.markdown.MarkdownElementTypes.CODE_BLOCK
import org.intellij.markdown.MarkdownElementTypes.CODE_FENCE
import org.intellij.markdown.MarkdownElementTypes.IMAGE
import org.intellij.markdown.MarkdownElementTypes.ORDERED_LIST
import org.intellij.markdown.MarkdownElementTypes.PARAGRAPH
import org.intellij.markdown.MarkdownElementTypes.SETEXT_1
import org.intellij.markdown.MarkdownElementTypes.SETEXT_2
import org.intellij.markdown.MarkdownElementTypes.UNORDERED_LIST
import org.intellij.markdown.MarkdownTokenTypes.Companion.EOL
import org.intellij.markdown.MarkdownTokenTypes.Companion.HORIZONTAL_RULE
import org.intellij.markdown.MarkdownTokenTypes.Companion.HTML_TAG
import org.intellij.markdown.MarkdownTokenTypes.Companion.TEXT
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMElementTypes.TABLE
import org.intellij.markdown.ast.getTextInNode

/**
 * Handles the rendering of a markdown element based on its [ASTNode.type].
 *
 * This function is responsible for determining the appropriate component to use for rendering
 * It does handle rendering of children recursively.
 *
 * @param node The ASTNode representing the markdown element.
 * @param components The [MarkdownComponents] instance containing the components to use.
 * @param content The original markdown content string.
 * @param includeSpacer Whether to include a spacer before rendering the element.
 */
@Composable
fun MarkdownElement(
    node: ASTNode,
    components: MarkdownComponents,
    content: String,
    includeSpacer: Boolean = true,
) = MarkdownElementInternal(node, components, content, includeSpacer)

@Composable
internal fun MarkdownElementInternal(
    node: ASTNode,
    components: MarkdownComponents,
    content: CharSequence,
    includeSpacer: Boolean = true,
) {
    val typography = LocalMarkdownTypography.current
    val model = remember(node, content, typography) {
        // It's safe to pass `CharSequence` and its `toString` here.
        // Reason: It's guaranteed that even the source `StringBuilder` changes, The render result is not dirty.
        // So it's fine to remember it.
        MarkdownComponentModel(
            content = content.toString(),
            node = node,
            typography = typography,
        )
    }
    var handled = true
    if (includeSpacer) Spacer(Modifier.height(LocalMarkdownPadding.current.block))
    when (node.type) {
        TEXT -> components.text(model)
        EOL -> components.eol(model)
        CODE_FENCE -> components.codeFence(model)
        CODE_BLOCK -> components.codeBlock(model)
        ATX_1 -> components.heading1(model)
        ATX_2 -> components.heading2(model)
        ATX_3 -> components.heading3(model)
        ATX_4 -> components.heading4(model)
        ATX_5 -> components.heading5(model)
        ATX_6 -> components.heading6(model)
        SETEXT_1 -> components.setextHeading1(model)
        SETEXT_2 -> components.setextHeading2(model)
        BLOCK_QUOTE -> components.blockQuote(model)
        PARAGRAPH -> {
            // GFM tokenises a paragraph that contains a `$$…$$` block-math
            // expression as a `PARAGRAPH` node whose children include the
            // `BLOCK_MATH` element. The default `components.paragraph`
            // renders the children through `buildMarkdownAnnotatedString`,
            // which routes the math through an `InlineTextContent`
            // placeholder — but the `huarangmeng/latex` renderer ignores
            // parent layout constraints and pins its canvas to the formula's
            // natural width, so the placeholder ends up clipping the
            // rendered equation (right side chopped off).
            //
            // Inline images have the same problem from a different angle:
            // they also flow through the same `InlineTextContent`
            // placeholder system, but `BasicText` consumes every pointer
            // event for its own text-selection / LinkAnnotation logic, so
            // any `Modifier.clickable` attached to the inline image's
            // `Image` composable never receives the tap. That meant the
            // tap-to-open-preview behaviour only worked for block-level
            // (`![]()`) images.
            //
            // To fix both, when a paragraph contains a `BLOCK_MATH`,
            // `IMAGE`, or HTML `<img>` child we split the paragraph into
            // runs: leading text, the element (rendered as a block), then
            // trailing text. The surrounding runs are short paragraphs
            // that fit cleanly on the text baseline. We mark
            // `handled = true` so the post-branch
            // `forEach { MarkdownElementInternal(child, ...) }` does NOT
            // re-render the same children.
            if (hasEscapedBlockChild(node.children, model.content)) {
                handled = true
                renderParagraphWithEscapedBlocks(node, components, model.content, includeSpacer)
            } else {
                components.paragraph(model)
            }
        }
        ORDERED_LIST -> components.orderedList(model)
        UNORDERED_LIST -> components.unorderedList(model)
        IMAGE -> components.image(model)
        HORIZONTAL_RULE -> components.horizontalRule(model)
        TABLE -> components.table(model)
        HTML_TAG -> {
            // Jetbrains-markdown tokenizes inline HTML tags (including `<img src="..." />`)
            // as a single `HTML_TAG` token, but the base renderer only handles markdown
            // elements. Without this branch the tag is silently dropped (the `else` path
            // treats the custom callback's `Unit` return as "handled" and short-circuits
            // child traversal). Render `<img>` through the same MarkdownImage pipeline as
            // the markdown image syntax so existing `app://` / `fid:` resolution keeps
            // working unchanged.
            //
            // Note: only top-level HTML tags reach this branch. Inline `<img>` tags
            // nested inside a paragraph are handled by
            // [buildMarkdownAnnotatedString] in the annotator package, which routes
            // them through the inline-content placeholder system.
            val src = node.extractHtmlImgSrc(model.content)
            if (!src.isNullOrEmpty()) {
                val alt = node.extractHtmlImgAlt(model.content)
                MarkdownImage(
                    content = model.content,
                    node = node,
                    altOverride = alt,
                    linkOverride = src,
                )
            }
        }
        else -> {
            handled = components.custom?.invoke(node.type, model) != null
        }
    }

    if (!handled) {
        node.children.forEach { child ->
            MarkdownElementInternal(child, components, content, includeSpacer)
        }
    }
}

/**
 * Returns true if the children of this paragraph contain any element
 * that must escape the inline flow — `BLOCK_MATH`, markdown `IMAGE`,
 * or HTML `<img>` tags. The check needs the source [content] because
 * `<img>` is tokenised as a generic `HTML_TAG` token and we have to
 * sniff its raw text.
 */
private fun hasEscapedBlockChild(children: List<ASTNode>, content: String): Boolean {
    return children.any { child ->
        child.type == GFMElementTypes.BLOCK_MATH ||
            child.type == MarkdownElementTypes.IMAGE ||
            (child.type == HTML_TAG && child.isHtmlImg(content))
    }
}

/**
 * Render a paragraph that contains at least one child that wants to
 * "escape" the inline flow — currently `BLOCK_MATH`, markdown `IMAGE`,
 * and HTML `<img>` tags. We split the children at every such boundary
 * and emit alternating runs of text (rendered through the annotator's
 * `Builder.buildMarkdownAnnotatedString` overload that accepts a
 * children list) and the escaped element rendered as a block.
 *
 * Block math wants the full container width, which `InlineTextContent`
 * cannot provide. Inline images technically fit in an `InlineTextContent`
 * placeholder, but `BasicText` consumes every pointer event for its own
 * text-selection / LinkAnnotation logic, so any `Modifier.clickable`
 * attached to the placeholder's child composable never receives the tap.
 * Both classes of element therefore have to escape the inline flow and
 * render as standalone blocks.
 */
@Composable
private fun renderParagraphWithEscapedBlocks(
    node: ASTNode,
    components: MarkdownComponents,
    content: String,
    includeSpacer: Boolean,
) {
    if (includeSpacer) Spacer(Modifier.height(LocalMarkdownPadding.current.block))
    val typography = LocalMarkdownTypography.current
    val children = node.children
    // `pending` is rebuilt every recomposition via `remember(node, content)`,
    // so the column body below does not need to track it across calls.
    val pending = remember(node, content) { mutableListOf<ASTNode>() }
    pending.clear()
    Column {
        for (child in children) {
            when {
                child.type == GFMElementTypes.BLOCK_MATH -> {
                    flushRun(pending, node, content, typography)
                    RenderMathNode(content, child)
                }
                child.type == MarkdownElementTypes.IMAGE -> {
                    flushRun(pending, node, content, typography)
                    MarkdownImage(content = content, node = child)
                }
                child.type == HTML_TAG && child.isHtmlImg(content) -> {
                    flushRun(pending, node, content, typography)
                    val src = child.extractHtmlImgSrc(content) ?: continue
                    val alt = child.extractHtmlImgAlt(content)
                    MarkdownImage(
                        content = content,
                        node = child,
                        altOverride = alt,
                        linkOverride = src,
                    )
                }
                else -> pending += child
            }
        }
        flushRun(pending, node, content, typography)
    }
}

/**
 * Returns true if [content] at the range covered by this `HTML_TAG`
 * node looks like an `<img ...>` element. We use the same regex the
 * annotator uses to extract `src` / `alt`, so the result is consistent
 * with what `AnnotatedStringKtx` would have routed into the inline-image
 * placeholder.
 */
private fun ASTNode.isHtmlImg(content: String): Boolean {
    return getTextInNode(content).toString().trimStart().startsWith("<img", ignoreCase = true)
}

@Composable
private fun flushRun(
    pending: MutableList<ASTNode>,
    parent: ASTNode,
    content: String,
    typography: com.ismartcoding.plain.lib.markdown.model.MarkdownTypography,
) {
    if (pending.isEmpty()) return
    // `MarkdownParagraph` walks `node.children`; we need to feed it
    // the buffered subset only. We can't mutate the original
    // `node`'s `children` (it is an immutable `val`), so we wrap
    // the buffered nodes in a lightweight `ASTNode` that exposes
    // them as its children.
    val first = pending.first()
    val last = pending.last()
    val wrapper = object : org.intellij.markdown.ast.ASTNode {
        override val type = PARAGRAPH
        override val startOffset = first.startOffset
        override val endOffset = last.endOffset
        override val parent = parent
        override val children: List<org.intellij.markdown.ast.ASTNode> = pending.toList()
    }
    MarkdownParagraph(content = content, node = wrapper, style = typography.paragraph)
    pending.clear()
}
