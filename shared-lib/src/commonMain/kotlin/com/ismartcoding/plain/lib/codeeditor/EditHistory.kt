package com.ismartcoding.plain.lib.codeeditor

import com.ismartcoding.plain.lib.TimeHelper

/**
 * Undo/redo over document replaces. Adjacent single-character typing is coalesced into one
 * step (same as mainstream editors): inserts merge while contiguous and within the time
 * window; deletions merge while backspacing sequentially.
 */
class EditHistory(
    private val maxSteps: Int = 200,
    private val coalesceMillis: Long = 800,
) {
    class Step(val range: EditRange, val afterRange: EditRange, val oldText: String, val newText: String, val timeMillis: Long)

    private val undoStack = ArrayDeque<Step>()
    private val redoStack = ArrayDeque<Step>()
    private var nowMillis: () -> Long = { TimeHelper.nowMillis() }

    fun injectClock(clock: () -> Long) {
        nowMillis = clock
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** Span covered by [text] inserted at [start] (in post-edit coordinates). */
    private fun extentOf(start: EditRange, text: String): EditRange {
        val lines = text.split('\n')
        val endLine = start.startLine + lines.size - 1
        val endCol = if (lines.size == 1) start.startCol + lines[0].length else lines.last().length
        return EditRange(start.startLine, start.startCol, endLine, endCol)
    }

    /** Records an applied replace; [range] must describe the replaced span before the edit. */
    fun push(range: EditRange, oldText: String, newText: String, isTyping: Boolean) {
        redoStack.clear()
        val now = nowMillis()
        val last = undoStack.lastOrNull()
        if (isTyping && last != null && now - last.timeMillis <= coalesceMillis && canCoalesce(last, range, oldText, newText)) {
            undoStack.removeLast()
            val mergedText = last.newText + newText
            undoStack.addLast(Step(last.range, extentOf(last.range, mergedText), last.oldText, mergedText, now))
            return
        }
        undoStack.addLast(Step(range, extentOf(range, newText), oldText, newText, now))
        while (undoStack.size > maxSteps) undoStack.removeFirst()
    }

    private fun canCoalesce(last: Step, range: EditRange, oldText: String, newText: String): Boolean {
        if (oldText.isNotEmpty()) {
            // Sequential backspace/delete merges when it extends the previous deletion leftward.
            if (newText.isNotEmpty()) return false
            val prev = last.range
            return oldText.length == 1 &&
                range.endLine == prev.startLine && range.endCol == prev.startCol &&
                range.startLine == prev.startLine && range.startCol == prev.startCol - 1
        }
        if (newText.length != 1) return false
        if (last.newText.isEmpty()) return false
        // Sequential typing merges when inserted right after the previous insert.
        return range.startLine == last.range.startLine &&
            range.startCol == last.range.startCol + last.newText.length &&
            range.endLine == range.startLine
    }

    /** Pops the next undo step and applies it to [doc]; returns the restored selection anchor. */
    fun undo(doc: LineVectorDocument): EditRange? {
        val step = undoStack.removeLastOrNull() ?: return null
        doc.replace(step.afterRange, step.oldText)
        redoStack.addLast(step)
        return EditRange(step.range.startLine, step.range.startCol, step.range.startLine, step.range.startCol)
    }

    fun redo(doc: LineVectorDocument): EditRange? {
        val step = redoStack.removeLastOrNull() ?: return null
        doc.replace(step.range, step.newText)
        undoStack.addLast(step)
        return step.afterRange
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
