package com.ismartcoding.plain.helpers

import kotlin.random.Random

object StringHelper {
    private const val RADIX = 36
    private val DIGITS = "0123456789abcdefghijklmnopqrstuvwxyz"

    fun shortUUID(): String {
        var value = Random.nextLong() and Long.MAX_VALUE
        if (value == 0L) return "0"
        val sb = StringBuilder()
        while (value > 0) {
            sb.append(DIGITS[(value % RADIX).toInt()])
            value /= RADIX
        }
        return sb.reverse().toString()
    }

    fun getQuestionMarks(size: Int) = ("?," * size).trimEnd(',')
}

private operator fun String.times(x: Int): String {
    val sb = StringBuilder(length * x)
    repeat(x) { sb.append(this) }
    return sb.toString()
}
