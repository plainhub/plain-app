package com.ismartcoding.plain.helpers

object StringHelper {
    fun getQuestionMarks(size: Int) = ("?," * size).trimEnd(',')
}

private operator fun String.times(x: Int): String {
    val sb = StringBuilder(length * x)
    repeat(x) { sb.append(this) }
    return sb.toString()
}
