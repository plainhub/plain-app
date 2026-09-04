package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.markdowntext.MarkdownText
import com.ismartcoding.plain.ui.base.mdeditor.blocks.BlockEditorState
import com.ismartcoding.plain.ui.base.mdeditor.blocks.MdEditorBlock

/**
 * Read-only rendered view used in cross-block selection mode: selected blocks are
 * highlighted; taps extend the selection. Tapping an already-selected boundary
 * (anchor/focus) block switches it to a read-only text field with a preset native
 * selection so the handles can refine that boundary to character precision.
 */
@Composable
internal fun SelectionBlockView(
    editor: BlockEditorState,
    block: MdEditorBlock,
    selectionBounds: MutableMap<Long, Rect>,
) {
    val selected = editor.isBlockSelected(block.id)
    val highlight = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))

    if (editor.refinedBlockId == block.id) {
        RefineField(editor, block)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 28.dp)
            .then(if (selected) highlight else Modifier)
            .onGloballyPositioned { selectionBounds[block.id] = it.boundsInRoot() }
            .pointerInput(block.id, editor.selectionMode) {
                // drag across blocks extends the selection to the block under the finger
                detectDragGestures { change, _ ->
                    change.consume()
                    val top = selectionBounds[block.id]?.top ?: return@detectDragGestures
                    val y = top + change.position.y
                    selectionBounds.entries
                        .filter { y >= it.value.top && y < it.value.bottom }
                        .minByOrNull { it.value.top }
                        ?.let { editor.setFocusEndToBlock(it.key) }
                }
            }
            .clickable {
                val isBoundary = editor.selectionAnchor?.blockId == block.id ||
                    editor.selectionFocus?.blockId == block.id
                if (selected && isBoundary) editor.startRefine(block) else editor.tapBlockInSelection(block)
            },
    ) {
        MarkdownText(
            text = block.state.text.toString(),
            isTextSelectable = false,
        )
    }
}

/** Read-only field refining a selection boundary: native handles move the endpoint. */
@Composable
private fun RefineField(editor: BlockEditorState, block: MdEditorBlock) {
    val fr = remember { FocusRequester() }
    LaunchedEffect(block.id) { fr.requestFocus() }
    LaunchedEffect(block.id) {
        snapshotFlow { block.state.selection }.collect {
            if (editor.refinedBlockId == block.id) editor.updateSelectionBoundary(block)
        }
    }
    BasicTextField(
        state = block.state,
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 28.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            .focusRequester(fr)
            .onFocusChanged { if (!it.isFocused) editor.exitRefine() },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
    )
}
