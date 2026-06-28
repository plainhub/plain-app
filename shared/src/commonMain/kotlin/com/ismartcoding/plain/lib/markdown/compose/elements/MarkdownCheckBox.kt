package com.ismartcoding.plain.lib.markdown.compose.elements

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode

@Composable
fun MarkdownCheckBox(
    content: String,
    node: ASTNode,
    style: TextStyle,
    checkedIndicator: @Composable (Boolean, Modifier) -> Unit = { checked, modifier ->
        MarkdownText(
            content = "[${if (checked) "x" else " "}] ",
            node = node,
            modifier = modifier,
            style = style.copy(fontFamily = FontFamily.Monospace)
        )
    },
) {
    val checked = node.getTextInNode(content).contains("[x]")
    Row {
        checkedIndicator(checked, Modifier.padding(end = 4.dp))
    }
}
