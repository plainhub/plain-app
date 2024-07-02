package com.ismartcoding.lib.pinyin

import com.ismartcoding.lib.ahocorasick.trie.Trie
import java.util.*

internal object Utils {
    fun dictsToTrie(pinyinDicts: List<PinyinDict>): Trie? {
        val all: MutableSet<String> = TreeSet()
        val builder = Trie.builder()
        for (dict in pinyinDicts) {
            all.addAll(dict.words())
        }
        if (all.size > 0) {
            for (key in all) {
                builder.addKeyword(key)
            }
            return builder.build()
        }
        return null
    }
}
