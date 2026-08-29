package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.markdowntext.MarkdownText
import com.ismartcoding.plain.ui.base.mdeditor.blocks.BlockEditorState
import com.ismartcoding.plain.ui.base.mdeditor.blocks.MdBlockKind
import com.ismartcoding.plain.ui.base.mdeditor.blocks.MdEditorBlock
import com.ismartcoding.plain.ui.base.mdeditor.livepreview.MarkdownLivePreviewTransformation
import com.ismartcoding.plain.ui.base.mdeditor.livepreview.rememberLivePreviewStyles
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.models.MdEditorViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import androidx.compose.runtime.withFrameNanos

/**
 * Block-based markdown editor. Each text line is its own editable block with
 * live-preview styling when unfocused; fenced code, math blocks, tables and
 * standalone images are atomic blocks rendered with the real markdown renderer.
 * Tapping an atomic block reveals its raw source for editing.
 */
@Composable
fun MdEditor(
    modifier: Modifier,
    mdEditorVM: MdEditorViewModel,
    scrollState: ScrollState,
    shouldRequestFocus: Boolean = false,
    onFocusRequested: () -> Unit = {},
    previewerState: MediaPreviewerState,
) {
    val editor = mdEditorVM.blocks
    val focusRequesters = remember { HashMap<Long, FocusRequester>() }

    LaunchedEffect(shouldRequestFocus) {
        if (shouldRequestFocus) {
            editor.focusInitial()
            onFocusRequested()
        }
    }

    // central focus consumer: focus requesters are registered by the block composables,
    // which may not exist yet in the same frame a new block was created — retry a few frames
    LaunchedEffect(Unit) {
        snapshotFlow { editor.pendingFocus.value }.collect { id ->
            if (id != null) {
                repeat(10) {
                    val fr = focusRequesters[id]
                    if (fr != null) {
                        editor.pendingFocus.value = null
                        fr.requestFocus()
                        return@collect
                    }
                    withFrameNanos { }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN),
    ) {
        editor.blocks.forEach { block ->
            key(block.id) {
                if (block.kind == MdBlockKind.TEXT) {
                    TextBlockField(editor, block, focusRequesters)
                } else {
                    AtomicBlockView(editor, block, focusRequesters, previewerState)
                }
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

@Composable
private fun TextBlockField(
    editor: BlockEditorState,
    block: MdEditorBlock,
    focusRequesters: MutableMap<Long, FocusRequester>,
) {
    val liveStyles = rememberLivePreviewStyles()
    var focused by remember { mutableStateOf(false) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val fr = remember { FocusRequester() }

    DisposableEffect(block.id) {
        focusRequesters[block.id] = fr
        onDispose { focusRequesters.remove(block.id) }
    }

    // Enter / multi-line paste: re-split this block into parsed sub-blocks
    LaunchedEffect(block.id) {
        snapshotFlow { block.state.text }.collect {
            if (it.contains('\n')) editor.splitMultilineBlock(block)
        }
    }

    // focused blocks show raw source (Obsidian-style); unfocused blocks render preview
    val transformation = remember(focused, liveStyles) {
        if (!focused) MarkdownLivePreviewTransformation(liveStyles, IntRange.EMPTY) else null
    }

    BasicTextField(
        state = block.state,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 28.dp)
            .focusRequester(fr)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) editor.focusedBlockId.value = block.id
            }
            .onPreviewKeyEvent { e -> handleBlockNavKeys(editor, block, e, layout) },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        outputTransformation = transformation,
        onTextLayout = { layout = it() },
    )
}

@Composable
private fun AtomicBlockView(
    editor: BlockEditorState,
    block: MdEditorBlock,
    focusRequesters: MutableMap<Long, FocusRequester>,
    previewerState: MediaPreviewerState,
) {
    val fr = remember { FocusRequester() }

    if (block.editing) {
        val mono = block.kind != MdBlockKind.IMAGE
        // commit only on a real focus loss: onFocusChanged also fires with false
        // when the field enters composition, before it ever receives focus
        var hadFocus by remember { mutableStateOf(false) }
        DisposableEffect(block.id) {
            focusRequesters[block.id] = fr
            onDispose { focusRequesters.remove(block.id) }
        }
        BasicTextField(
            state = block.state,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 28.dp)
                .focusRequester(fr)
                .onFocusChanged {
                    if (it.isFocused) {
                        hadFocus = true
                        editor.focusedBlockId.value = block.id
                    } else if (hadFocus) {
                        hadFocus = false
                        editor.commitAtomic(block)
                    }
                }
                .onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.Backspace) {
                        editor.backspaceAtStart(block)
                    } else {
                        false
                    }
                },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = if (mono) FontFamily.Monospace else null,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { editor.startEdit(block) },
        ) {
            MarkdownText(
                text = block.state.text.toString(),
                isTextSelectable = false,
                previewerState = previewerState,
            )
        }
    }
}

private fun handleBlockNavKeys(
    editor: BlockEditorState,
    block: MdEditorBlock,
    event: KeyEvent,
    layout: TextLayoutResult?,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val sel = block.state.selection
    if (sel.min != sel.max) return false
    return when (event.key) {
        Key.Backspace -> editor.backspaceAtStart(block)
        Key.DirectionUp -> {
            val line = layout?.getLineForOffset(sel.min) ?: 0
            if (line == 0) editor.moveFocusToPrevious(block) else false
        }
        Key.DirectionDown -> {
            if (layout != null && layout.getLineForOffset(sel.min) == layout.lineCount - 1) {
                editor.moveFocusToNext(block)
            } else {
                false
            }
        }
        else -> false
    }
}
