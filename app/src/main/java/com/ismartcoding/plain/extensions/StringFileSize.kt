package com.ismartcoding.plain.extensions

fun String.parseSizeToBytes(): Long? {
    val s = trim().replace("_", "").replace(",", "")
    if (s.isEmpty()) return null

    val m = Regex("(?i)^([0-9]+(?:\\.[0-9]+)?)\\s*([kmgtp]?i?b?|[kmgtp])?$").matchEntire(s) ?: return null
    val number = m.groupValues[1].toDoubleOrNull() ?: return null
    val unit = m.groupValues.getOrNull(2)?.lowercase() ?: ""

    val multiplier = when (unit) {
        "", "b" -> 1.0
        "k", "kb", "kib" -> 1024.0
        "m", "mb", "mib" -> 1024.0 * 1024.0
        "g", "gb", "gib" -> 1024.0 * 1024.0 * 1024.0
        "t", "tb", "tib" -> 1024.0 * 1024.0 * 1024.0 * 1024.0
        "p", "pb", "pib" -> 1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0
        else -> return null
    }

    val v = number * multiplier
    if (v.isNaN() || v.isInfinite()) return null
    return v.toLong()
}
