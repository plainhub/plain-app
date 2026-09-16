package com.ismartcoding.plain.ui.components.codeeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PIconButton

/**
 * The single persistent input field (mdeditor-proven pattern: the IME session never moves
 * between fields). It floats over the active row and mirrors its text; all edits are
 * translated into document commands by the controller. Hardware/IME key events are
 * intercepted for cross-line caret movement and backspace joins at column 0; soft keyboards
 * get an assist toolbar for the same moves.
 */
@Composable
fun EditorInputSurface(controller: EditorController) {
    val fontSize = controller.fontSizeSp.value
    val style = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 1.5f).sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(controller.readOnly.value) {
        if (!controller.readOnly.value) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val info = controller.listState.layoutInfo
    val visual = controller.mapper.logicalToVisual(controller.activeLine.value)
    val item = info.visibleItemsInfo.firstOrNull { it.index == visual }
    val density = LocalDensity.current
    val gutterWidthPx = with(density) { ((controller.gutterDigits() * 9 + 16).dp).toPx() }

    Box(modifier = Modifier.fillMaxWidth()) {
        if (item != null) {
            val widthDp = if (controller.wrapContent.value) {
                with(density) { (info.viewportSize.width - gutterWidthPx).toDp() }
            } else {
                with(density) { maxOf(controller.contentWidthPx.value, info.viewportSize.width * 0.5f).toDp() }
            }
            BasicTextField(
                value = androidx.compose.ui.text.input.TextFieldValue(
                    controller.fieldText.value,
                    controller.fieldSelection.value,
                ),
                onValueChange = { nv -> controller.onFieldChange(nv.text, nv.selection) },
                textStyle = style,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .offset(
                        x = with(density) { (gutterWidthPx - controller.hPanOffset.floatValue).toDp() },
                        y = with(density) { item.offset.toDp() },
                    )
                    .width(widthDp)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        val sel = controller.fieldSelection.value
                        val caretAtStart = sel.start == 0 && sel.end == 0
                        when (event.key) {
                            Key.DirectionUp -> {
                                controller.moveCaretRelative(-1, 0); true
                            }
                            Key.DirectionDown -> {
                                controller.moveCaretRelative(1, 0); true
                            }
                            Key.Backspace -> {
                                if (controller.selection.value == null && caretAtStart) {
                                    controller.joinWithPreviousLine()
                                    true
                                } else false
                            }
                            else -> false
                        }
                    },
            )
        }
        EditAssistToolbar(controller, modifier = Modifier.align(Alignment.BottomEnd))
    }
}

/** Cursor/undo keys for soft keyboards; also the fallback for backspace-joins. */
@Composable
fun EditAssistToolbar(controller: EditorController, modifier: Modifier = Modifier) {
    if (controller.readOnly.value) return
    val bg = MaterialTheme.colorScheme.surfaceContainerHigh
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg.copy(alpha = 0.94f))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        PIconButton(icon = Res.drawable.chevron_up, tint = tint) { controller.moveCaretRelative(-1, 0) }
        PIconButton(icon = Res.drawable.chevron_down, tint = tint) { controller.moveCaretRelative(1, 0) }
        PIconButton(icon = Res.drawable.chevron_left, tint = tint) { controller.moveCaretRelative(0, -1) }
        PIconButton(icon = Res.drawable.chevron_right, tint = tint) { controller.moveCaretRelative(0, 1) }
        PIconButton(icon = Res.drawable.delete_forever, tint = tint) {
            if (controller.selection.value != null) {
                controller.deleteSelection()
            } else if (controller.activeCol.value == 0) {
                controller.joinWithPreviousLine()
            }
        }
        PIconButton(
            icon = Res.drawable.undo_2,
            enabled = controller.canUndo.value,
            tint = if (controller.canUndo.value) MaterialTheme.colorScheme.onSurface else tint.copy(alpha = 0.4f),
        ) { controller.undo() }
        PIconButton(
            icon = Res.drawable.redo_2,
            enabled = controller.canRedo.value,
            tint = if (controller.canRedo.value) MaterialTheme.colorScheme.onSurface else tint.copy(alpha = 0.4f),
        ) { controller.redo() }
    }
}

/** Floating actions over an active selection (copy / cut / select all / clear). */
@Composable
fun SelectionToolbarOverlay(controller: EditorController) {
    val selection = controller.selection.value?.normalized() ?: return
    val clipboard = LocalClipboardManager.current
    val bg = MaterialTheme.colorScheme.surfaceContainerHigh
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(bg.copy(alpha = 0.96f))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PIconButton(icon = Res.drawable.copy, tint = tint) {
                controller.selectedText()?.let { clipboard.setText(AnnotatedString(it)) }
            }
            if (!controller.readOnly.value) {
                PIconButton(icon = Res.drawable.scissors, tint = tint) {
                    controller.selectedText()?.let { clipboard.setText(AnnotatedString(it)) }
                    controller.deleteSelection()
                }
            }
            PIconButton(icon = Res.drawable.select_all, tint = tint) {
                controller.selectAll()
            }
            PIconButton(icon = Res.drawable.x, tint = tint) {
                controller.selection.value = null
            }
        }
    }
}
