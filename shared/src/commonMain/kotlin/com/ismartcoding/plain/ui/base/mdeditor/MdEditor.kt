package com.ismartcoding.plain.ui.base.mdeditor

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.markdowntext.MarkdownText
import com.ismartcoding.plain.ui.base.mdeditor.blocks.BlockEditorState
import com.ismartcoding.plain.ui.base.mdeditor.blocks.MdBlockKind
import com.ismartcoding.plain.ui.base.mdeditor.blocks.MdEditorBlock
import com.ismartcoding.plain.ui.base.mdeditor.livepreview.MarkdownLivePreviewTransformation
import com.ismartcoding.plain.ui.base.mdeditor.livepreview.rememberLivePreviewStyles
import com.ismartcoding.plain.ui.base.mdeditor.livepreview.renderPreview
import com.ismartcoding.plain.ui.extensions.setSelection
import com.ismartcoding.plain.ui.models.MdEditorViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme

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
 * The field is emitted as the LAST child of a custom [Layout] and is placed over
 * an invisible placeholder that occupies the active block's slot. Keeping the
 * field in a fixed composition slot matters: moving a composable between block
 * slots detaches its modifier nodes, which cancels the IME session and bounces
 * the keyboard.
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

    Layout(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = PlainTheme.PAGE_HORIZONTAL_MARGIN),
        content = {
            val activeId = editor.focusedBlockId.value
            editor.blocks.forEach { block ->
                key(block.id) {
                    if (editor.selectionMode) {
                        SelectionBlockView(editor, block, selectionBounds)
                    } else if (block.id == activeId) {
                        ActivePlaceholder(editor, block)
                    } else if (block.kind == MdBlockKind.TEXT) {
                        PreviewBlock(editor, block, keyboard, fieldFocus)
                    } else {
                        AtomicPreviewBlock(editor, block, keyboard, fieldFocus)
                    }
                }
            }
            if (!editor.selectionMode && activeId != null) {
                ActiveEditorField(editor, fieldFocus)
            }
        },
    ) { measurables, constraints ->
        val hasField = measurables.size == editor.blocks.size + 1
        var activeIdx = -1
        if (hasField) {
            val activeId = editor.focusedBlockId.value
            activeIdx = editor.blocks.indexOfFirst { it.id == activeId }
        }
        val contentConstraints = constraints.copy(minHeight = 0)
        val field = if (hasField) measurables.last().measure(contentConstraints) else null
        var y = 0
        var fieldY = 0
        val placeables = measurables.dropLast(if (hasField) 1 else 0).mapIndexed { i, m ->
            val p = m.measure(contentConstraints)
            if (i == activeIdx) fieldY = y
            y += p.height
            p
        }
        layout(constraints.maxWidth, y) {
            var cy = 0
            placeables.forEach { p ->
                p.place(0, cy)
                cy += p.height
            }
            field?.place(0, fieldY)
        }
    }
}

/** Style shared by the field and the active placeholder so their layouts match. */
@Composable
private fun activeTextStyle(editor: BlockEditorState) =
    MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontFamily = if (editor.activeBlock()?.kind != MdBlockKind.TEXT) FontFamily.Monospace else null,
    )

/** The one editing field. Fixed composition slot; placed over the active placeholder. */
@Composable
private fun ActiveEditorField(editor: BlockEditorState, fieldFocus: FocusRequester) {
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
private fun ActivePlaceholder(editor: BlockEditorState, block: MdEditorBlock) {
    val keyboard = LocalSoftwareKeyboardController.current
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

@Composable
private fun PreviewBlock(
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

@Composable
private fun AtomicPreviewBlock(
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
    selectionBounds: MutableMap<Long, Rect>,
) {
    val selected = editor.isBlockSelected(block.id)
    val highlight = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))

    if (editor.refinedBlockId == block.id) {
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
