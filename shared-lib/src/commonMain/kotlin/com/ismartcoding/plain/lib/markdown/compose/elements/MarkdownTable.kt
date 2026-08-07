package com.ismartcoding.plain.lib.markdown.compose.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.ismartcoding.plain.lib.markdown.annotator.AnnotatorSettings
import com.ismartcoding.plain.lib.markdown.annotator.annotatorSettings
import com.ismartcoding.plain.lib.markdown.annotator.buildMarkdownAnnotatedString
import com.ismartcoding.plain.lib.markdown.compose.LocalImageTransformer
import com.ismartcoding.plain.lib.markdown.compose.LocalImageWidth
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownColors
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownDimens
import com.ismartcoding.plain.lib.markdown.compose.LocalMarkdownInlineContent
import com.ismartcoding.plain.lib.markdown.compose.elements.material.MarkdownBasicText
import kotlinx.collections.immutable.toPersistentMap
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.flavours.gfm.GFMElementTypes.HEADER
import org.intellij.markdown.flavours.gfm.GFMElementTypes.ROW
import org.intellij.markdown.flavours.gfm.GFMTokenTypes.CELL
import org.intellij.markdown.flavours.gfm.GFMTokenTypes.TABLE_SEPARATOR

/**
 * Internal hook so the table iteration can pass the current body row index
 * (header is row 0, body rows start at 1) to [MarkdownTableRow] without
 * breaking the public `rowBlock` lambda signature. Custom `rowBlock`
 * implementations can read this to set their own collection semantics.
 */
val LocalTableRowIndex = compositionLocalOf { 1 }

@Composable
fun MarkdownTable(
    content: String,
    node: ASTNode,
    style: TextStyle,
    annotatorSettings: AnnotatorSettings = annotatorSettings(),
    headerBlock: @Composable (String, ASTNode, Dp, TextStyle) -> Unit = { content, header, tableWidth, style ->
        MarkdownTableHeader(
            content = content, header = header, tableWidth = tableWidth, style = style, annotatorSettings = annotatorSettings,
        )
    },
    rowBlock: @Composable (String, ASTNode, Dp, TextStyle) -> Unit = { content, header, tableWidth, style ->
        MarkdownTableRow(
            content = content, header = header, tableWidth = tableWidth, style = style, annotatorSettings = annotatorSettings,
        )
    },
) {
    val tableMaxWidth = LocalMarkdownDimens.current.tableMaxWidth
    val tableCellWidth = LocalMarkdownDimens.current.tableCellWidth
    val tableCornerSize = LocalMarkdownDimens.current.tableCornerSize
    val dividerColor = LocalMarkdownColors.current.dividerColor

    val columnsCount = remember(node) { node.findChildOfType(HEADER)?.children?.count { it.type == CELL } ?: 0 }
    val rowsCount = remember(node) { node.children.count { it.type == ROW } + 1 /* header */ }
    val tableMinWidth = tableCellWidth * columnsCount

    val backgroundCodeColor = LocalMarkdownColors.current.tableBackground
    BoxWithConstraints(
        modifier = Modifier
            .background(backgroundCodeColor, RoundedCornerShape(tableCornerSize))
            .widthIn(max = tableMaxWidth)
            .semantics {
                collectionInfo = CollectionInfo(rowCount = rowsCount, columnCount = columnsCount)
            }
    ) {
        val scrollable = maxWidth <= tableMinWidth
        Column(
            modifier = if (scrollable) {
                Modifier.horizontalScroll(rememberScrollState()).requiredWidth(tableMinWidth)
            } else {
                Modifier.fillMaxWidth()
            }
        ) {
            var rowIndex = 1
            node.children.forEach {
                when (it.type) {
                    HEADER -> headerBlock(content, it, tableMinWidth, style)
                    ROW -> {
                        CompositionLocalProvider(LocalTableRowIndex provides rowIndex) {
                            rowBlock(content, it, tableMinWidth, style)
                        }
                        rowIndex++
                    }
                    TABLE_SEPARATOR -> { /* Handled by header row bottom border */ }
                }
            }
        }
    }
}

@Composable
fun MarkdownTableHeader(
    content: String,
    header: ASTNode,
    tableWidth: Dp,
    style: TextStyle,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    annotatorSettings: AnnotatorSettings = annotatorSettings(),
) {
    val tableCellPadding = LocalMarkdownDimens.current.tableCellPadding
    val headerBg = LocalMarkdownColors.current.tableHeaderBackground
    val dividerColor = LocalMarkdownColors.current.dividerColor

    Column {
        Row(
            verticalAlignment = verticalAlignment,
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)
        ) {
            header.children.filter { it.type == CELL }.forEachIndexed { colIndex, cell ->
                if (colIndex > 0) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(dividerColor)
                    )
                }
                Column(
                    modifier = Modifier
                        .background(headerBg)
                        .weight(1f)
                        .padding(tableCellPadding)
                        .semantics {
                            heading()
                            collectionItemInfo = CollectionItemInfo(
                                rowIndex = 0, rowSpan = 1,
                                columnIndex = colIndex, columnSpan = 1,
                            )
                        }
                ) {
                    MarkdownTableBasicText(
                        content = content,
                        cell = cell,
                        style = style.copy(fontWeight = FontWeight.Bold),
                        softWrap = false,
                        annotatorSettings = annotatorSettings,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(dividerColor)
        )
    }
}

@Composable
fun MarkdownTableRow(
    content: String,
    header: ASTNode,
    tableWidth: Dp,
    style: TextStyle,
    rowIndex: Int = LocalTableRowIndex.current,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    annotatorSettings: AnnotatorSettings = annotatorSettings(),
) {
    val tableCellPadding = LocalMarkdownDimens.current.tableCellPadding
    val dividerColor = LocalMarkdownColors.current.dividerColor

    Column {
        Row(
            verticalAlignment = verticalAlignment,
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)
        ) {
            header.children.filter { it.type == CELL }.forEachIndexed { colIndex, cell ->
                if (colIndex > 0) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(dividerColor)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(tableCellPadding)
                        .semantics {
                            collectionItemInfo = CollectionItemInfo(
                                rowIndex = rowIndex, rowSpan = 1,
                                columnIndex = colIndex, columnSpan = 1,
                            )
                        },
                ) {
                    MarkdownTableBasicText(
                        content = content,
                        cell = cell,
                        style = style,
                        annotatorSettings = annotatorSettings,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(dividerColor)
        )
    }
}

/**
 * Renders an individual table cell.
 *
 * GFM table cells contain inline content only (no block elements). This composable builds the
 * cell's [androidx.compose.ui.text.AnnotatedString] via the standard inline annotator pipeline
 * and resolves inline content from two sources:
 *  - user-provided placeholders via [LocalMarkdownInlineContent]
 *  - inline images (`![alt](url)`) found in the cell, rendered through [LocalImageTransformer]
 *
 * Images in cells are always rendered inline (the GFM spec disallows block content inside cells),
 * so block-image promotion is intentionally disabled.
 */
@Composable
fun MarkdownTableBasicText(
    content: String,
    cell: ASTNode,
    style: TextStyle,
    softWrap: Boolean = true,
    annotatorSettings: AnnotatorSettings = annotatorSettings(),
) {
    val text = buildAnnotatedString {
        pushStyle(style.toSpanStyle())
        buildMarkdownAnnotatedString(content = content, node = cell, annotatorSettings = annotatorSettings)
        pop()
    }

    val transformer = LocalImageTransformer.current
    val userInlineContent = LocalMarkdownInlineContent.current.inlineContent
    val inlineImageWidth = LocalImageWidth.current
    val density = LocalDensity.current

    val containerSize = remember { mutableStateOf(Size.Unspecified) }
    val imageSizeByLink = remember { mutableStateMapOf<String, Size>() }
    val imageSizeByLinkSnapshot = imageSizeByLink.toPersistentMap()

    val resolvedInlineContent = remember(
        text, userInlineContent, transformer, inlineImageWidth,
        containerSize.value, imageSizeByLinkSnapshot,
    ) {
        userInlineContent + buildImageInlineContent(
            content = text,
            node = cell,
            transformer = transformer,
            density = density,
            containerSize = containerSize.value,
            inlineImageWidth = inlineImageWidth,
            imageSizeByLink = imageSizeByLinkSnapshot,
            inlineImageAsBlock = false,
            imageSizeChanged = { link, size -> imageSizeByLink += (link to size) },
        )
    }

    MarkdownBasicText(
        text = text,
        modifier = Modifier.onPlaced { coords ->
            coords.parentLayoutCoordinates?.also { containerSize.value = it.size.toSize() }
        },
        style = style,
        softWrap = softWrap,
        inlineContent = resolvedInlineContent,
    )
}