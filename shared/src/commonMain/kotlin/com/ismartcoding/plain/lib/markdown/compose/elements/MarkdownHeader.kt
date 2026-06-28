package com.ismartcoding.plain.lib.markdown.compose.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode

@Composable
fun MarkdownHeader(
    content: String,
    node: ASTNode,
    style: TextStyle,
    contentChildType: IElementType = MarkdownTokenTypes.ATX_CONTENT,
) = MarkdownText(
    modifier = Modifier.semantics {
        heading()
    },
    content = content,
    node = node,
    style = style,
    contentChildType = contentChildType,
)
