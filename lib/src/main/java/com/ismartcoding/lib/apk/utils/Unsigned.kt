package com.ismartcoding.lib.apk.utils

/**
 * Unsigned utils, for compatible with java6/java7.
 */
object Unsigned {
    @JvmStatic
    fun toLong(value: Int): Long {
        return value.toLong() and 0xffffffffL
    }

    @JvmStatic
    fun toUInt(value: Long): Int {
        return value.toInt()
    }

    @JvmStatic
    fun toInt(value: Short): Int {
        return value.toInt() and 0xffff
    }

    @JvmStatic
    fun toUShort(value: Int): Short {
        return value.toShort()
    }

    @JvmStatic
    fun ensureUInt(value: Long): Int {
        if (value < 0 || value > Int.MAX_VALUE) {
            throw ArithmeticException("unsigned integer overflow")
        }
        return value.toInt()
    }

    @JvmStatic
    fun ensureULong(value: Long): Long {
        if (value < 0) {
            throw ArithmeticException("unsigned long overflow")
        }
        return value
    }

    @JvmStatic
    fun toShort(value: Byte): Short {
        return (value.toInt() and 0xff).toShort()
    }

    @JvmStatic
    fun toUByte(value: Short): Byte {
        return value.toByte()
    }
}