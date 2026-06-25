package com.ismartcoding.plain.lib.helpers

import com.ismartcoding.plain.lib.extensions.times
import java.nio.ByteBuffer
import java.util.*

object StringHelper {
    fun shortUUID(): String {
        return ByteBuffer.wrap(UUID.randomUUID().toString().toByteArray()).long.toString(Character.MAX_RADIX)
    }

    fun getQuestionMarks(size: Int) = ("?," * size).trimEnd(',')
}
