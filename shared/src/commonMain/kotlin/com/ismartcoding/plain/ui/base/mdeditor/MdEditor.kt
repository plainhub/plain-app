package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.markdowntext.MarkdownText
import com.ismartcoding.plain.ui.base.mdeditor.blocks.BlockEditorState
import com.ismartcoding.plain.ui.base.mdeditor.blocks.MdBlockKind
import com.ismartcoding.plain.ui.base.mdeditor.blocks.MdEditorBlock
import com.ismartcoding.plain.ui.base.mdeditor.livepreview.MarkdownLivePreviewTransformation
import com.ismartcoding.plain.ui.base.mdeditor.livepreview.rememberLivePreviewStyles
import com.ismartcoding.plain.ui.models.MdEditorViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme

/**
 * Block-based markdown editor. Each text line is its own editable block with
 * live-preview styling when unfocused; fenced code, math blocks, tables and
 * standalone images are atomic blocks rendered with the real markdown renderer.
 * Tapping an atomic block reveals its raw source for editing. A cross-block
 * selection mode renders every block read-only with selection highlights.
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
    val focusRequesters = remember { HashMap<Long, FocusRequester>() }
    // selection-mode drag-to-extend: latest root-space bounds of every rendered block
    val selectionBounds = remember { HashMap<Long, Rect>() }

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
                if (editor.selectionMode) {
                    SelectionBlockView(editor, block, focusRequesters, selectionBounds)
                } else if (block.kind == MdBlockKind.TEXT) {
                    TextBlockField(editor, block, focusRequesters)
                } else {
                    AtomicBlockView(editor, block, focusRequesters)
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
                .combinedClickable(
                    onClick = { editor.startEdit(block) },
                    onLongClick = {
                        // long-press on a rendered atomic block starts cross-block selection
                        // anchored on this whole block
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
}

/**
 * Read-only rendered view used in cross-block selection mode: selected blocks are
 * highlighted; taps extend the selection. Tapping an already-selected boundary
 * (anchor/focus) block switches it to a read-only text field with a preset native
 * selection so the handles can refine that boundary to character precision.
 */
@Composable
private fun SelectionBlockView(
    editor: BlockEditorState,
    block: MdEditorBlock,
    focusRequesters: MutableMap<Long, FocusRequester>,
    selectionBounds: MutableMap<Long, Rect>,
) {
    val selected = editor.isBlockSelected(block.id)
    val highlight = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))

    if (editor.refinedBlockId == block.id) {
        val fr = remember { FocusRequester() }
        DisposableEffect(block.id) {
            focusRequesters[block.id] = fr
            onDispose { focusRequesters.remove(block.id) }
        }
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
                .then(highlight)
                .focusRequester(fr)
                .onFocusChanged { if (!it.isFocused) editor.exitRefine() },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        )
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
