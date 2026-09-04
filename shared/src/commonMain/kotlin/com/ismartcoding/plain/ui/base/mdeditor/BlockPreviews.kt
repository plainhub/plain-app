package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.markdowntext.MarkdownText
import com.ismartcoding.plain.ui.base.mdeditor.blocks.BlockEditorState
import com.ismartcoding.plain.ui.base.mdeditor.blocks.MdBlockKind
import com.ismartcoding.plain.ui.base.mdeditor.blocks.MdEditorBlock
import com.ismartcoding.plain.ui.base.mdeditor.livepreview.rememberLivePreviewStyles
import com.ismartcoding.plain.ui.base.mdeditor.livepreview.renderPreview

/** Inactive TEXT block: live-preview styled, tapping activates the block at the tapped offset. */
@Composable
internal fun PreviewBlock(
    editor: BlockEditorState,
    block: MdEditorBlock,
    keyboard: SoftwareKeyboardController?,
    fieldFocus: FocusRequester,
) {
    val liveStyles = rememberLivePreviewStyles()
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val render = remember(block.content()) { renderPreview(block.content(), liveStyles) }
    Text(
        text = render.annotated,
        onTextLayout = { layout = it },
        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 28.dp)
            .pointerInput(block.id, render) {
                detectTapGestures { pos ->
                    val l = layout ?: return@detectTapGestures
                    val outOffset = l.getOffsetForPosition(pos)
                    editor.activate(block, render.mapToRaw(outOffset))
                    fieldFocus.requestFocus()
                    keyboard?.show()
                }
            },
    )
}

/**
 * Inactive atomic block (code / math / table / image): rendered with the real
 * markdown renderer. Tap edits its raw source; long-press starts cross-block
 * selection anchored on this whole block.
 */
@Composable
internal fun AtomicPreviewBlock(
    editor: BlockEditorState,
    block: MdEditorBlock,
    keyboard: SoftwareKeyboardController?,
    fieldFocus: FocusRequester,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    editor.activate(block, block.content().length)
                    fieldFocus.requestFocus()
                    keyboard?.show()
                },
                onLongClick = {
                    editor.enterSelectionMode()
                    editor.tapBlockInSelection(block)
                },
            ),
    ) {
        MarkdownText(
            text = block.state.text.toString(),
            isTextSelectable = false,
        )
    }
}
