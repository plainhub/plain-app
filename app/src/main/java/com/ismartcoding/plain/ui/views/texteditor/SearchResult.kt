package com.ismartcoding.plain.ui.views.texteditor

import java.util.*

class SearchResult(
    var foundIndex: LinkedList<Int>,
    var textLength: Int,
    var isReplace: Boolean,
    var whatToSearch: String,
    var textToReplace: String,
    var isRegex: Boolean,
) {
    var index = 0

    fun doneReplace() {
        foundIndex.removeAt(index)
        var i: Int = index
        while (i < foundIndex.size) {
            foundIndex[i] = foundIndex[i] + textToReplace.length - textLength
            i++
        }
        index-- // an element was removed so we decrease the index
    }

    fun numberOfResults(): Int {
        return foundIndex.size
    }

    operator fun hasNext(): Boolean {
        return index < foundIndex.size - 1
    }

    fun hasPrevious(): Boolean {
        return index > 0
    }

    fun canReplaceSomething(): Boolean {
        return isReplace && foundIndex.size > 0
    }
}
