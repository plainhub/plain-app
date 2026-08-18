package com.ismartcoding.plain.services.screenmirror

import com.ismartcoding.plain.lib.screenmirror.VideoPacket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoPacketTest {

    @Test
    fun `magic byte is 0x56 at offset 0`() {
        val out = VideoPacket.encode(0, 0L, 0, byteArrayOf(0x65))
        assertEquals(0x56, out[0].toInt() and 0xFF)
    }

    @Test
    fun `flags are written at offset 1`() {
        val out = VideoPacket.encode(0, 0L, VideoPacket.FLAG_KEY_FRAME, byteArrayOf())
        assertEquals(0x01, out[1].toInt() and 0xFF)

        val out2 = VideoPacket.encode(0, 0L, (VideoPacket.FLAG_KEY_FRAME.toInt() or VideoPacket.FLAG_AUDIO.toInt()).toByte(), byteArrayOf())
        assertEquals(0x05, out2[1].toInt() and 0xFF)
    }

    @Test
    fun `frameId is big-endian uint32 at offset 2-5`() {
        val out = VideoPacket.encode(0x12345678, 0L, 0, byteArrayOf())
        assertEquals(0x12, out[2].toInt() and 0xFF)
        assertEquals(0x34, out[3].toInt() and 0xFF)
        assertEquals(0x56, out[4].toInt() and 0xFF)
        assertEquals(0x78, out[5].toInt() and 0xFF)
    }

    @Test
    fun `frameId 0 is all zeros`() {
        val out = VideoPacket.encode(0, 0L, 0, byteArrayOf())
        for (i in 2..5) assertEquals(0, out[i].toInt() and 0xFF)
    }

    @Test
    fun `frameId max uint32 wraps correctly`() {
        val out = VideoPacket.encode(-1, 0L, 0, byteArrayOf())
        assertEquals(0xFF, out[2].toInt() and 0xFF)
        assertEquals(0xFF, out[3].toInt() and 0xFF)
        assertEquals(0xFF, out[4].toInt() and 0xFF)
        assertEquals(0xFF, out[5].toInt() and 0xFF)
    }

    @Test
    fun `timestamp is big-endian int64 at offset 6-13`() {
        val ts = 0x0102030405060708L
        val out = VideoPacket.encode(0, ts, 0, byteArrayOf())
        assertEquals(0x01, out[6].toInt() and 0xFF)
        assertEquals(0x02, out[7].toInt() and 0xFF)
        assertEquals(0x03, out[8].toInt() and 0xFF)
        assertEquals(0x04, out[9].toInt() and 0xFF)
        assertEquals(0x05, out[10].toInt() and 0xFF)
        assertEquals(0x06, out[11].toInt() and 0xFF)
        assertEquals(0x07, out[12].toInt() and 0xFF)
        assertEquals(0x08, out[13].toInt() and 0xFF)
    }

    @Test
    fun `timestamp 0 is all zeros`() {
        val out = VideoPacket.encode(0, 0L, 0, byteArrayOf())
        for (i in 6..13) assertEquals(0, out[i].toInt() and 0xFF)
    }

    @Test
    fun `negative timestamp uses two complement encoding`() {
        val out = VideoPacket.encode(0, -1L, 0, byteArrayOf())
        for (i in 6..13) assertEquals(0xFF, out[i].toInt() and 0xFF)
    }

    @Test
    fun `payload is copied at offset 14`() {
        val data = byteArrayOf(0x65.toByte(), 0x88.toByte(), 0x80.toByte(), 0x40)
        val out = VideoPacket.encode(1, 1000L, VideoPacket.FLAG_KEY_FRAME, data)
        assertEquals(VideoPacket.HEADER_SIZE + 4, out.size)
        assertEquals(0x65, out[14].toInt() and 0xFF)
        assertEquals(0x88, out[15].toInt() and 0xFF)
        assertEquals(0x80, out[16].toInt() and 0xFF)
        assertEquals(0x40, out[17].toInt() and 0xFF)
    }

    @Test
    fun `empty payload produces header-only buffer`() {
        val out = VideoPacket.encode(0, 0L, 0, byteArrayOf())
        assertEquals(VideoPacket.HEADER_SIZE, out.size)
    }

    @Test
    fun `output does not alias input data`() {
        val data = byteArrayOf(0xAA.toByte(), 0xBB.toByte())
        val out = VideoPacket.encode(0, 0L, 0, data)
        data[0] = 0x00
        assertEquals(0xAA, out[14].toInt() and 0xFF)
    }

    @Test
    fun `HEADER_SIZE is 14`() {
        assertEquals(14, VideoPacket.HEADER_SIZE)
    }

    @Test
    fun `flag constants are bit-disjoint`() {
        assertTrue(VideoPacket.FLAG_KEY_FRAME.toInt() and VideoPacket.FLAG_CONFIG.toInt() == 0)
        assertTrue(VideoPacket.FLAG_KEY_FRAME.toInt() and VideoPacket.FLAG_AUDIO.toInt() == 0)
        assertTrue(VideoPacket.FLAG_CONFIG.toInt() and VideoPacket.FLAG_AUDIO.toInt() == 0)
    }
}
