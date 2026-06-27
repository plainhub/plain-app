package com.ismartcoding.plain.lib.extensions

import com.ismartcoding.plain.lib.pinyin.Pinyin

fun String.toSortName(): String {
    return Pinyin.toPinyin(this).lowercase()
}
