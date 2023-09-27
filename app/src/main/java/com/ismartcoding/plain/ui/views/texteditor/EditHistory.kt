package com.ismartcoding.plain.ui.views.texteditor

import java.util.*

/**
 * Keeps track of all the edit history of a
 * text.
 */
class EditHistory {
    /**
     * The list of edits in chronological
     * order.
     */
    val history = LinkedList<EditItem>()

    /**
     * The position from which an EditItem will
     * be retrieved when getNext() is called. If
     * getPrevious() has not been called, this
     * has the same value as mmHistory.size().
     */
    var position = 0

    /**
     * Maximum undo history size.
     * If size is negative, then history size is only
     * limited by the device memory.
     */
    var maxHistorySize = -1
        set(value) {
            field = value
            if (value >= 0) {
                trimHistory()
            }
        }

    /**
     * Clear history.
     */
    fun clear() {
        position = 0
        history.clear()
    }

    /**
     * Adds a new edit operation to the history
     * at the current position. If executed
     * after a call to getPrevious() removes all
     * the future history (elements with
     * positions >= current history position).
     */
    fun add(item: EditItem) {
        while (history.size > position) {
            history.removeLast()
        }
        history.add(item)
        position++
        if (maxHistorySize >= 0) {
            trimHistory()
        }
    }

    /**
     * Trim history when it exceeds max history
     * size.
     */
    private fun trimHistory() {
        while (history.size
            > maxHistorySize
        ) {
            history.removeFirst()
            position--
        }
        if (position < 0) {
            position = 0
        }
    }

    /**
     * Traverses the history backward by one
     * position, returns and item at that
     * position.
     */
    val previous: EditItem?
        get() {
            if (position == 0) {
                return null
            }
            position--
            return history[position]
        }

    /**
     * Traverses the history forward by one
     * position, returns and item at that
     * position.
     */
    val next: EditItem?
        get() {
            if (position >= history.size) {
                return null
            }
            val item = history[position]
            position++
            return item
        }

    /**
     * Represents the changes performed by a
     * single edit operation.
     */
    class EditItem(val start: Int, val before: CharSequence?, val after: CharSequence?)
}
