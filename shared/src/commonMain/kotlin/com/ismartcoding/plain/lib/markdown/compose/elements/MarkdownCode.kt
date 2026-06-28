package com.ismartcoding.plain.lib.markdown.compose.elements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownA11yLabels
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownColors
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownDimens
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownPadding
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownTypography
import com.ismartcoding.plain.lib.markdown.compose.elements.material.MarkdownBasicText
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode

@Composable
private fun MarkdownCode(
    code: String,
    language: String? = null,
    style: TextStyle = LocalMarkdownTypography.current.code,
    showHeader: Boolean = false,
) {
    val backgroundCodeColor = LocalMarkdownColors.current.codeBackground
    val codeBackgroundCornerSize = LocalMarkdownDimens.current.codeBackgroundCornerSize
    val codeBlockPadding = LocalMarkdownPadding.current.codeBlock
    MarkdownCodeBackground(
        color = backgroundCodeColor,
        shape = RoundedCornerShape(codeBackgroundCornerSize),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        showHeader = showHeader,
        language = language,
        code = code
    ) {
        MarkdownBasicText(
            text = code,
            style = style,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(codeBlockPadding),
        )
    }
}

@Composable
fun MarkdownCodeFence(
    content: String,
    node: ASTNode,
    style: TextStyle = LocalMarkdownTypography.current.code,
    block: @Composable (String, String?, TextStyle) -> Unit = { code, language, style -> MarkdownCode(code = code, language = language, style = style) },
) {
    // CODE_FENCE_START, FENCE_LANG, EOL, {content // CODE_FENCE_CONTENT // x-times}, CODE_FENCE_END
    // CODE_FENCE_START, EOL, {content // CODE_FENCE_CONTENT // x-times}, EOL
    // CODE_FENCE_START, EOL, {content // CODE_FENCE_CONTENT // x-times}
    // CODE_FENCE_START, FENCE_LANG, EOL, {content // CODE_FENCE_CONTENT // x-times}

    val language = node.findChildOfType(MarkdownTokenTypes.FENCE_LANG)?.getTextInNode(content)?.toString()
    if (node.children.size >= 3) {
        val start = node.children[2].startOffset
        val minCodeFenceCount = if (language != null && node.children.size > 3) 3 else 2
        val end = node.children[(node.children.size - 2).coerceAtLeast(minCodeFenceCount)].endOffset
        block(content.subSequence(start, end).toString().replaceIndent(), language, style)
    } else {
        // invalid code block, skipping
    }
}

@Composable
fun MarkdownCodeBlock(
    content: String,
    node: ASTNode,
    style: TextStyle = LocalMarkdownTypography.current.code,
    block: @Composable (String, String?, TextStyle) -> Unit = { code, language, style -> MarkdownCode(code = code, language = language, style = style) },
) {
    val start = node.children[0].startOffset
    val end = node.children[node.children.size - 1].endOffset
    val language = node.findChildOfType(MarkdownTokenTypes.FENCE_LANG)?.getTextInNode(content)?.toString()
    block(content.subSequence(start, end).toString().replaceIndent(), language, style)
}

@Composable
fun MarkdownCodeBackground(
    color: Color,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    border: BorderStroke? = null,
    elevation: Dp = 0.dp,
    showHeader: Boolean = false,
    language: String? = null,
    code: String = "",
    content: @Composable () -> Unit,
) {
    val a11yLabels = LocalMarkdownA11yLabels.current
    Box(
        modifier = modifier
            .shadow(elevation, shape, clip = false)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .background(color = color, shape = shape)
            .clip(shape)
            .semantics(mergeDescendants = false) {
                isTraversalGroup = true
                contentDescription = if (language.isNullOrBlank()) a11yLabels.codeBlock
                else a11yLabels.codeBlockWithLanguage(language)
            }
            .pointerInput(Unit) {},
        propagateMinConstraints = true
    ) {
        if (showHeader) {
            Column {
                MarkdownCodeTopBar(
                    language = language,
                    code = code
                )
                MarkdownDivider(
                    color = LocalMarkdownColors.current.dividerColor.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )
                content()
            }
        } else {
            content()
        }
    }
}
