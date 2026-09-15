package com.ismartcoding.plain.lib.codeeditor

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Incremental highlight pipeline: spans are computed per line on demand and cached; lexer
 * carry-over states (block comments, triple-quoted strings) are remembered sparsely so a
 * viewport jump re-lexes at most BACKTRACK_LIMIT lines instead of the whole file.
 */
class HighlightEngine(private val lexer: RegexLexer?) : SynchronizedObject() {

    private val spansCache = LruMap<Int, Array<Span>>(MAX_SPAN_ENTRIES)

    // Lexer state at the START of a line, kept sparsely (every ANCHOR_STRIDE lines while walking).
    private val stateAnchors = LruMap<Int, Int>(MAX_ANCHORS)

    fun spansFor(doc: LineVectorDocument, line: Int): Array<Span>? = synchronized(this) { spansForLocked(doc, line) }

    private fun spansForLocked(doc: LineVectorDocument, line: Int): Array<Span>? {
        if (lexer == null) return null
        spansCache.get(line)?.let { return it }
        val state = stateAt(doc, line) ?: return null
        var current = state
        var index = line
        // Compute the requested line; also seed anchors along the way for nearby requests.
        while (true) {
            val text = doc.lineText(index)
            val result = lexer.lex(text, current)
            if (index == line) {
                spansCache.put(index, result.spans)
                stateAnchors.put(line + 1, result.nextState)
                return result.spans
            }
            if (index % ANCHOR_STRIDE == 0) stateAnchors.put(index, current)
            current = result.nextState
            index++
        }
    }

    private fun stateAt(doc: LineVectorDocument, line: Int): Int? {
        stateAnchors.get(line)?.let { return it }
        var best = -1
        for ((anchor, _) in stateAnchors.entriesSnapshot()) {
            if (anchor < line && anchor > best) best = anchor
        }
        val start = if (line - best <= BACKTRACK_LIMIT) best + 1 else -1
        if (start < 0) return null
        var state = if (best >= 0) stateAnchors.get(best)!! else 0
        for (index in start until line) {
            val result = lexer!!.lex(doc.lineText(index), state)
            state = result.nextState
            if (index % ANCHOR_STRIDE == 0) stateAnchors.put(index, state)
        }
        return state
    }

    /** Drops cached data for lines >= [from] (edits shift everything below them). */
    fun invalidateFrom(from: Int) = synchronized(this) {
        spansCache.entriesSnapshot().filter { it.first >= from }.forEach { spansCache.remove(it.first) }
        stateAnchors.entriesSnapshot().filter { it.first >= from + 1 }.forEach { stateAnchors.remove(it.first) }
    }

    fun clear() = synchronized(this) {
        spansCache.clear()
        stateAnchors.clear()
    }

    companion object {
        private const val MAX_SPAN_ENTRIES = 4096
        private const val MAX_ANCHORS = 512
        private const val ANCHOR_STRIDE = 64
        private const val BACKTRACK_LIMIT = 4096
    }
}
