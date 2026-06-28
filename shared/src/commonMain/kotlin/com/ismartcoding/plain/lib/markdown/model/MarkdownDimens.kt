package com.ismartcoding.plain.lib.markdown.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
interface MarkdownDimens {
    val dividerThickness: Dp
    val codeBackgroundCornerSize: Dp
    val blockQuoteThickness: Dp
    val tableMaxWidth: Dp
    val tableCellWidth: Dp
    val tableCellPadding: Dp
    val tableCornerSize: Dp
}

@Immutable
private data class DefaultMarkdownDimens(
    override val dividerThickness: Dp,
    override val codeBackgroundCornerSize: Dp,
    override val blockQuoteThickness: Dp,
    override val tableMaxWidth: Dp,
    override val tableCellWidth: Dp,
    override val tableCellPadding: Dp,
    override val tableCornerSize: Dp,
) : MarkdownDimens

@Composable
fun markdownDimens(
    dividerThickness: Dp = 1.dp,
    codeBackgroundCornerSize: Dp = 8.dp,
    blockQuoteThickness: Dp = 2.dp,
    tableMaxWidth: Dp = Dp.Unspecified,
    tableCellWidth: Dp = 160.dp,
    tableCellPadding: Dp = 16.dp,
    tableCornerSize: Dp = 8.dp,
): MarkdownDimens = DefaultMarkdownDimens(
    dividerThickness = dividerThickness,
    codeBackgroundCornerSize = codeBackgroundCornerSize,
    blockQuoteThickness = blockQuoteThickness,
    tableMaxWidth = tableMaxWidth,
    tableCellWidth = tableCellWidth,
    tableCellPadding = tableCellPadding,
    tableCornerSize = tableCornerSize,
)
