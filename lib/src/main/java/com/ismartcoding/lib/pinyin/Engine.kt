package com.ismartcoding.lib.pinyin

import com.ismartcoding.lib.ahocorasick.trie.Emit
import com.ismartcoding.lib.ahocorasick.trie.Trie
import com.ismartcoding.lib.pinyin.Pinyin.toPinyin
import com.ismartcoding.lib.pinyin.Utils.dictsToTrie
import java.util.*

/**
 * 字符串转拼音引擎，支持字典和[SegmentationSelector]
 */
internal object Engine {
    val EMIT_COMPARATOR = EmitComparator()

    fun toPinyin(
        inputStr: String,
        config: Pinyin.Config,
        separator: String,
    ): String {
        val pinyinDicts = Collections.unmodifiableList(config.pinyinDicts)
        val trie = dictsToTrie(config.pinyinDicts)
        val selector = config.selector
        return toPinyin(inputStr, trie, pinyinDicts, separator, selector)
    }

    fun toPinyin(
        inputStr: String,
        trie: Trie?,
        pinyinDictList: List<PinyinDict>?,
        separator: String,
        selector: SegmentationSelector?,
    ): String {
        if (inputStr.isEmpty()) {
            return inputStr
        }
        if (trie == null || selector == null) {
            // 没有提供字典或选择器，按单字符转换输出
            val resultPinyinStrBuf = StringBuffer()
            for (i in inputStr.indices) {
                resultPinyinStrBuf.append(toPinyin(inputStr[i]))
                if (i != inputStr.length - 1) {
                    resultPinyinStrBuf.append(separator)
                }
            }
            return resultPinyinStrBuf.toString()
        }
        val selectedEmits = selector.select(trie.parseText(inputStr))
        Collections.sort(selectedEmits, EMIT_COMPARATOR)
        val resultPinyinStrBuf = StringBuffer()
        var nextHitIndex = 0
        var i = 0
        while (i < inputStr.length) {
            // 首先确认是否有以第i个字符作为begin的hit
            if (nextHitIndex < selectedEmits.size && i == selectedEmits[nextHitIndex].start) {
                // 有以第i个字符作为begin的hit
                val fromDicts = pinyinFromDict(selectedEmits[nextHitIndex].keyword, pinyinDictList)
                for (j in fromDicts.indices) {
                    resultPinyinStrBuf.append(fromDicts[j].uppercase(Locale.getDefault()))
                    if (j != fromDicts.size - 1) {
                        resultPinyinStrBuf.append(separator)
                    }
                }
                i += selectedEmits[nextHitIndex].size()
                nextHitIndex++
            } else {
                // 将第i个字符转为拼音
                resultPinyinStrBuf.append(toPinyin(inputStr[i]))
                i++
            }
            if (i != inputStr.length) {
                resultPinyinStrBuf.append(separator)
            }
        }
        return resultPinyinStrBuf.toString()
    }

    private fun pinyinFromDict(
        wordInDict: String,
        pinyinDictSet: List<PinyinDict>?,
    ): Array<String> {
        if (pinyinDictSet != null) {
            for (dict in pinyinDictSet) {
                if (dict.words().contains(wordInDict)) {
                    return dict.toPinyin(wordInDict)
                }
            }
        }
        throw IllegalArgumentException("No pinyin dict contains word: $wordInDict")
    }

    internal class EmitComparator : Comparator<Emit> {
        override fun compare(
            o1: Emit,
            o2: Emit,
        ): Int {
            return if (o1.start == o2.start) {
                // 起点相同时，更长的排前面
                if (o1.size() < o2.size()) {
                    1
                } else if (o1.size() == o2.size()) {
                    0
                } else {
                    -1
                }
            } else {
                // 起点小的放前面
                if (o1.start < o2.start) {
                    -1
                } else if (o1.start == o2.start) {
                    0
                } else {
                    1
                }
            }
        }
    }
}
