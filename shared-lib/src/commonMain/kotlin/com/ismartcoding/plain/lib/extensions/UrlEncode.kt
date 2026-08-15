package com.ismartcoding.plain.lib.extensions

import kotlin.text.iterator

/**
 * Decodes a percent-encoded string. All `%XX` bytes are collected first and then
 * decoded as a whole, so multi-byte UTF-8 sequences (e.g. Chinese) are restored
 * correctly. `+` is treated as a space. Returns the original string if the input
 * is not valid UTF-8 after decoding, so it never throws.
 */
fun String.urlDecode(): String {
    val bytes = mutableListOf<Byte>()
    var i = 0
    while (i < length) {
        val c = this[i]
        if (c == '%' && i + 2 < length) {
            val code = substring(i + 1, i + 3).toIntOrNull(16)
            if (code != null) {
                bytes.add(code.toByte())
                i += 3
                continue
            }
        }
        val byte = when {
            c == '+' -> ' '.code.toByte()
            c.code in 0x00..0x7F -> c.code.toByte()
            else -> null
        }
        byte?.let { bytes.add(it) }
        i++
    }
    return try {
        bytes.toByteArray().decodeToString()
    } catch (_: Exception) {
        this
    }
}

fun String.urlEncode(): String {
    return buildString {
        for (ch in this@urlEncode) {
            when (ch) {
                in 'a'..'z', in 'A'..'Z', in '0'..'9', '-', '_', '.', '~' -> append(ch)
                else -> {
                    val bytes = ch.toString().encodeToByteArray()
                    for (b in bytes) {
                        append('%')
                        val hex = (b.toInt() and 0xFF).toString(16).uppercase()
                        if (hex.length == 1) append('0')
                        append(hex)
                    }
                }
            }
        }
    }
}
