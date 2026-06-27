package com.ismartcoding.plain.lib.pinyin

import com.ismartcoding.plain.lib.ahocorasick.trie.Trie

internal object Utils {
    fun dictsToTrie(pinyinDicts: List<PinyinDict>): Trie? {
        val all: MutableSet<String> = HashSet()
        for (dict in pinyinDicts) {
            all.addAll(dict.words())
        }
        if (all.isEmpty()) return null
        val trie = Trie()
        for (key in all) {
            trie.addKeyword(key)
        }
        return trie
    }
}
