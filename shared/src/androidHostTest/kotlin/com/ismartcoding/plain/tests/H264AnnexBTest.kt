package com.ismartcoding.plain.tests

import com.ismartcoding.plain.services.screenmirror.H264AnnexB
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class H264AnnexBTest {

    private fun be32(n: Int): ByteArray =
        byteArrayOf(((n shr 24) and 0xFF).toByte(), ((n shr 16) and 0xFF).toByte(),
            ((n shr 8) and 0xFF).toByte(), (n and 0xFF).toByte())

    private fun avccOf(vararg nals: ByteArray): ByteArray {
        val out = ArrayList<Byte>()
        for (nal in nals) {
            for (b in be32(nal.size)) out.add(b)
            for (b in nal) out.add(b)
        }
        return out.toByteArray()
    }

    private fun annexBOf(vararg nals: ByteArray): ByteArray {
        val sc = H264AnnexB.START_CODE_4
        val out = ArrayList<Byte>()
        for (nal in nals) {
            for (b in sc) out.add(b)
            for (b in nal) out.add(b)
        }
        return out.toByteArray()
    }

    private fun b(vararg ints: Int): ByteArray = ByteArray(ints.size) { ints[it].toByte() }

    // ── avccToAnnexB ─────────────────────────────────────────────────────────

    @Test
    fun `avccToAnnexB converts a single NAL`() {
        val idr = b(0x65, 0x88, 0x80, 0x40)
        val out = H264AnnexB.avccToAnnexB(avccOf(idr))
        assertArrayEquals(annexBOf(idr), out)
    }

    @Test
    fun `avccToAnnexB converts multiple NALs with start codes between them`() {
        val aud = b(0x09, 0xF0)
        val idr = b(0x65, 0x88, 0x80, 0x40, 0x00)
        val out = H264AnnexB.avccToAnnexB(avccOf(aud, idr))
        assertArrayEquals(annexBOf(aud, idr), out)
    }

    @Test
    fun `avccToAnnexB truncates when length overruns the buffer`() {
        // declared length 100 but only 4 payload bytes follow
        val raw = b(0, 0, 0, 100, 0x65, 0x88, 0x80, 0x40)
        val out = H264AnnexB.avccToAnnexB(raw)
        assertTrue("got ${out.size}", out.isEmpty())
    }

    @Test
    fun `avccToAnnexB truncates when length is zero`() {
        val raw = b(0, 0, 0, 0, 0x65, 0x88)
        val out = H264AnnexB.avccToAnnexB(raw)
        assertEquals(0, out.size)
    }

    @Test
    fun `avccToAnnexB on empty input returns empty`() {
        assertEquals(0, H264AnnexB.avccToAnnexB(ByteArray(0)).size)
    }

    @Test
    fun `avccToAnnexB handles input shorter than 4 bytes`() {
        val out = H264AnnexB.avccToAnnexB(b(0, 0))
        assertEquals(0, out.size)
    }

    // ── ensureStartCode ──────────────────────────────────────────────────────

    @Test
    fun `ensureStartCode leaves a 4-byte-start-coded buffer untouched`() {
        val buf = b(0, 0, 0, 1, 0x67, 0x42, 0xC0, 0x1E)
        val out = H264AnnexB.ensureStartCode(buf)
        assertArrayEquals(buf, out)
        assertTrue(buf === out)
    }

    @Test
    fun `ensureStartCode upgrades a 3-byte start code to 4 bytes`() {
        val buf = b(0, 0, 1, 0x67, 0x42)
        val out = H264AnnexB.ensureStartCode(buf)
        assertArrayEquals(b(0, 0, 0, 1, 0x67, 0x42), out)
    }

    @Test
    fun `ensureStartCode prepends a start code when missing`() {
        val buf = b(0x67, 0x42, 0xC0, 0x1E)
        val out = H264AnnexB.ensureStartCode(buf)
        assertArrayEquals(b(0, 0, 0, 1, 0x67, 0x42, 0xC0, 0x1E), out)
    }

    // ── joinSpsPps ──────────────────────────────────────────────────────────

    @Test
    fun `joinSpsPps concatenates SPS and PPS with start codes`() {
        val sps = b(0, 0, 0, 1, 0x67, 0x42, 0xC0, 0x1E)
        val pps = b(0x68, 0xCE, 0x38, 0x80)
        val out = H264AnnexB.joinSpsPps(sps, pps)
        // ensureStartCode on pps adds 4 bytes, so the blob is sps.size + 4 + pps.size
        assertEquals(sps.size + 4 + pps.size, out.size)
        assertArrayEquals(sps, out.copyOfRange(0, sps.size))
        assertArrayEquals(b(0, 0, 0, 1, 0x68, 0xCE, 0x38, 0x80),
            out.copyOfRange(sps.size, out.size))
    }
}
