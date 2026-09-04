package com.ismartcoding.plain.ui.base.mdeditor.blocks

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import com.ismartcoding.plain.ui.extensions.add
import com.ismartcoding.plain.ui.extensions.inlineWrap
import com.ismartcoding.plain.ui.extensions.setSelection
import com.ismartcoding.plain.ui.extensions.toggleWrap
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Holds the block list of the markdown editor. Inactive blocks store their raw
 * markdown in their own [MdEditorBlock.state]; the block being edited lives in
 * [buffer] — the single [TextFieldState] the one long-lived editor field renders.
 *
 * The editor field keeps IME focus and [buffer] for its whole lifetime: moving
 * the caret between blocks swaps the buffer content instead of transferring
 * focus. Transferring focus between two fields makes Compose tear down the old
 * IME session and start a new one, and the two commands routinely land in
 * separate IME command flushes — the keyboard hides and immediately re-shows.
 * With a single session that never changes owner, block switches never touch
 * the IME connection.
 *
 * The full document is always [text] = blocks joined with newlines (the active
 * block's contribution comes from [buffer]), so the raw text remains the single
 * source of truth for autosave. Undo/redo is document-level (debounced
 * snapshots for typing, flushed on structural changes), which also covers
 * block splits/merges.
 */

class MdEditorBlock(
    val id: Long,
    val state: TextFieldState,
    kind: MdBlockKind,
) {
    var kind by mutableStateOf(kind)
    fun content(): String = state.text.toString()
}

/** A selection endpoint: a caret offset inside a specific block. */
data class BlockAnchor(val blockId: Long, val offset: Int)

class BlockEditorState {
    val blocks = mutableStateListOf<MdEditorBlock>()

    /** The block whose content [buffer] currently holds. */
    val focusedBlockId = mutableStateOf<Long?>(null)
    val canUndo = mutableStateOf(false)
    val canRedo = mutableStateOf(false)

    /**
     * The single editing buffer of the persistent editor field. Selection here is
     * the document caret while [focusedBlockId] is set.
     */
    val buffer = TextFieldState()

    /**
     * Closing marker awaiting confirmation after a wrap action (bold, italic, link…):
     * the caret sits right before it, and pressing Enter should jump past the marker
     * instead of inserting a newline. Null when no wrap is pending.
     */
    var pendingWrapSuffix: String? = null
        private set

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

    fun text(): String {
        val activeId = focusedBlockId.value
        return blocks.joinToString("\n") { b ->
            if (b.id == activeId) buffer.text.toString() else b.content()
        }
    }

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
        blocks.firstOrNull()?.let { activate(it, 0) }
    }

    // ---- active block / buffer ------------------------------------------------

    fun activeBlock(): MdEditorBlock? =
        focusedBlockId.value?.let { id -> blocks.firstOrNull { it.id == id } }

    private fun contentOf(b: MdEditorBlock): String =
        if (b.id == focusedBlockId.value) buffer.text.toString() else b.content()

    private fun syncActiveBlockToBuffer() {
        val b = activeBlock() ?: return
        val t = buffer.text.toString()
        if (t != b.content()) {
            b.state.edit { replace(0, length, t) }
        }
    }

    /**
     * Makes [block] the block the editor field edits. The previously active block
     * (including a rendered atomic block) is committed back to storage first, then
     * the buffer is loaded with [block]'s content and the caret placed at [offset].
     * No focus is requested here: the editor field never loses or regains focus
     * during block switches, so the IME session is never restarted.
     */
    fun activate(block: MdEditorBlock, offset: Int) {
        val current = activeBlock()
        if (current != null && current.id != block.id) {
            syncActiveBlockToBuffer()
            if (current.kind != MdBlockKind.TEXT) commitAtomic(current)
            pendingWrapSuffix = null
        }
        if (block.id == focusedBlockId.value) {
            buffer.edit { setSelection(offset.coerceIn(0, length)) }
        } else {
            buffer.edit {
                replace(0, length, block.content())
                setSelection(offset.coerceIn(0, length))
            }
            focusedBlockId.value = block.id
        }
    }

    fun activateInitial() {
        val p = focusedBlockId.value
        if (p != null && blocks.any { it.id == p }) return
        blocks.firstOrNull()?.let { activate(it, 0) }
    }

    fun activatePrevious(): Boolean {
        val idx = blocks.indexOfFirst { it.id == focusedBlockId.value }
        if (idx <= 0) return false
        val prev = blocks[idx - 1]
        activate(prev, prev.content().length)
        return true
    }

    fun activateNext(): Boolean {
        val idx = blocks.indexOfFirst { it.id == focusedBlockId.value }
        if (idx < 0 || idx >= blocks.size - 1) return false
        activate(blocks[idx + 1], 0)
        return true
    }

    // ---- block operations -------------------------------------------------

    /**
     * Enter key or multi-line paste: the buffer gained a newline, so re-parse its
     * text into several blocks. The caret's segment becomes the new active block
     * and the buffer is reloaded with just that segment. Enter at the end of a
     * list line continues the marker on the next line (numbered markers
     * increment); Enter on a marker-only line clears the marker.
     */
    fun splitActiveBlock() {
        val b = activeBlock() ?: return
        if (!buffer.text.contains('\n')) return
        var caret = buffer.selection.min
        var text = buffer.text.toString()
        val firstLine = text.substringBefore('\n')
        val m = listContinuationRegex.find(firstLine)
        if (m != null && caret == firstLine.length + 1) {
            val rest = text.substringAfter('\n', "")
            if (m.value.length >= firstLine.length) {
                text = rest
                caret = 0
            } else {
                val next = if (m.groups[1] != null) {
                    val box = m.groups[2]?.value?.replace("[xX]", "[ ]") ?: ""
                    m.groups[1]!!.value + box + " "
                } else {
                    "${m.groups[3]!!.value.toInt() + 1}. "
                }
                text = firstLine + "\n" + next + rest
                caret += next.length
            }
            buffer.edit {
                replace(0, length, text)
                setSelection(caret.coerceIn(0, length))
            }
            caret = buffer.selection.min
            text = buffer.text.toString()
        }
        val idx = blocks.indexOf(b)
        if (idx < 0) return
        val parsed = MdBlockParser.parse(text)
        if (parsed.size == 1) {
            syncActiveBlockToBuffer()
            b.kind = parsed[0].kind
            return
        }
        var targetIdx = parsed.size - 1
        var targetOffset = parsed.last().content.length
        var abs = 0
        for (i in parsed.indices) {
            val len = parsed[i].content.length
            if (caret <= abs + len) {
                targetIdx = i
                targetOffset = caret - abs
                break
            }
            abs += len + 1
        }
        val newList = parsed.mapIndexed { i, p ->
            // the caret segment reuses the block id so focusedBlockId stays stable
            if (i == targetIdx) MdEditorBlock(b.id, TextFieldState(p.content), p.kind)
            else newBlock(p.kind, p.content)
        }
        blocks.removeAt(idx)
        blocks.addAll(idx, newList)
        focusedBlockId.value = newList[targetIdx].id
        buffer.edit {
            replace(0, length, newList[targetIdx].content())
            setSelection(targetOffset.coerceIn(0, length))
        }
    }

    // list marker at line start: indent, then bullet [+ task box] or "N."
    private val listContinuationRegex = Regex("""^\s*(?:([-*+])(\s+\[[ xX]])?\s|(\d+)\.\s)""")

    /**
     * Backspace at the very start of the active block: deletes an empty block or
     * merges the previous block into the buffer (the active block survives, so the
     * IME session is untouched).
     */
    fun backspaceAtStart(): Boolean {
        val b = activeBlock() ?: return false
        val sel = buffer.selection
        if (sel.min != 0 || sel.max != 0) return false
        val idx = blocks.indexOf(b)
        if (idx <= 0) return false
        val prev = blocks[idx - 1]
        if (buffer.text.isEmpty()) {
            blocks.removeAt(idx)
            activate(prev, prev.content().length)
            return true
        }
        if (prev.kind == MdBlockKind.TEXT) {
            val prevLen = prev.content().length
            buffer.edit {
                replace(0, 0, prev.content())
                setSelection(prevLen)
            }
            blocks.removeAt(idx - 1)
            return true
        }
        // previous is a rendered atomic block: open it in source mode at its end
        activate(prev, prev.content().length)
        return true
    }

    /**
     * Leaves source mode of a former active atomic block; re-splits it when its
     * raw text no longer matches its kind. Storage-only: never touches the buffer.
     */
    fun commitAtomic(block: MdEditorBlock) {
        val idx = blocks.indexOf(block)
        if (idx < 0) return
        val parsed = MdBlockParser.parse(block.content())
        if (parsed.size == 1 && parsed[0].kind == block.kind) return
        spliceStorage(idx, parsed)
    }

    /** Replaces blocks[idx] with the parsed sub-blocks (storage-level, no activation). */
    private fun spliceStorage(idx: Int, parsed: List<ParsedBlock>) {
        val newList = parsed.map { newBlock(it.kind, it.content) }
        blocks.removeAt(idx)
        blocks.addAll(idx, newList)
    }

    /**
     * Re-parses the whole document so that line-by-line typed fences/tables/math
     * collapse into atomic blocks. States of blocks whose content is unchanged are
     * reused; the active block is preserved by mapping its caret offset into the
     * new structure.
     */
    fun normalize() {
        if (blocks.isEmpty()) return
        val parsed = MdBlockParser.parse(text())
        if (parsed.size == blocks.size && parsed.indices.all { parsed[it].content == contentOf(blocks[it]) }) {
            parsed.indices.forEach {
                blocks[it].kind = parsed[it].kind
            }
            return
        }
        val focusAbs = focusedAbsoluteOffset()
        rebuild(parsed, focusAbs)
    }

    // ---- toolbar entry points ----------------------------------------------

    fun insertAtFocused(before: String, after: String = "") {
        val b = targetBlockForInsert() ?: return
        if (b.id != focusedBlockId.value) activate(b, b.content().length)
        buffer.edit { inlineWrap(before, after) }
        armWrapConfirm(after)
    }

    fun insertText(s: String) {
        val b = targetBlockForInsert() ?: return
        if (b.id != focusedBlockId.value) activate(b, b.content().length)
        buffer.edit {
            add(s)
        }
    }

    /** Wrap (or unwrap) the active block's selection with inline markers. */
    fun toggleWrap(before: String, after: String = before) {
        val b = activeBlock() ?: return
        buffer.edit { toggleWrap(before, after) }
        armWrapConfirm(after)
    }

    /**
     * Arms Enter-to-confirm when the caret now sits directly before the closing
     * marker [after] (collapsed selection inside a freshly inserted wrap).
     */
    private fun armWrapConfirm(after: String) {
        pendingWrapSuffix = null
        if (after.isEmpty()) return
        val caret = buffer.selection
        if (!caret.collapsed) return
        if (buffer.text.startsWith(after, caret.min)) pendingWrapSuffix = after
    }

    /** Drops any pending wrap confirmation (caret moved elsewhere, block switched…). */
    fun clearWrapConfirm() {
        pendingWrapSuffix = null
    }

    /**
     * Runs inside the editor field's [androidx.compose.foundation.text.input.InputTransformation].
     * When a wrap suffix is pending and the incoming edit is exactly one newline typed
     * right before it, the newline is rejected and the caret jumps past the marker —
     * the keyboard's Enter "confirms" the wrap instead of breaking the line. Any other
     * edit, or a caret that no longer sits before the marker, disarms the pending suffix.
     */
    fun onFieldInput(buffer: TextFieldBuffer) {
        val suffix = pendingWrapSuffix ?: return
        val orig = buffer.originalText
        val caret = buffer.originalSelection
        if (!caret.collapsed || caret.min + suffix.length > orig.length ||
            !orig.startsWith(suffix, caret.min)
        ) {
            pendingWrapSuffix = null
            return
        }
        // proposed text must be the original with a single '\n' inserted at the caret
        if (buffer.length != orig.length + 1) return
        var p = 0
        while (p < orig.length && buffer.charAt(p) == orig[p]) p++
        if (p == caret.min && buffer.charAt(p) == '\n') {
            buffer.revertAllChanges()
            buffer.selection = androidx.compose.ui.text.TextRange(caret.min + suffix.length)
            pendingWrapSuffix = null
        }
    }

    // heading / list / quote / callout markers at the start of the focused block's line
    private val linePrefixRegex = Regex("""^(?:#{1,6}|[-*+]|\d+\.|>)(?: ?\[[^\]]*])? +""")

    /** Toggle a line-start marker on the active text block; empty [prefix] strips any marker. */
    fun toggleLinePrefix(prefix: String) {
        val b = activeBlock() ?: return
        if (b.kind != MdBlockKind.TEXT) return
        buffer.edit {
            val m = linePrefixRegex.find(toString())
            if (m == null) {
                replace(0, 0, prefix)
            } else if (!m.value.startsWith(prefix)) {
                replace(0, m.value.length, prefix)
            } else {
                replace(0, m.value.length, "")
                return@edit
            }
            setSelection((selection.min + prefix.length).coerceIn(0, length))
        }
    }

    fun moveCaretToStart() {
        buffer.edit { setSelection(0) }
    }

    fun moveCaretToEnd() {
        buffer.edit { setSelection(length) }
    }

    // ---- cross-block selection ------------------------------------------------

    fun enterSelectionMode() {
        exitSelectionMode()
        syncActiveBlockToBuffer()
        pendingWrapSuffix = null
        selectionMode = true
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

    fun selectedBlockCount(): Int = blocks.count { isBlockSelected(it.id) }

    fun isAllSelected(): Boolean = blocks.isNotEmpty() && blocks.all { isBlockSelected(it.id) }

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

    /**
     * Toolbar inserts go to the active text block; when the active block is a
     * rendered atomic block (or nothing is active) a fresh text block is inserted
     * after it so raw markdown never lands inside rendered content.
     */
    private fun targetBlockForInsert(): MdEditorBlock? {
        val focused = activeBlock()
        if (focused != null && focused.kind == MdBlockKind.TEXT) return focused
        val idx = if (focused != null) blocks.indexOf(focused) + 1 else blocks.size
        val nb = newBlock(MdBlockKind.TEXT, "")
        blocks.add(idx.coerceIn(0, blocks.size), nb)
        return nb
    }

    /** Absolute caret offset in the full document, or null when nothing is active. */
    private fun focusedAbsoluteOffset(): Int? {
        val idx = blocks.indexOfFirst { it.id == focusedBlockId.value }
        if (idx < 0) return null
        var abs = 0
        for (i in 0 until idx) {
            abs += contentOf(blocks[i]).length + 1
        }
        return abs + buffer.selection.min
    }

    private fun rebuild(parsed: List<ParsedBlock>, focusAbs: Int?) {
        val used = HashSet<Long>()
        val newList = parsed.map { p ->
            val match = blocks.firstOrNull { it.id !in used && contentOf(it) == p.content }
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
        if (focusAbs == null) {
            focusedBlockId.value = null
            return
        }
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
        target?.let { activate(it, offset) }
    }

    private fun applyText(t: String) {
        val focused = activeBlock()
        val focusIdx = if (focused != null) blocks.indexOf(focused) else 0
        val offset = buffer.selection.min
        rebuild(MdBlockParser.parse(t), null)
        val target = blocks.getOrNull(focusIdx.coerceIn(0, blocks.size - 1)) ?: return
        activate(target, offset)
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
