package com.ismartcoding.plain.lib.yjs

import kotlin.math.min

/**
 * Low-level binary decoder for lib0 encoding format used by Yjs V1 updates.
 *
 * Implements: readVarUint, readVarInt, readVarString, readUint8, readAny.
 */
class YjsDecoder(private val data: ByteArray) {
    private var pos = 0

    fun eof(): Boolean = pos >= data.size

    fun readUint8(): Int {
        return data[pos++].toInt() and 0xFF
    }

    fun readVarUint(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            if (pos >= data.size) throw IllegalStateException("Unexpected end of data in readVarUint")
            val b = data[pos++].toInt() and 0xFF
            result = result or ((b and 0x7F).toLong() shl shift)
            if ((b and 0x80) == 0) break
            shift += 7
            if (shift > 63) throw IllegalStateException("VarUint too large")
        }
        return result
    }

    fun readVarInt(): Long {
        val firstByte = readUint8()
        var num = (firstByte and 0x3F).toLong()
        val sign = if ((firstByte and 0x40) != 0) -1L else 1L
        if ((firstByte and 0x80) == 0) {
            return sign * num
        }
        var mult = 64L
        while (true) {
            if (pos >= data.size) throw IllegalStateException("Unexpected end of data in readVarInt")
            val b = data[pos++].toInt() and 0xFF
            num += (b and 0x7F).toLong() * mult
            mult *= 128
            if ((b and 0x80) == 0) break
        }
        return sign * num
    }

    fun readVarString(): String {
        val len = readVarUint().toInt()
        if (len == 0) return ""
        val end = pos + len
        if (end > data.size) throw IllegalStateException("Unexpected end of data in readVarString")
        val str = data.decodeToString(pos, end)
        pos = end
        return str
    }

    fun readVarUint8Array(): ByteArray {
        val len = readVarUint().toInt()
        val end = pos + len
        if (end > data.size) throw IllegalStateException("Unexpected end of data in readVarUint8Array")
        val arr = data.copyOfRange(pos, end)
        pos = end
        return arr
    }

    fun readFloat32(): Float {
        val bits = readUint32BE()
        return Float.fromBits(bits)
    }

    fun readFloat64(): Double {
        val bits = readUint64BE()
        return Double.fromBits(bits)
    }

    private fun readUint32BE(): Int {
        val b0 = data[pos++].toInt() and 0xFF
        val b1 = data[pos++].toInt() and 0xFF
        val b2 = data[pos++].toInt() and 0xFF
        val b3 = data[pos++].toInt() and 0xFF
        return (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
    }

    private fun readUint64BE(): Long {
        val hi = readUint32BE().toLong() and 0xFFFFFFFFL
        val lo = readUint32BE().toLong() and 0xFFFFFFFFL
        return (hi shl 32) or lo
    }

    /**
     * Read a value using lib0's "any" encoding.
     * lib0 uses an inverted lookup: readAnyLookupTable[127 - type].
     * Type tags (byte values):
     *   127 = undefined, 126 = null, 125 = integer, 124 = float32,
     *   123 = float64, 122 = bigint, 121 = false, 120 = true,
     *   119 = string, 118 = object, 117 = array, 116 = Uint8Array
     */
    fun readAny(): Any? {
        val type = readUint8()
        return when (type) {
            127 -> null  // undefined
            126 -> null  // null
            125 -> readVarInt()  // integer
            124 -> readFloat32()  // float32
            123 -> readFloat64()  // float64
            121 -> false  // boolean false
            120 -> true   // boolean true
            119 -> readVarString()  // string
            118 -> {  // object<string,any>
                val obj = mutableMapOf<String, Any?>()
                val numKeys = readVarUint().toInt()
                for (i in 0 until numKeys) {
                    val key = readVarString()
                    obj[key] = readAny()
                }
                obj
            }
            117 -> {  // array<any>
                val arr = mutableListOf<Any?>()
                val numVals = readVarUint().toInt()
                for (i in 0 until numVals) {
                    arr.add(readAny())
                }
                arr
            }
            116 -> readVarUint8Array()  // Uint8Array
            else -> throw IllegalStateException("Unknown readAny type tag: $type at pos ${pos - 1}")
        }
    }

    fun readBytes(n: Int): ByteArray {
        val end = pos + n
        if (end > data.size) throw IllegalStateException("Unexpected end of data")
        val arr = data.copyOfRange(pos, end)
        pos = end
        return arr
    }

    fun remaining(): Int = data.size - pos
}
