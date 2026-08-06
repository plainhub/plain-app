package com.ismartcoding.plain.lib.markdown.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownColors
import com.ismartcoding.plain.lib.markdown.model.MarkdownTypography
import com.ismartcoding.plain.lib.markdown.model.ReferenceLinkHandler
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.CompositeASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

/**
 * Tag used to indicate an image url for inline content. Required for rendering.
 */
const val MARKDOWN_TAG_IMAGE_URL = "MARKDOWN_IMAGE_URL"

/**
 * Tag used to indicate an inline LaTeX expression for inline content. The
 * payload appended after the prefix is the full LaTeX source (including the
 * `$…$` / `$$…$$` delimiters that the `huarangmeng/latex` renderer
 * recognises natively).
 */
const val MARKDOWN_TAG_MATH = "MARKDOWN_MATH"

/**
 * Resolve the alt text for an `IMAGE` node — used as the contents of the
 * web-style hover tooltip. Falls back through `LINK_TEXT` (inline + full
 * reference forms) and `LINK_LABEL` (short reference form).
 */
internal fun ASTNode.resolveImageAlt(content: String): String? {
    findChildOfTypeRecursive(MarkdownElementTypes.LINK_TEXT)?.let {
        val text = it.getUnescapedTextInNode(content).trim('[', ']').trim()
        if (text.isNotEmpty()) return text
    }
    findChildOfTypeRecursive(MarkdownElementTypes.LINK_LABEL)?.let {
        val text = it.getUnescapedTextInNode(content).trim('[', ']').trim()
        if (text.isNotEmpty()) return text
    }
    return null
}

/**
 * Resolve the destination URL for an `IMAGE` node. Falls back to looking up
 * a `FULL_REFERENCE_LINK` / `SHORT_REFERENCE_LINK` descendant's `LINK_LABEL`
 * via the supplied [referenceLinkHandler] when no inline `LINK_DESTINATION`
 * is present (i.e. for reference-style images like `![alt][id]`).
 */
internal fun ASTNode.resolveImageLink(
    content: String,
    referenceLinkHandler: ReferenceLinkHandler?,
): String? {
    findChildOfTypeRecursive(MarkdownElementTypes.LINK_DESTINATION)?.let {
        return it.getUnescapedTextInNode(content)
    }
    val refNode = findChildOfTypeRecursive(MarkdownElementTypes.FULL_REFERENCE_LINK)
        ?: findChildOfTypeRecursive(MarkdownElementTypes.SHORT_REFERENCE_LINK)
        ?: return null
    val label = refNode.findChildOfType(MarkdownElementTypes.LINK_LABEL)
        ?.getUnescapedTextInNode(content)
        ?: return null
    return referenceLinkHandler?.find(label)?.takeIf { it.isNotEmpty() }
}

/**
 * Find a child node recursive
 */
internal fun ASTNode.findChildOfTypeRecursive(type: IElementType): ASTNode? {
    children.forEach {
        if (it.type == type) {
            return it
        } else {
            val found = it.findChildOfTypeRecursive(type)
            if (found != null) {
                return found
            }
        }
    }
    return null
}

/**
 * Helper function to drop the first and last element in the children list.
 * E.g., we don't want to render the brackets of a link
 */
internal fun List<ASTNode>.innerList(): List<ASTNode> = this.subList(1, this.size - 1)

/**
 * Extension function for `ASTNode` that retrieves the unescaped text within the node.
 *
 * This function first gets the text within the node, then converts it to a string.
 * It then uses `EntityConverter.replaceEntities` to replace any escaped entities
 * in the text, ensuring that the text is properly unescaped.
 *
 * @param allFileText The complete text of the file, which is used to extract the text within the node.
 * @return The unescaped text within the node.
 */
fun ASTNode.getUnescapedTextInNode(allFileText: CharSequence): String {
    val escapedText = getTextInNode(allFileText).toString()
    return EntityConverter.replaceEntities(
        escapedText,
        processEntities = false,
        processEscapes = true
    )
}

/**
 * Extension function to map auto-link nodes to a specified target type.
 *
 * This function iterates over a list of `ASTNode` objects and replaces any
 * auto-link nodes (either `GFM_AUTOLINK` or `AUTOLINK`) with a new `LeafASTNode`
 * of the specified target type. Other nodes remain unchanged.
 *
 * @param targetType The target type to map auto-link nodes to. Defaults to `MarkdownTokenTypes.TEXT`.
 * @return A new list of `ASTNode` objects with auto-link nodes mapped to the target type.
 */
fun List<ASTNode>.mapAutoLinkToType(targetType: IElementType = MarkdownTokenTypes.TEXT): List<ASTNode> {
    return map {
        if (it is LeafASTNode) {
            if (it.type == GFMTokenTypes.GFM_AUTOLINK || it.type == MarkdownElementTypes.AUTOLINK) {
                LeafASTNode(targetType, it.startOffset, it.endOffset)
            } else {
                it
            }
        } else if (it is CompositeASTNode) {
            CompositeASTNode(it.type, it.children.mapAutoLinkToType(targetType))
        } else {
            it
        }
    }
}

/**
 * Helper function to lookup link definitions in the parsed markdown tree.
 */
internal fun lookupLinkDefinition(
    store: MutableMap<String, String?>,
    node: ASTNode,
    content: CharSequence,
    recursive: Boolean = true,
    onlyDefinitions: Boolean = false,
) {
    var linkOnly = false
    val linkLabel = if (node.type == MarkdownElementTypes.LINK_DEFINITION) {
        node.findChildOfType(MarkdownElementTypes.LINK_LABEL)?.getUnescapedTextInNode(content)
    } else if (!onlyDefinitions && node.type == MarkdownElementTypes.INLINE_LINK) {
        node.findChildOfType(MarkdownElementTypes.LINK_TEXT)?.getUnescapedTextInNode(content)
    } else if (!onlyDefinitions && node.type == MarkdownElementTypes.AUTOLINK) {
        linkOnly = true
        (node.children.firstOrNull { it.type.name == MarkdownElementTypes.AUTOLINK.name } ?: node).getUnescapedTextInNode(content)
    } else {
        null
    }

    if (linkLabel != null) {
        val destination = if (linkOnly) {
            linkLabel
        } else {
            node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getUnescapedTextInNode(content)
        }
        store[linkLabel] = destination
    }

    if (recursive) {
        node.children.forEach {
            lookupLinkDefinition(store, it, content)
        }
    }
}


/**
 * Resolves a [TextUnit] to pixels without crashing on non-`sp` units.
 *
 * [TextUnit.toPx] only supports `sp`; calling it on an `em` (or unspecified)
 * value throws. This resolves:
 * - `sp` -> via density
 * - `em` -> [relativeToPx] (the value it is relative to, e.g. the font size) scaled by the em factor
 * - unspecified / unknown -> `0f`
 */
internal fun Density.toPxOrZero(unit: TextUnit, relativeToPx: Float = 0f): Float = when (unit.type) {
    TextUnitType.Sp -> unit.toPx()
    TextUnitType.Em -> unit.value * relativeToPx
    else -> 0f
}

/**
 * Extension property to get the `SpanStyle` for inline code text.
 * This style is defined by the `inlineCode` typography and the current markdown colors.
 */
val MarkdownTypography.codeSpanStyle: SpanStyle
    @Composable
    get() = inlineCode.copy(
        background = LocalMarkdownColors.current.inlineCodeBackground
    ).toSpanStyle()
