package com.ismartcoding.plain.helpers

import java.nio.ByteBuffer
import java.util.UUID

object StringHelper {
    fun shortUUID(): String {
        return ByteBuffer.wrap(UUID.randomUUID().toString().toByteArray()).long.toString(Character.MAX_RADIX)
    }

    fun getQuestionMarks(size: Int) = ("?," * size).trimEnd(',')
}

private operator fun String.times(x: Int): String {
    val sb = StringBuilder(length * x)
    repeat(x) { sb.append(this) }
    return sb.toString()
}