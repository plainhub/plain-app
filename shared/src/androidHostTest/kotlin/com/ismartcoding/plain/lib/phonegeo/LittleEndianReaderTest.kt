package com.ismartcoding.plain.lib.phonegeo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Unit tests for [LittleEndianReader] — the pure-Kotlin little-endian byte
 * reader that replaced `java.nio.ByteBuffer` for KMP phone-geo index parsing.
 */
class LittleEndianReaderTest {

    @Test
    fun capacityAndLimitReflectBackingArray() {
        val reader = LittleEndianReader(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        assertEquals(4, reader.capacity)
        assertEquals(4, reader.limit)
    }

    @Test
    fun getIntReadsFourBytesLittleEndian() {
        // Bytes 0x01 0x02 0x03 0x04 => LE int 0x04030201
        val reader = LittleEndianReader(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        assertEquals(0x04030201, reader.getInt())
    }

    @Test
    fun getIntAtExplicitOffsetDoesNotMovePosition() {
        val reader = LittleEndianReader(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08))
        val v = reader.getInt(2)
        // LE 4 bytes from offset 2: 0x03 0x04 0x05 0x06 => 0x06050403
        assertEquals(0x06050403, v)
        assertEquals(0, reader.position, "getInt(offset) must not advance the position")
    }

    @Test
    fun getIntAdvancesPositionByFour() {
        val reader = LittleEndianReader(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08))
        assertEquals(0x04030201, reader.getInt())
        assertEquals(4, reader.position)
        assertEquals(0x08070605, reader.getInt())
        assertEquals(8, reader.position)
    }

    @Test
    fun getReadsSingleByte() {
        val reader = LittleEndianReader(byteArrayOf(0x10, 0x20, 0x30))
        assertEquals(0x10.toByte(), reader.get())
        assertEquals(1, reader.position)
        assertEquals(0x20.toByte(), reader.get())
        assertEquals(2, reader.position)
    }

    @Test
    fun getNegativeBytePreserved() {
        // 0xFF as signed byte = -1
        val reader = LittleEndianReader(byteArrayOf(0xFF.toByte()))
        assertEquals(-1, reader.get().toInt())
    }

    @Test
    fun getIntoBufferCopiesRange() {
        val reader = LittleEndianReader(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05))
        val dst = ByteArray(3)
        reader.get(dst)
        assertEquals(3, reader.position)
        assertEquals(0x01.toByte(), dst[0])
        assertEquals(0x02.toByte(), dst[1])
        assertEquals(0x03.toByte(), dst[2])
    }

    @Test
    fun getIntoBufferWithOffsetAndLength() {
        val reader = LittleEndianReader(byteArrayOf(0x10, 0x20, 0x30, 0x40, 0x50))
        val dst = ByteArray(5) { 0 }
        reader.get(dst, offset = 1, length = 3)
        assertEquals(3, reader.position)
        assertEquals(0.toByte(), dst[0])
        assertEquals(0x10.toByte(), dst[1])
        assertEquals(0x20.toByte(), dst[2])
        assertEquals(0x30.toByte(), dst[3])
        assertEquals(0.toByte(), dst[4])
    }

    @Test
    fun copyReturnsIndependentReader() {
        val reader = LittleEndianReader(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        reader.getInt() // advance to 4
        val copy = reader.copy()
        // Copy shares the same backing data but independent position
        assertEquals(0, copy.position)
        assertNotEquals(reader.position, copy.position)
        assertEquals(0x04030201, copy.getInt())
    }

    @Test
    fun positionIsMutable() {
        val reader = LittleEndianReader(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        reader.position = 2
        assertEquals(2, reader.position)
        // Reading now should start at index 2 — but only 2 bytes remain so
        // getInt() would read OOB; verify get() instead.
        assertEquals(0x03.toByte(), reader.get())
    }

    @Test
    fun getIntWithHighBitSetReturnsNegativeInt() {
        // 0xFF FF FF FF => -1 in two's complement
        val reader = LittleEndianReader(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))
        assertEquals(-1, reader.getInt())
    }
}
