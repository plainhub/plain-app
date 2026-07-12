package com.ismartcoding.plain.lib.ahocorasick.trie

/**
 * Pure-Kotlin multi-pattern matcher. Replaces the Java ahocorasick library so
 * the Pinyin engine stays KMP-pure (no java/ source set needed).
 *
 * The matcher finds all dictionary words that occur in the input text. For each
 * match it produces an [Emit] with the start position, the matched keyword and
 * the keyword length.
 */
class Trie {

    private val root = Node()
    private val keywords: MutableList<String> = mutableListOf()

    fun addKeyword(keyword: String) {
        if (keyword.isEmpty()) return
        var node = root
        for (ch in keyword) {
            node = node.children.getOrPut(ch) { Node() }
        }
        if (!node.isLeaf) {
            node.isLeaf = true
            keywords.add(keyword)
        }
    }

    fun parseText(text: String): Collection<Emit> {
        val emits = mutableListOf<Emit>()
        if (keywords.isEmpty()) return emits
        for (i in text.indices) {
            var node = root
            for (j in i until text.length) {
                node = node.children[text[j]] ?: break
                if (node.isLeaf) {
                    emits.add(Emit(start = i, keyword = text.substring(i, j + 1)))
                }
            }
        }
        return emits
    }

    fun keywords(): Collection<String> = keywords

    private class Node {
        val children: MutableMap<Char, Node> = HashMap()
        var isLeaf: Boolean = false
    }
}

data class Emit(val start: Int, val keyword: String) {
    /** Convenience accessor for the end position of the match (exclusive). */
    val end: Int get() = start + keyword.length
    fun size(): Int = keyword.length
}
