package com.ismartcoding.plain.lib.markdown.compose.elements

import androidx.compose.runtime.Composable
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

/**
 * Returns the raw LaTeX source for a `BLOCK_MATH` / `INLINE_MATH` node.
 *
 * The GFM lexer splits the formula into:
 *
 *     DOLLAR  <latex body>  DOLLAR
 *
 * so we strip the leading and trailing `$` and re-emit the body wrapped in
 * the matching delimiter pair (`$$…$$` or `$…$`) — `Latex` recognises both
 * syntaxes natively.
 */
fun ASTNode.mathBody(content: String): String? {
    if (type != GFMElementTypes.BLOCK_MATH && type != GFMElementTypes.INLINE_MATH) {
        return null
    }
    val children = children
    if (children.size < 2) return null
    val first = children.first()
    val last = children.last()
    if (first.type != GFMTokenTypes.DOLLAR || last.type != GFMTokenTypes.DOLLAR) {
        // Fallback: just dump the whole node range, in case the lexer decides
        // to change the wrapping token.
        val start = startOffset
        val end = endOffset
        if (end <= start) return null
        return content.substring(start, end).trim()
    }
    val start = first.endOffset
    val end = last.startOffset
    if (end <= start) return ""
    val body = content.substring(start, end).trim()
    val delimiter = if (type == GFMElementTypes.BLOCK_MATH) "$$" else "$"
    return "$delimiter$body$delimiter"
}

/**
 * Render an `INLINE_MATH` / `BLOCK_MATH` node. No-op when the node does not
 * actually wrap a math block.
 */
@Composable
fun RenderMathNode(content: String, node: ASTNode) {
    val latex = node.mathBody(content) ?: return
    MarkdownMath(latex = latex)
}
