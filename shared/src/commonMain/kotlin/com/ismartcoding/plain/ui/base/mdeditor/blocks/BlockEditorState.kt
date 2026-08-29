package com.ismartcoding.plain.ui.base.mdeditor.blocks

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import com.ismartcoding.plain.ui.extensions.add
import com.ismartcoding.plain.ui.extensions.inlineWrap
import com.ismartcoding.plain.ui.extensions.setSelection
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Holds the block list of the markdown editor. Every block owns a [TextFieldState]
 * whose text is the block's raw markdown, so the full document is always
 * [text] = blocks joined with newlines — the raw text remains the single source
 * of truth for autosave. Undo/redo is document-level (debounced snapshots for
 * typing, flushed on structural changes), which also covers block splits/merges.
 */

class MdEditorBlock(
    val id: Long,
    val state: TextFieldState,
    kind: MdBlockKind,
    editing: Boolean = false,
) {
    var kind by mutableStateOf(kind)
    var editing by mutableStateOf(editing)
    fun content(): String = state.text.toString()
}

/** A selection endpoint: a caret offset inside a specific block. */
data class BlockAnchor(val blockId: Long, val offset: Int)

class BlockEditorState {
    val blocks = mutableStateListOf<MdEditorBlock>()
    val focusedBlockId = mutableStateOf<Long?>(null)
    val pendingFocus = mutableStateOf<Long?>(null)
    val canUndo = mutableStateOf(false)
    val canRedo = mutableStateOf(false)

    // ── cross-block selection (whole blocks + boundary refinement) ──
    // anchor stays fixed while the focus follows taps; both are (blockId, offset)
    // pairs that map to absolute document offsets for copy/cut.
    var selectionMode by mutableStateOf(false)
    var selectionAnchor by mutableStateOf<BlockAnchor?>(null)
    var selectionFocus by mutableStateOf<BlockAnchor?>(null)
    var refinedBlockId by mutableStateOf<Long?>(null)

    private var nextId = 1L
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()
    private var lastSnapshot = ""
    private var snapshotJob: Job? = null
    private var normalizeJob: Job? = null

    fun text(): String = blocks.joinToString("\n") { it.content() }

    /** Starts the debounced observers that maintain undo snapshots and structure. */
    fun start(scope: CoroutineScope) {
        scope.launch {
            snapshotFlow { text() }.collect {
                if (it != lastSnapshot) {
                    snapshotJob?.cancel()
                    snapshotJob = scope.launch {
                        delay(600)
                        commitSnapshot()
                    }
                }
                normalizeJob?.cancel()
                normalizeJob = scope.launch {
                    delay(400)
                    normalize()
                }
            }
        }
    }

    fun loadText(text: String) {
        flush()
        blocks.clear()
        MdBlockParser.parse(text).forEach { blocks.add(newBlock(it.kind, it.content)) }
        lastSnapshot = text
        undoStack.clear()
        redoStack.clear()
        canUndo.value = false
        canRedo.value = false
        focusedBlockId.value = null
        blocks.firstOrNull()?.let { pendingFocus.value = it.id }
    }

    // ---- block operations -------------------------------------------------

    /**
     * Re-parses a block whose text gained a newline (Enter key or multi-line paste)
     * into several blocks and moves the caret into the block under the old caret.
     */
    fun splitMultilineBlock(block: MdEditorBlock) {
        val idx = blocks.indexOf(block)
        if (idx < 0) return
        val content = block.content()
        if (!content.contains('\n')) return
        val caret = block.state.selection.min
        val parsed = MdBlockParser.parse(content)
        if (parsed.size == 1) {
            block.kind = parsed[0].kind
            return
        }
        splice(idx, parsed, caret)
    }

    /** Backspace at the very start of a block: deletes an empty block or merges into the previous one. */
    fun backspaceAtStart(block: MdEditorBlock): Boolean {
        val idx = blocks.indexOf(block)
        if (idx <= 0) return false
        val sel = block.state.selection
        if (sel.min != 0 || sel.max != 0) return false
        val prev = blocks[idx - 1]
        val content = block.content()
        if (content.isEmpty()) {
            blocks.removeAt(idx)
            focusBlock(prev, prev.content().length)
            return true
        }
        if (prev.kind == MdBlockKind.TEXT) {
            val prevLen = prev.content().length
            prev.state.edit {
                append(content)
                setSelection(prevLen)
            }
            blocks.removeAt(idx)
            focusedBlockId.value = prev.id
            return true
        }
        // previous is a rendered atomic block: open it in source mode at its end
        focusBlock(prev, prev.content().length)
        return true
    }

    fun moveFocusToPrevious(block: MdEditorBlock): Boolean {
        val idx = blocks.indexOf(block)
        if (idx <= 0) return false
        val prev = blocks[idx - 1]
        focusBlock(prev, prev.content().length)
        return true
    }

    fun moveFocusToNext(block: MdEditorBlock): Boolean {
        val idx = blocks.indexOf(block)
        if (idx < 0 || idx >= blocks.size - 1) return false
        focusBlock(blocks[idx + 1], 0)
        return true
    }

    fun startEdit(block: MdEditorBlock) {
        focusBlock(block, block.content().length)
    }

    /** Leaves source mode of an atomic block; re-splits it when its raw text no longer matches its kind. */
    fun commitAtomic(block: MdEditorBlock) {
        if (!block.editing) return
        block.editing = false
        val idx = blocks.indexOf(block)
        if (idx < 0) return
        val parsed = MdBlockParser.parse(block.content())
        if (parsed.size == 1 && parsed[0].kind == block.kind) return
        splice(idx, parsed, null)
    }

    /**
     * Re-parses the whole document so that line-by-line typed fences/tables/math
     * collapse into atomic blocks. States of blocks whose content is unchanged are
     * reused so caret and IME state survive; the focused block is preserved by
     * mapping its caret offset into the new structure.
     */
    fun normalize() {
        if (blocks.isEmpty()) return
        val parsed = MdBlockParser.parse(text())
        if (parsed.size == blocks.size && parsed.indices.all { parsed[it].content == blocks[it].content() }) {
            parsed.indices.forEach {
                val b = blocks[it]
                b.kind = parsed[it].kind
                // a focused block that just turned atomic must keep its source editable
                if (b.kind != MdBlockKind.TEXT && focusedBlockId.value == b.id) b.editing = true
            }
            return
        }
        val focusAbs = focusedAbsoluteOffset()
        rebuild(parsed, focusAbs)
    }

    // ---- toolbar entry points ----------------------------------------------

    fun insertAtFocused(before: String, after: String = "") {
        val b = targetBlockForInsert() ?: return
        b.state.edit { inlineWrap(before, after) }
        focusBlock(b, b.state.selection.min)
    }

    fun insertText(s: String) {
        val b = targetBlockForInsert() ?: return
        b.state.edit { add(s) }
        focusBlock(b, b.state.selection.min)
    }

    fun moveCaretToStart() {
        val b = focusedBlock() ?: blocks.firstOrNull() ?: return
        focusBlock(b, 0)
    }

    fun moveCaretToEnd() {
        val b = focusedBlock() ?: blocks.lastOrNull() ?: return
        focusBlock(b, b.content().length)
    }

    fun focusInitial() {
        val p = pendingFocus.value
        if (p != null && blocks.any { it.id == p }) return
        blocks.firstOrNull()?.let { focusBlock(it, 0) }
    }

    // ---- cross-block selection ------------------------------------------------

    fun enterSelectionMode() {
        exitSelectionMode()
        selectionMode = true
        focusedBlockId.value = null
        pendingFocus.value = null
    }

    fun exitSelectionMode() {
        selectionMode = false
        selectionAnchor = null
        selectionFocus = null
        refinedBlockId = null
    }

    /**
     * Tap in selection mode: the first tap anchors on a whole block, later taps move
     * the focus end (block-granular). Both endpoints are re-bound to the block edge
     * facing the other endpoint so the selection always covers whole blocks.
     * Tapping an already-selected boundary block is handled by the UI via [startRefine].
     */
    fun tapBlockInSelection(block: MdEditorBlock) {
        if (selectionAnchor == null) {
            selectionAnchor = BlockAnchor(block.id, 0)
            selectionFocus = BlockAnchor(block.id, block.content().length)
            return
        }
        setFocusEndToBlock(block.id)
    }

    /** Move the selection's focus end to fully cover [blockId]. Shared by tap and drag extension. */
    fun setFocusEndToBlock(blockId: Long) {
        val a = selectionAnchor ?: return
        val anchorIdx = blocks.indexOfFirst { it.id == a.blockId }
        val anchorBlock = blocks.getOrNull(anchorIdx) ?: return
        val idx = blocks.indexOfFirst { it.id == blockId }
        if (idx < 0) return
        if (idx >= anchorIdx) {
            selectionAnchor = BlockAnchor(anchorBlock.id, 0)
            selectionFocus = BlockAnchor(blockId, blocks[idx].content().length)
        } else {
            selectionAnchor = BlockAnchor(anchorBlock.id, anchorBlock.content().length)
            selectionFocus = BlockAnchor(blockId, 0)
        }
    }

    fun selectAllBlocks() {
        blocks.firstOrNull() ?: return
        selectionAnchor = BlockAnchor(blocks.first().id, 0)
        selectionFocus = BlockAnchor(blocks.last().id, blocks.last().content().length)
    }

    fun isBlockSelected(id: Long): Boolean {
        val range = selectedDocRange() ?: return false
        var abs = 0
        for (b in blocks) {
            val len = b.content().length
            if (b.id == id) return abs <= range.second && range.first <= abs + len
            abs += len + 1
        }
        return false
    }

    /** Absolute document range [start, end) covered by the selection, or null. */
    fun selectedDocRange(): Pair<Int, Int>? {
        val a = selectionAnchor ?: return null
        val f = selectionFocus ?: return null
        val start = blockAbsoluteOffset(a)
        val end = blockAbsoluteOffset(f)
        return if (start <= end) start to end else end to start
    }

    fun selectedText(): String? {
        val range = selectedDocRange() ?: return null
        if (range.second <= range.first) return null
        return text().substring(range.first.coerceIn(0, text().length), range.second.coerceIn(0, text().length))
    }

    fun deleteSelectedRange() {
        val range = selectedDocRange() ?: return
        val t = text()
        if (range.first < 0 || range.second > t.length || range.second <= range.first) return
        exitSelectionMode()
        applyText(t.removeRange(range.first, range.second))
    }

    /** Boundary refinement: preset the native selection inside the anchor/focus block. */
    fun startRefine(block: MdEditorBlock) {
        val len = block.content().length
        val isAnchor = selectionAnchor?.blockId == block.id
        val from = if (isAnchor) (selectionAnchor?.offset ?: 0) else 0
        val to = if (isAnchor) len else (selectionFocus?.offset ?: len)
        refinedBlockId = block.id
        block.state.edit { selection = TextRange(from.coerceIn(0, length), to.coerceIn(0, length)) }
        pendingFocus.value = block.id
    }

    /** Called by the refining block whenever its native selection changes. */
    fun updateSelectionBoundary(block: MdEditorBlock) {
        val sel = block.state.selection
        val isAnchor = selectionAnchor?.blockId == block.id
        if (isAnchor) {
            selectionAnchor = BlockAnchor(block.id, sel.min)
        } else if (selectionFocus?.blockId == block.id) {
            selectionFocus = BlockAnchor(block.id, sel.max)
        }
    }

    fun exitRefine() {
        refinedBlockId = null
    }

    private fun blockAbsoluteOffset(a: BlockAnchor): Int {
        var abs = 0
        for (b in blocks) {
            if (b.id == a.blockId) return abs + a.offset.coerceIn(0, b.content().length)
            abs += b.content().length + 1
        }
        return abs
    }

    // ---- undo / redo -------------------------------------------------------

    fun undo() {
        flush()
        val t = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(text())
        lastSnapshot = t
        applyText(t)
        canUndo.value = undoStack.isNotEmpty()
        canRedo.value = true
    }

    fun redo() {
        flush()
        val t = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(text())
        lastSnapshot = t
        applyText(t)
        canRedo.value = redoStack.isNotEmpty()
        canUndo.value = true
    }

    // ---- internals ----------------------------------------------------------

    private fun newBlock(kind: MdBlockKind, content: String): MdEditorBlock =
        MdEditorBlock(nextId++, TextFieldState(content), kind)

    internal fun focusedBlock(): MdEditorBlock? =
        focusedBlockId.value?.let { id -> blocks.firstOrNull { it.id == id } }

    /**
     * Toolbar inserts go to the focused text block; when the focused block is a
     * rendered atomic block (or nothing is focused) a fresh text block is inserted
     * after it so raw markdown never lands inside rendered content.
     */
    private fun targetBlockForInsert(): MdEditorBlock? {
        val focused = focusedBlock()
        if (focused != null && (focused.kind == MdBlockKind.TEXT || focused.editing)) return focused
        val idx = if (focused != null) blocks.indexOf(focused) + 1 else blocks.size
        val nb = newBlock(MdBlockKind.TEXT, "")
        blocks.add(idx.coerceIn(0, blocks.size), nb)
        return nb
    }

    private fun focusBlock(b: MdEditorBlock, offset: Int) {
        if (b.kind != MdBlockKind.TEXT) b.editing = true
        b.state.edit { setSelection(offset.coerceIn(0, length)) }
        focusedBlockId.value = b.id
        pendingFocus.value = b.id
    }

    /** Replaces blocks[idx] with the parsed sub-blocks; caretAbs positions the caret inside them. */
    private fun splice(idx: Int, parsed: List<ParsedBlock>, caretAbs: Int?) {
        var targetIdx = parsed.size - 1
        var targetOffset = parsed.last().content.length
        if (caretAbs != null) {
            var abs = 0
            for (i in parsed.indices) {
                val len = parsed[i].content.length
                if (caretAbs <= abs + len) {
                    targetIdx = i
                    targetOffset = caretAbs - abs
                    break
                }
                abs += len + 1
            }
        }
        val newList = parsed.map { newBlock(it.kind, it.content) }
        blocks.removeAt(idx)
        blocks.addAll(idx, newList)
        // only take focus when this splice originates from caret activity (Enter/paste),
        // never from commitAtomic which runs because focus already moved elsewhere
        if (caretAbs != null) {
            focusBlock(newList[targetIdx], targetOffset)
        }
    }

    /** Absolute caret offset in the full document, or null when nothing is focused. */
    private fun focusedAbsoluteOffset(): Int? {
        val focused = focusedBlock() ?: return null
        var abs = 0
        for (b in blocks) {
            if (b.id == focused.id) return abs + focused.state.selection.min
            abs += b.content().length + 1
        }
        return null
    }

    private fun rebuild(parsed: List<ParsedBlock>, focusAbs: Int?) {
        val used = HashSet<Long>()
        val newList = parsed.map { p ->
            val match = blocks.firstOrNull { it.id !in used && it.content() == p.content }
            if (match != null) {
                used.add(match.id)
                match.kind = p.kind
                match
            } else {
                newBlock(p.kind, p.content)
            }
        }
        blocks.clear()
        blocks.addAll(newList)
        if (focusAbs == null) return
        var abs = 0
        var target: MdEditorBlock? = null
        var offset = 0
        for (b in newList) {
            val len = b.content().length
            if (focusAbs <= abs + len) {
                target = b
                offset = focusAbs - abs
                break
            }
            abs += len + 1
        }
        if (target == null && newList.isNotEmpty()) {
            target = newList.last()
            offset = target.content().length
        }
        target?.let { focusBlock(it, offset) }
    }

    private fun applyText(t: String) {
        val focused = focusedBlock()
        val focusIdx = if (focused != null) blocks.indexOf(focused) else 0
        val offset = focused?.state?.selection?.min ?: 0
        rebuild(MdBlockParser.parse(t), null)
        val target = blocks.getOrNull(focusIdx.coerceIn(0, blocks.size - 1)) ?: return
        focusBlock(target, offset)
    }

    private fun commitSnapshot() {
        val cur = text()
        if (cur != lastSnapshot) {
            undoStack.addLast(lastSnapshot)
            if (undoStack.size > 100) undoStack.removeFirst()
            redoStack.clear()
            canUndo.value = true
            canRedo.value = false
            lastSnapshot = cur
        }
    }

    private fun flush() {
        snapshotJob?.cancel()
        snapshotJob = null
        normalizeJob?.cancel()
        normalizeJob = null
        commitSnapshot()
    }
}
