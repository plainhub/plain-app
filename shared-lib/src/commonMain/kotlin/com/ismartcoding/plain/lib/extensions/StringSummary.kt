package com.ismartcoding.plain.lib.extensions

fun String.getSummary(): String {
    val regex = Regex("!\\[.*?\\]\\(.*?\\)|!\\[.*?\\]\\[.*?\\]|<img.*?>", RegexOption.IGNORE_CASE)
    return replace(regex, "🖼").replace("\n", "").replaceFirst("^\\s*".toRegex(), "")
}
