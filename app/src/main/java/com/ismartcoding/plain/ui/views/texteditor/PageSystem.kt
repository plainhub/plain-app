package com.ismartcoding.plain.ui.views.texteditor

import com.ismartcoding.lib.extensions.splitInParts
import java.util.*

class PageSystem(var text: String = "") {
    private val pages: MutableList<String> = LinkedList()
    private val startingLines: IntArray
    var onPageChanged: ((page: Int) -> Unit)? = null
    var currentPage = 0
        private set
    private var pageSystemEnabled = true
    private val charForPage = 5000
    private val splitChars = arrayOf('\n', '.', ':', '?', '!', ';', ' ')

    init {
        if (pageSystemEnabled && text.isNotEmpty()) {
            pages.addAll(text.splitInParts(charForPage, splitChars))
        } else {
            pages.add(text)
        }
        startingLines = IntArray(pages.size)
        setStartingLines()
    }

    val startingLine: Int
        get() = startingLines[currentPage]
    val currentPageText: String
        get() = pages[currentPage]

    fun savePage(currentText: String) {
        pages[currentPage] = currentText
    }

    fun nextPage() {
        if (!canReadNextPage()) return
        goToPage(currentPage + 1)
    }

    fun prevPage() {
        if (!canReadPrevPage()) return
        goToPage(currentPage - 1)
    }

    fun goToPage(page: Int) {
        var newPage = page
        if (newPage >= pages.size) newPage = pages.size - 1
        if (newPage < 0) newPage = 0
        val shouldUpdateLines = newPage > currentPage && canReadNextPage()
        if (shouldUpdateLines) {
            val nOfNewLineNow = currentPageText.length - currentPageText.replace("\n", "").length + 1 // normally the last line is not counted so we have to add 1
            val nOfNewLineBefore = startingLines[currentPage + 1] - startingLines[currentPage]
            val difference = nOfNewLineNow - nOfNewLineBefore
            updateStartingLines(currentPage + 1, difference)
        }
        currentPage = newPage
        onPageChanged?.invoke(newPage)
    }

    fun setStartingLines() {
        var startingLine: Int
        var nOfNewLines: Int
        var text: String
        startingLines[0] = 0
        var i = 1
        while (i < pages.size) {
            text = pages[i - 1]
            nOfNewLines = text.length - text.replace("\n", "").length + 1
            startingLine = startingLines[i - 1] + nOfNewLines
            startingLines[i] = startingLine
            i++
        }
    }

    fun updateStartingLines(
        fromPage: Int,
        difference: Int,
    ) {
        var newFromPage = fromPage
        if (difference == 0) return
        if (newFromPage < 1) newFromPage = 1
        var i: Int = newFromPage
        while (i < pages.size) {
            startingLines[i] += difference
            i++
        }
    }

    val maxPage: Int
        get() = pages.size - 1

    fun getAllText(currentPageText: String): String {
        pages[currentPage] = currentPageText
        return pages.joinToString("")
    }

    fun canReadNextPage(): Boolean {
        return currentPage < pages.size - 1
    }

    fun canReadPrevPage(): Boolean {
        return currentPage >= 1
    }
}
