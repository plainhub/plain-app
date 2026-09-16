package com.ismartcoding.plain.lib.codeeditor

data class SearchMatch(val line: Int, val startCol: Int, val length: Int)

/**
 * Line-oriented text search. The controller drives iteration over document lines on a
 * background dispatcher with cancellation between lines, so 100MB files stream results
 * instead of blocking on a full scan.
 */
object SearchEngine {

    /** Returns match start columns for a single line, in order. */
    fun findInLine(text: String, query: String, caseSensitive: Boolean, regex: Regex?, queryLowered: String? = null): List<SearchMatch> {
        if (query.isEmpty() && regex == null) return emptyList()
        if (regex != null) {
            val matches = regex.findAll(text)
            return matches.filter { it.value.isNotEmpty() }.map { SearchMatch(-1, it.range.first, it.value.length) }.toList()
        }
        if (query.isEmpty()) return emptyList()
        val needle = queryLowered ?: query.lowercase()
        val haystack = if (caseSensitive) text else text.lowercase()
        val result = ArrayList<SearchMatch>(4)
        var from = 0
        while (from <= haystack.length - needle.length) {
            val idx = haystack.indexOf(needle, from)
            if (idx < 0) break
            result.add(SearchMatch(-1, idx, needle.length))
            from = idx + needle.length.coerceAtLeast(1)
        }
        return result
    }

    fun indexOfIgnoreCase(text: String, query: String, fromIndex: Int): Int {
        // String.indexOf is an intrinsified, vectorized scan on both JVM and ART — orders of
        // magnitude faster than per-char case folding on the multi-million-char sweeps a
        // 100MB file produces. The per-line lowercase allocation wins by a wide margin.
        val lowered = text.lowercase()
        val idx = lowered.indexOf(query.lowercase(), fromIndex.coerceAtMost(lowered.length))
        return idx
    }
}
