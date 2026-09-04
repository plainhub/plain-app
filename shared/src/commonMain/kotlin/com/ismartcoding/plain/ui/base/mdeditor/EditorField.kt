package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.mdeditor.blocks.BlockEditorState
import com.ismartcoding.plain.ui.base.mdeditor.blocks.MdBlockKind
import com.ismartcoding.plain.ui.base.mdeditor.blocks.MdEditorBlock
import com.ismartcoding.plain.ui.base.mdeditor.livepreview.MarkdownLivePreviewTransformation
import com.ismartcoding.plain.ui.base.mdeditor.livepreview.rememberLivePreviewStyles
import com.ismartcoding.plain.ui.extensions.setSelection

/** Style shared by the field and the active placeholder so their layouts match. */
@Composable
internal fun activeTextStyle(editor: BlockEditorState) =
    MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = if (editor.activeBlock()?.kind != MdBlockKind.TEXT) FontFamily.Monospace else null,
    )

/** The one editing field. Fixed composition slot; placed over the active placeholder. */
@Composable
internal fun ActiveEditorField(editor: BlockEditorState, fieldFocus: FocusRequester) {
    val liveStyles = rememberLivePreviewStyles()
    var focused by remember { mutableStateOf(false) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val bringIntoView = remember { BringIntoViewRequester() }

    // claim focus when the field enters composition (page entry, selection-mode exit);
    // block switches never re-compose this slot, so no session churn happens then
    LaunchedEffect(Unit) { fieldFocus.requestFocus() }

    // keep the active block visible when the caret moves to another block
    LaunchedEffect(editor.focusedBlockId.value) {
        if (editor.focusedBlockId.value != null) bringIntoView.bringIntoView()
    }

    // focused blocks show raw source (Obsidian-style); when unfocused the block previews
    val transformation = remember(focused, liveStyles) {
        if (!focused) MarkdownLivePreviewTransformation(liveStyles, IntRange.EMPTY) else null
    }

    // Enter-to-confirm for wrap actions (bold/italic/link…): a pending closing marker
    // turns the keyboard's Enter into "jump past the marker" instead of a newline
    val wrapConfirmTransformation = remember(editor) {
        InputTransformation { editor.onFieldInput(this) }
    }

    BasicTextField(
        state = editor.buffer,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 28.dp)
            .bringIntoViewRequester(bringIntoView)
            .focusRequester(fieldFocus)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { e -> handleBlockNavKeys(editor, e, layout) },
        textStyle = activeTextStyle(editor),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        inputTransformation = wrapConfirmTransformation,
        outputTransformation = transformation,
        onTextLayout = { layout = it() },
    )
}

/**
 * Invisible stand-in for the active block: reserves exactly the space the field
 * occupies and forwards taps to caret positioning. Never focusable — focusing is
 * owned by the persistent field.
 */
@Composable
internal fun ActivePlaceholder(
    editor: BlockEditorState,
    block: MdEditorBlock,
    keyboard: SoftwareKeyboardController?,
) {
    val bringIntoView = remember { BringIntoViewRequester() }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    LaunchedEffect(editor.focusedBlockId.value) {
        if (editor.focusedBlockId.value != null) bringIntoView.bringIntoView()
    }
    Text(
        text = editor.buffer.text.toString(),
        onTextLayout = { layout = it },
        style = activeTextStyle(editor).copy(color = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 28.dp)
            .bringIntoViewRequester(bringIntoView)
            .pointerInput(block.id) {
                detectTapGestures { pos ->
                    val l = layout ?: return@detectTapGestures
                    editor.clearWrapConfirm()
                    editor.buffer.edit {
                        setSelection(l.getOffsetForPosition(pos).coerceIn(0, length))
                    }
                    keyboard?.show()
                }
            },
    )
}
