package com.ismartcoding.plain.ui.components.codeeditor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorInputSurface(controller: EditorController, modifier: Modifier = Modifier) {
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

    Box(modifier = modifier.fillMaxWidth()) {
        if (item != null) {
            val widthDp = if (controller.wrapContent.value) {
                // Match the row Text's wrap width (cell minus RowTrailingPad) so the
                // caret and wrapped text align with the rendered rows.
                with(density) { (info.viewportSize.width - gutterWidthPx - RowTrailingPad.toPx()).toDp() }
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
