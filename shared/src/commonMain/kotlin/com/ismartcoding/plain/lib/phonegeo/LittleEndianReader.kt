package com.ismartcoding.plain.lib.phonegeo

/**
 * Minimal little-endian byte reader replacing [java.nio.ByteBuffer] for KMP.
 */
class LittleEndianReader(val data: ByteArray) {
    var position: Int = 0
    val capacity: Int get() = data.size
    val limit: Int get() = data.size

    fun getInt(): Int {
        val v = getInt(position)
        position += 4
        return v
    }

    fun getInt(offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
            ((data[offset + 1].toInt() and 0xFF) shl 8) or
            ((data[offset + 2].toInt() and 0xFF) shl 16) or
            ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    fun get(): Byte {
        val b = data[position]
        position++
        return b
    }

    fun get(
        bytes: ByteArray,
        offset: Int = 0,
        length: Int = bytes.size,
    ) {
        for (i in 0 until length) {
            bytes[offset + i] = data[position + i]
        }
        position += length
    }

    fun copy(): LittleEndianReader = LittleEndianReader(data)
}
