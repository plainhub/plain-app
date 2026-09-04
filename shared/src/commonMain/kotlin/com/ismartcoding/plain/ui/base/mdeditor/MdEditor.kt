package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import com.ismartcoding.plain.ui.base.mdeditor.blocks.BlockEditorState
import com.ismartcoding.plain.ui.base.mdeditor.blocks.MdBlockKind
import com.ismartcoding.plain.ui.base.mdeditor.blocks.MdEditorBlock
import com.ismartcoding.plain.ui.models.MdEditorViewModel

/**
 * Block-based markdown editor with live-preview styling. There is exactly ONE
 * editing field: it renders [BlockEditorState.buffer] for the active block and
 * keeps IME focus across block switches — switching lines swaps the buffer
 * content, never the focus, so the IME connection is never restarted and the
 * keyboard never dips. Inactive text blocks render as non-focusable previews;
 * tapping one activates the block (caret placed via the preview's offset map).
 * Fenced code, math blocks, tables and standalone images are atomic blocks
 * rendered with the real markdown renderer; activating one edits its raw source
 * in the same field. A cross-block selection mode renders every block read-only.
 *
 * Layout lives in [BlockColumn]; the single field in [ActiveEditorField]; inactive
 * blocks in [BlockPreviews] / [SelectionBlockView]; key navigation in
 * [handleBlockNavKeys].
 */
@Composable
fun MdEditor(
    modifier: Modifier,
    mdEditorVM: MdEditorViewModel,
    scrollState: ScrollState,
    shouldRequestFocus: Boolean = false,
    onFocusRequested: () -> Unit = {},
) {
    val editor = mdEditorVM.blocks
    // selection-mode drag-to-extend: latest root-space bounds of every rendered block
    val selectionBounds = remember { HashMap<Long, Rect>() }
    val keyboard = LocalSoftwareKeyboardController.current
    val fieldFocus = remember { FocusRequester() }

    LaunchedEffect(shouldRequestFocus) {
        if (shouldRequestFocus) {
            editor.activateInitial()
            fieldFocus.requestFocus()
            onFocusRequested()
        }
    }

    // Enter / multi-line paste: the buffer gained a newline; re-parse into blocks.
    LaunchedEffect(Unit) {
        snapshotFlow { editor.buffer.text }.collect {
            if (it.contains('\n')) editor.splitActiveBlock()
        }
    }

    BlockColumn(editor, scrollState, modifier) {
        editor.blocks.forEach { block ->
            key(block.id) {
                EditorBlockSlot(editor, block, keyboard, fieldFocus, selectionBounds)
            }
        }
        if (!editor.selectionMode && editor.focusedBlockId.value != null) {
            ActiveEditorField(editor, fieldFocus)
        }
    }
}

/** Emits one slot per block: selection view, active placeholder, or a preview. */
@Composable
private fun EditorBlockSlot(
    editor: BlockEditorState,
    block: MdEditorBlock,
    keyboard: SoftwareKeyboardController?,
    fieldFocus: FocusRequester,
    selectionBounds: MutableMap<Long, Rect>,
) {
    val activeId = editor.focusedBlockId.value
    when {
        editor.selectionMode -> SelectionBlockView(editor, block, selectionBounds)
        block.id == activeId -> ActivePlaceholder(editor, block, keyboard)
        block.kind == MdBlockKind.TEXT -> PreviewBlock(editor, block, keyboard, fieldFocus)
        else -> AtomicPreviewBlock(editor, block, keyboard, fieldFocus)
    }
}
