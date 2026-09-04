package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextLayoutResult
import com.ismartcoding.plain.ui.base.mdeditor.blocks.BlockEditorState

/**
 * Keyboard navigation that crosses block boundaries: Backspace at a block's start
 * merges/deletes, Up/Down at the first/last visual line move to the previous/next
 * block. Returns true when the event is consumed.
 */
internal fun handleBlockNavKeys(
    editor: BlockEditorState,
    event: KeyEvent,
    layout: TextLayoutResult?,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val sel = editor.buffer.selection
    if (sel.min != sel.max) return false
    return when (event.key) {
        Key.Backspace -> editor.backspaceAtStart()
        Key.DirectionUp -> {
            val line = layout?.getLineForOffset(sel.min) ?: 0
            if (line == 0) editor.activatePrevious() else false
        }
        Key.DirectionDown -> {
            if (layout != null && layout.getLineForOffset(sel.min) == layout.lineCount - 1) {
                editor.activateNext()
            } else {
                false
            }
        }
        else -> false
    }
}
