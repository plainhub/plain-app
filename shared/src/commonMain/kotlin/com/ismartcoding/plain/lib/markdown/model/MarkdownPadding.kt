package com.ismartcoding.plain.lib.markdown.model

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
interface MarkdownPadding {
    val block: Dp

    /** Padding top and bottom of a list (per layer) */
    val list: Dp

    /** Padding top of a list item */
    val listItemTop: Dp

    /** Padding bottom of a list item */
    val listItemBottom: Dp

    /** The indent per list level */
    val listIndent: Dp
    val codeBlock: PaddingValues
    val blockQuote: PaddingValues
    val blockQuoteText: PaddingValues
    val blockQuoteBar: PaddingValues.Absolute
}

@Immutable
private data class DefaultMarkdownPadding(
    override val block: Dp,
    override val list: Dp,
    override val listItemTop: Dp,
    override val listItemBottom: Dp,
    override val listIndent: Dp,
    override val codeBlock: PaddingValues,
    override val blockQuote: PaddingValues,
    override val blockQuoteText: PaddingValues,
    override val blockQuoteBar: PaddingValues.Absolute,
) : MarkdownPadding

@Composable
fun markdownPadding(
    block: Dp = 2.dp,
    list: Dp = 4.dp,
    listItemTop: Dp = 4.dp,
    listItemBottom: Dp = 4.dp,
    /** Deprecated, please use `listIndent` instead */
    indentList: Dp? = null,
    listIndent: Dp = 8.dp,
    codeBlock: PaddingValues = PaddingValues(8.dp),
    blockQuote: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
    blockQuoteText: PaddingValues = PaddingValues(vertical = 4.dp),
    blockQuoteBar: PaddingValues.Absolute = PaddingValues.Absolute(left = 4.dp, top = 2.dp, right = 4.dp, bottom = 2.dp),
): MarkdownPadding = DefaultMarkdownPadding(
    block = block,
    list = list,
    listItemTop = listItemTop,
    listItemBottom = listItemBottom,
    listIndent = indentList ?: listIndent,
    codeBlock = codeBlock,
    blockQuote = blockQuote,
    blockQuoteText = blockQuoteText,
    blockQuoteBar = blockQuoteBar,
)
