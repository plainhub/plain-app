package com.ismartcoding.plain.ui.components.codeeditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.lib.codeeditor.EditRange
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

/** Public entry: renders [controller]'s document with gutter, highlighting and search marks. */
@Composable
fun CodeEditor(controller: EditorController, modifier: Modifier = Modifier) {
    val dark = com.ismartcoding.plain.enums.DarkTheme.isDarkTheme(com.ismartcoding.plain.preferences.LocalDarkTheme.current)
    val colors = if (dark) DarkSyntaxColors else LightSyntaxColors
    androidx.compose.runtime.CompositionLocalProvider(LocalEditorSyntaxColors provides colors) {
        Column(modifier = modifier) {
            if (controller.searchVisible.value) {
                EditorSearchBar(controller)
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                EditorViewport(controller, colors)
                if (!controller.readOnly.value) {
                    EditorInputSurface(controller)
                }
                SelectionToolbarOverlay(controller)
            }
            if (controller.statusBarVisible.value) {
                EditorStatusBar(controller)
            }
        }
    }
}

@Composable
private fun EditorViewport(controller: EditorController, colors: EditorSyntaxColors) {
    val fontSize = controller.fontSizeSp.value
    val textStyle = remember(fontSize) {
        TextStyle(fontFamily = FontFamily.Monospace, fontSize = fontSize.sp, lineHeight = (fontSize * 1.5f).sp)
    }
    val gutterStyle = remember(fontSize) {
        TextStyle(fontFamily = FontFamily.Monospace, fontSize = (fontSize - 2).sp, lineHeight = (fontSize * 1.5f).sp)
    }
    val density = LocalDensity.current
    val gutterWidth = (controller.gutterDigits() * 9 + 16).dp
    val scope = rememberCoroutineScope()
    val contentWidthPx = controller.contentWidthPx

    LaunchedEffect(controller, controller.fontSizeSp.value) {
        controller.charWidthPx = with(density) { 8.4.dp.toPx() * controller.fontSizeSp.value / 14f }
        controller.lineHeightPx = with(density) { (controller.fontSizeSp.value * 1.5f).dp.toPx() }
    }

    // Async highlighting for whatever is visible.
    LaunchedEffect(controller.docVersion.value) {
        snapshotFlow { controller.listState.firstVisibleItemIndex to controller.listState.layoutInfo.visibleItemsInfo.size }
            .collect { (first, n) ->
                if (n > 0) {
                    scope.launch(Dispatchers.Default) {
                        controller.computeHighlights(first..(first + n + 8))
                    }
                }
            }
    }

    val wrap = controller.wrapContent.value
    val onSurface = MaterialTheme.colorScheme.onSurface
    val gutterColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)

    // Subscribe to mapper publication: visualCount() itself is not backed by a State.
    controller.mapperVersion.value

    LazyColumn(
        state = controller.listState,
            modifier = Modifier.fillMaxWidth().onSizeChanged { size ->
                controller.pannableWidthPx = size.width.toFloat() - with(density) { gutterWidth.toPx() }
                controller.syncPan()
            },
    ) {
        items(controller.visualCount()) { visual ->
            EditorRow(
                controller = controller,
                colors = colors,
                visual = visual,
                textStyle = textStyle,
                gutterStyle = gutterStyle,
                gutterWidth = gutterWidth,
                wrap = wrap,
                onSurface = onSurface,
                gutterColor = gutterColor,
                contentWidthPx = contentWidthPx,
            )
        }
    }
}

@Composable
private fun EditorRow(
    controller: EditorController,
    colors: EditorSyntaxColors,
    visual: Int,
    textStyle: TextStyle,
    gutterStyle: TextStyle,
    gutterWidth: Dp,
    wrap: Boolean,
    onSurface: Color,
    gutterColor: Color,
    contentWidthPx: MutableState<Float>,
) {
    controller.docVersion.value
    controller.highlightVersion.value
    controller.matchesVersion.value
    controller.mapperVersion.value
    val density = LocalDensity.current
    val selection = controller.selection.value?.normalized()
    val line = controller.rowLine(visual)
    val chunk = controller.rowChunk(visual)
    val isActiveLine = !controller.readOnly.value && line == controller.activeLine.value
    val isFieldRow = isActiveLine && chunk == controller.activeChunk.value

    val text = controller.rowText(visual)
    val charBase = controller.rowCharBase(visual)
    var layout by remember(line, chunk) { mutableStateOf<TextLayoutResult?>(null) }

    val annotated = remember(text, controller.highlightVersion.value, controller.matchesVersion.value, selection, isFieldRow) {
        buildRowText(controller, colors, text, line, charBase, isFieldRow)
    }

    layout?.let { l ->
        if (!wrap) {
            // Track the widest row including the trailing pad, so the pan bound stops the
            // text 8dp short of the cell's right edge instead of clipping it flush.
            val fullWidth = l.size.width + with(density) { RowTrailingPad.toPx() }
            if (fullWidth > contentWidthPx.value) {
                contentWidthPx.value = fullWidth
                controller.syncPan()
            }
        }
    }

    val selStart = if (selection != null && selection.startLine <= line && selection.endLine >= line) {
        if (selection.startLine == line) (selection.startCol - charBase).coerceAtLeast(0) else 0
    } else -1
    val selEnd = if (selection != null && selection.startLine <= line && selection.endLine >= line) {
        if (selection.endLine == line) (selection.endCol - charBase).coerceAtMost(text.length) else text.length
    } else -1

    val activeBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    val rowBg = if (isActiveLine) activeBg else Color.Transparent

    Row(modifier = Modifier.fillMaxWidth().background(rowBg)) {
        Text(
            text = "${line + 1}",
            style = gutterStyle,
            color = if (isActiveLine) onSurface else gutterColor,
            textAlign = TextAlign.Right,
            maxLines = 1,
            modifier = Modifier.width(gutterWidth).padding(end = 8.dp),
        )
        val cellModifier = if (wrap) {
            Modifier.weight(1f)
        } else {
            // Uniform horizontal panning: every row shifts by the same shared offset
            // (short rows just reveal trailing space), clipped at the cell edge.
            Modifier
                .weight(1f)
                .clipToBounds()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (controller.hPan.drag(dragAmount)) {
                            controller.hPanOffset.floatValue = controller.hPan.offsetPx
                        }
                    }
                }
        }
        Box(
            modifier = cellModifier
                .pointerInput(line, chunk, controller.readOnly.value) {
                    detectTapGestures(onTap = { pos ->
                        layout?.let { l ->
                            val col = charBase + l.getOffsetForPosition(pos).coerceIn(0, text.length)
                            controller.setActive(line, col)
                        }
                    })
                }
                .pointerInput(line, chunk) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { pos ->
                            layout?.let { l ->
                                val col = charBase + l.getOffsetForPosition(pos).coerceIn(0, text.length)
                                controller.beginSelectionAt(line, col)
                            }
                        },
                        onDrag = { change, _ ->
                            layout?.let { l ->
                                val col = charBase + l.getOffsetForPosition(change.position).coerceIn(0, text.length)
                                controller.extendSelectionTo(line, col)
                            }
                        },
                    )
                },
        ) {
            Text(
                text = annotated,
                style = textStyle,
                color = onSurface,
                softWrap = wrap,
                onTextLayout = { layout = it },
                modifier = if (wrap) {
                    Modifier.fillMaxWidth()
                } else {
                    // Measure the full line without the cell width cap, pan it, and let the
                    // cell clip. Measuring under the cap would clip the text to one screen
                    // width and make panning a no-op.
                    Modifier
                        .wrapContentWidth(Alignment.Start, unbounded = true)
                        .offset { IntOffset(-controller.hPanOffset.floatValue.roundToInt(), 0) }
                        .padding(end = RowTrailingPad)
                },
            )
            val l = layout
            if (selStart in 0 until selEnd && l != null) {
                SelectionHighlight(l, selStart, selEnd, -controller.hPanOffset.floatValue)
            }
        }
    }
}

@Composable
private fun SelectionHighlight(layout: TextLayoutResult, start: Int, end: Int, panPx: Float) {
    val selColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val rects = ArrayList<Rect>()
    val firstDisplay = layout.getLineForOffset(start)
    val lastDisplay = layout.getLineForOffset((end - 1).coerceAtLeast(start))
    for (dl in firstDisplay..lastDisplay) {
        val left = if (dl == firstDisplay) layout.getBoundingBox(start).left else layout.getLineLeft(dl)
        val right = if (dl == lastDisplay) layout.getBoundingBox((end - 1).coerceAtLeast(start)).right else layout.getLineRight(dl)
        if (right > left) {
            rects.add(Rect(left, layout.getLineTop(dl), right, layout.getLineBottom(dl)))
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Row rects are laid out at offset 0; shift them with the panned content.
        translate(left = panPx) {
            rects.forEach { r ->
                drawRect(color = selColor, topLeft = Offset(r.left, r.top), size = Size(r.width, r.height))
            }
        }
    }
}

private fun buildRowText(
    controller: EditorController,
    colors: EditorSyntaxColors,
    text: String,
    line: Int,
    charBase: Int,
    isFieldRow: Boolean,
): AnnotatedString = buildAnnotatedString {
    append(text)
    if (isFieldRow) {
        addStyle(SpanStyle(color = Color.Transparent), 0, text.length)
    }
    controller.cachedSpans(line)?.forEach { span ->
        val start = span.start - charBase
        val end = span.end - charBase
        if (end > 0 && start < text.length) {
            val style = syntaxSpanStyleFor(colors, span.kind) ?: return@forEach
            addStyle(style, start.coerceAtLeast(0), end.coerceAtMost(text.length))
        }
    }
    val current = controller.currentMatch()
    controller.matchLineInfo(line).forEach { m ->
        val start = m.startCol - charBase
        val end = start + m.length
        if (end > 0 && start < text.length) {
            val isCurrent = current != null && current.line == m.line && current.startCol == m.startCol
            val bg = if (isCurrent) MatchCurrentBg else MatchBg
            addStyle(SpanStyle(background = bg), start.coerceAtLeast(0), end.coerceAtMost(text.length))
        }
    }
}

private val MatchBg = Color(0x24D9A514)
private val MatchCurrentBg = Color(0x59D9A514)

/** Trailing space kept after the text in no-wrap mode, also part of the pan bound. */
private val RowTrailingPad = 8.dp
