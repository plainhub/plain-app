package com.ismartcoding.plain.lib.markdown.compose.elements

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.ismartcoding.plain.lib.markdown.annotator.AnnotatorSettings
import com.ismartcoding.plain.lib.markdown.annotator.annotatorSettings
import com.ismartcoding.plain.lib.markdown.annotator.buildMarkdownAnnotatedString
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownTypography
import org.intellij.markdown.ast.ASTNode

@Composable
fun MarkdownParagraph(
    content: String,
    node: ASTNode,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalMarkdownTypography.current.paragraph,
    annotatorSettings: AnnotatorSettings = annotatorSettings(),
) {
    val styledText = buildAnnotatedString {
        pushStyle(style.toSpanStyle())
        buildMarkdownAnnotatedString(content = content, node = node, annotatorSettings = annotatorSettings)
        pop()
    }

    MarkdownText(
        content = styledText,
        node = node,
        modifier = modifier,
        style = style,
        sourceContent = content,
    )
}
