package com.ismartcoding.lib.pinyin

import java.util.HashMap

class PinyinRules {
    private val mOverrides: MutableMap<String, Array<String>> = HashMap()

    fun add(
        c: Char,
        pinyin: String,
    ): PinyinRules {
        mOverrides[c.toString()] = arrayOf(pinyin)
        return this
    }

    fun add(
        str: String,
        pinyin: String,
    ): PinyinRules {
        mOverrides[str] = arrayOf(pinyin)
        return this
    }

    fun toPinyin(c: Char): String {
        return mOverrides[c.toString()]!![0]
    }

    fun toPinyinMapDict(): PinyinMapDict {
        return object : PinyinMapDict() {
            override fun mapping(): Map<String, Array<String>> {
                return mOverrides
            }
        }
    }
}
