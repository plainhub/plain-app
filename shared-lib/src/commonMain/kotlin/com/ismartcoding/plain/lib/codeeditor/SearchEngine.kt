package com.ismartcoding.plain.lib.codeeditor

data class SearchMatch(val line: Int, val startCol: Int, val length: Int)

/**
 * Line-oriented text search. The controller drives iteration over document lines on a
 * background dispatcher with cancellation between lines, so 100MB files stream results
 * instead of blocking on a full scan.
 */
object SearchEngine {

    /** Returns match start columns for a single line, in order. */
    fun findInLine(text: String, query: String, caseSensitive: Boolean, regex: Regex?): List<SearchMatch> {
        if (query.isEmpty() && regex == null) return emptyList()
        if (regex != null) {
            val matches = regex.findAll(text)
            return matches.filter { it.value.isNotEmpty() }.map { SearchMatch(-1, it.range.first, it.value.length) }.toList()
        }
        if (query.isEmpty()) return emptyList()
        val result = ArrayList<SearchMatch>(4)
        var from = 0
        while (from <= text.length - query.length) {
            val idx = if (caseSensitive) text.indexOf(query, from) else indexOfIgnoreCase(text, query, from)
            if (idx < 0) break
            result.add(SearchMatch(-1, idx, query.length))
            from = idx + query.length.coerceAtLeast(1)
        }
        return result
    }

    fun indexOfIgnoreCase(text: String, query: String, fromIndex: Int): Int {
        val limit = text.length - query.length
        var from = fromIndex.coerceAtLeast(0)
        while (from <= limit) {
            var i = 0
            while (i < query.length && text[from + i].equals(query[i], ignoreCase = true)) i++
            if (i == query.length) return from
            from++
        }
        return -1
    }
}
