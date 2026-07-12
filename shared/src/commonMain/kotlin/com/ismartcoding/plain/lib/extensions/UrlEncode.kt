package com.ismartcoding.plain.lib.extensions

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
