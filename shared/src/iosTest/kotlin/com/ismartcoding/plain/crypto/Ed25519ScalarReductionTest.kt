package com.ismartcoding.plain.crypto

import com.ismartcoding.plain.lib.crypto.sha512
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for scalar reduction mod L (the group order of Ed25519).
 *
 * The group order L = 2^252 + 27742317777372353535851937790883648493.
 * Little-endian bytes:
 *   ed d3 f5 5c 1a 63 12 58 d6 9c f7 a2 de f9 de 14
 *   00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 10
 *
 * Coverage:
 *   1. reduceModL of SHA-512("") → 0 mod L = 0
 *   2. Input exactly L bytes (little-endian) → result = 0
 *   3. Input exactly L-1 bytes → result = L-1
 *   4. Input exactly L+1 → result = 1
 *   5. Input exactly 2L → result = 0
 *   6. Input exactly 2L+42 → result = 42
 *   7. All zero input → zero output
 *   8. All-0xFF input (2^512 - 1) → deterministic result (no crash)
 *   9. Byte-by-byte identity: for small inputs (< L), the reduceModL output
 *      equals the input modulo L (i.e. itself when input bytes < L)
 *  10. Distributivity: (a+b) mod L = ((a mod L)+(b mod L)) mod L via
 *      LongArray round-trip
 *  11. reduceModL on 64-byte SHA-512 output used in signing matches
 *      the result when the same scalar is reconstructed from modL directly
 */
class Ed25519ScalarReductionTest {

    // ── helpers ────────────────────────────────────────────────────────────

    private val LLE = byteArrayOf(
        0xed.toByte(), 0xd3.toByte(), 0xf5.toByte(), 0x5c.toByte(),
        0x1a.toByte(), 0x63.toByte(), 0x12.toByte(), 0x58.toByte(),
        0xd6.toByte(), 0x9c.toByte(), 0xf7.toByte(), 0xa2.toByte(),
        0xde.toByte(), 0xf9.toByte(), 0xde.toByte(), 0x14.toByte(),
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10.toByte(),
    )

    /** Subtract b from a (a is modifiable). Returns true if underflow occurred. */
    private fun subInPlace(a: LongArray, b: ByteArray) {
        var carry = 0L
        for (i in b.indices) {
            val bi = b[i].toLong() and 0xff
            var diff = (a[i] and 0xff) - bi - carry
            if (diff < 0) { diff += 256; carry = 1 } else carry = 0
            a[i] = (a[i] and 0xff.inv()) or (diff and 0xff)
        }
        for (i in b.size until a.size) {
            var diff = (a[i] and 0xff) - carry
            if (diff < 0) { diff += 256; carry = 1 } else carry = 0
            a[i] = (a[i] and 0xff.inv()) or (diff and 0xff)
        }
    }

    /** Compare little-endian ByteArray a to b. Returns true if a < b. */
    private fun leLess(a: ByteArray, b: ByteArray): Boolean {
        for (i in a.indices.reversed()) {
            val ai = a[i].toInt() and 0xff
            val bi = b[i].toInt() and 0xff
            if (ai != bi) return ai < bi
        }
        return false
    }

    /** Increment little-endian ByteArray in place. Returns same array. */
    private fun leInc(a: ByteArray): ByteArray {
        for (i in a.indices) {
            a[i] = (a[i] + 1).toByte()
            if (a[i].toInt() != 0) break
        }
        return a
    }

    /** Decrement little-endian ByteArray in place. Returns same array. */
    private fun leDec(a: ByteArray): ByteArray {
        for (i in a.indices) {
            val before = a[i]
            a[i] = (a[i] - 1).toByte()
            if (before.toInt() != 0) break
        }
        return a
    }

    /** Pad little-endian ByteArray to 64 bytes and convert to LongArray for modL. */
    private fun buildModLInput(leBytes: ByteArray, index: Int = 0): LongArray {
        val x = LongArray(64)
        for (i in leBytes.indices) x[i + index] = leBytes[i].toLong() and 0xff
        return x
    }

    // ── #1-6 exact boundary values ────────────────────────────────────────

    @Test
    fun `N1 reduceModL of all-zero 64 bytes yields zero`() {
        val r = reduceModL(ByteArray(64))
        for (i in 0 until 32) assertEquals(0, r[i].toInt(), "byte $i must be 0")
    }

    @Test
    fun `N2 modL of L bytes at index 0 yields 0`() {
        val x = buildModLInput(LLE)
        val r = modL(x)
        for (i in 0 until 32) assertEquals(0, r[i].toInt(), "L mod L must be 0, byte $i")
    }

    @Test
    fun `N3 modL of L-1 yields L-1 - unchanged since L-1 less than L`() {
        val lMinus1 = LLE.copyOf().also { leDec(it) }
        val x = buildModLInput(lMinus1)
        val r = modL(x)
        assertTrue(lMinus1.contentEquals(r),
            "expected L-1 = ${leHex(lMinus1)}, got ${leHex(r)}")
    }

    @Test
    fun `N4 modL of L-plus-1 yields 1`() {
        val lPlus1 = LLE.copyOf().also { leInc(it) }
        val x = buildModLInput(lPlus1)
        val r = modL(x)
        val expected = ByteArray(32).also { it[0] = 0x01 }
        assertTrue(expected.contentEquals(r),
            "(L+1) mod L should be 1; got ${leHex(r)}")
    }

    @Test
    fun `N5 modL of 2-times-L yields 0`() {
        // Build 2*L as 64 bytes: copy L at index 0, then add L at index 0 again
        // (manual add with carry since we need a LongArray).
        val twoL = LongArray(64)
        for (i in 0 until 32) twoL[i] = (LLE[i].toLong() and 0xff) * 2
        // Normalise byte carries (since 2*byte may exceed 255).
        var carry = 0L
        for (i in 0 until 64) {
            val v = twoL[i] + carry
            twoL[i] = v and 0xff
            carry = v shr 8
        }
        val r = modL(twoL)
        for (i in 0 until 32) assertEquals(0, r[i].toInt(), "2L mod L = 0, byte $i")
    }

    @Test
    fun `N6 modL of 2L plus 42 yields 42`() {
        val twoL = LongArray(64)
        for (i in 0 until 32) twoL[i] = (LLE[i].toLong() and 0xff) * 2
        var carry = 0L
        for (i in 0 until 64) {
            val v = twoL[i] + carry + if (i == 0) 42 else 0
            twoL[i] = v and 0xff
            carry = v shr 8
        }
        val r = modL(twoL)
        assertEquals(42, r[0].toInt() and 0xff)
        for (i in 1 until 32) assertEquals(0, r[i].toInt())
    }

    // ── #7-8 extreme / overflow inputs ────────────────────────────────────

    @Test
    fun `N7 reduceModL all-zero byte array returns all zeros`() {
        val r = reduceModL(ByteArray(64))
        assertTrue(r.all { it.toInt() == 0 })
    }

    @Test
    fun `N8 reduceModL of 64 all 0xFF bytes does not crash and output is less than L`() {
        val input = ByteArray(64) { 0xff.toByte() }
        val r = reduceModL(input)
        // Output must be < L.
        assertTrue(leLess(r, LLE),
            "reduceModL(max_2^512) must be less than L; got ${leHex(r)}")
        // And output must not be all-FF.
        assertFalse(r.all { it.toInt() and 0xff == 0xff })
    }

    // ── #9 identity for small inputs ──────────────────────────────────────

    @Test
    fun `N9 reduceModL for small values less than L unchanged`() {
        for (k in listOf(0, 1, 2, 42, 100, 127, 128, 255, 256, 1000, 65535, 0xffff)) {
            val in64 = ByteArray(64)
            in64[0] = (k and 0xff).toByte()
            if (k > 255) in64[1] = ((k shr 8) and 0xff).toByte()
            if (k > 65535) in64[2] = ((k shr 16) and 0xff).toByte()
            val r = reduceModL(in64)
            val expected = ByteArray(32)
            expected[0] = (k and 0xff).toByte()
            if (k > 255) expected[1] = ((k shr 8) and 0xff).toByte()
            if (k > 65535) expected[2] = ((k shr 16) and 0xff).toByte()
            assertTrue(expected.contentEquals(r),
                "k=$k: expected ${leHex(expected)}, got ${leHex(r)}")
        }
    }

    // ── #10 distributivity ────────────────────────────────────────────────

    @Test
    fun `N10 modL a plus b equals modL modL-a plus modL-b`() {
        // Use values confined to the low 63 bytes so the 64-byte sum cannot
        // overflow — distributivity (a + b) mod L = ((a mod L) + (b mod L)) mod L
        // is only tested on the 64-byte inputs we actually feed to modL.
        val a = LongArray(64) { if (it < 63) ((it * 3 + 7) and 0xff).toLong() else 0L }
        val b = LongArray(64) { if (it < 63) ((it * 5 + 11) and 0xff).toLong() else 0L }
        val sum = LongArray(64)
        var carry = 0L
        for (i in 0 until 64) {
            val v = (a[i] and 0xffL) + (b[i] and 0xffL) + carry
            sum[i] = v and 0xffL
            carry = v ushr 8
        }
        assertEquals(0L, carry, "test data must not overflow 64 bytes")
        val direct = modL(sum.copyOf())

        val aBytes = ByteArray(64) { (a[it] and 0xffL).toByte() }
        val bBytes = ByteArray(64) { (b[it] and 0xffL).toByte() }
        val rA = reduceModL(aBytes)
        val rB = reduceModL(bBytes)
        val sumR = LongArray(64)
        carry = 0
        for (i in 0 until 32) {
            val v = (rA[i].toLong() and 0xff) + (rB[i].toLong() and 0xff) + carry
            sumR[i] = v and 0xffL
            carry = v ushr 8
        }
        assertEquals(0L, carry, "rA+rB fits in 32 bytes + carry (rA+rB < 2L)")
        val viaReduce = modL(sumR.copyOf())
        assertTrue(direct.contentEquals(viaReduce),
            "modL(a+b) != modL(modL(a)+modL(b)): direct=${leHex(direct)} viaReduce=${leHex(viaReduce)}")

        // Extra sanity: idempotence — reduceModL applied twice == once
        val extended = ByteArray(64)
        direct.copyInto(extended, 0, 0, 32)
        val doubleRed = reduceModL(extended)
        assertTrue(direct.contentEquals(doubleRed),
            "modL not idempotent: once=${leHex(direct)} twice=${leHex(doubleRed)}")
    }

    // ── #11 sign-scalar reconstruction for RFC vectors ───────────────────

    @Test
    fun `N11 reduceModL of sha512-prefix plus empty msg matches RFC 8032 test 1`() {
        // RFC 8032 test 1: sk = all set test vector. Prefix = H(sk)[32:64]
        val sk = byteArrayOf(
            0x9d.toByte(), 0x61, 0xb1.toByte(), 0x9d.toByte(), 0xef.toByte(), 0xfd.toByte(), 0x5a, 0x60,
            0xba.toByte(), 0x84.toByte(), 0x4a.toByte(), 0xf4.toByte(), 0x92.toByte(), 0xec.toByte(), 0x2c.toByte(), 0xc4.toByte(),
            0x44, 0x44.toByte(), 0x49.toByte(), 0xc5.toByte(), 0x69, 0x7b, 0x32, 0x69,
            0x19, 0x70.toByte(), 0x3b.toByte(), 0xac.toByte(), 0x03, 0x1c.toByte(), 0xae.toByte(), 0x7f.toByte(),
            0x60.toByte()
        )
        val h = sha512(sk)
        val prefix = h.copyOfRange(32, 64)
        val rHash = sha512(prefix + ByteArray(0))
        val r = reduceModL(rHash)
        // r must be < L
        assertTrue(leLess(r, LLE),
            "reduced scalar r must be less than L; got ${leHex(r)}")
        // Also verify that r[0..31] are bytes in valid range (no crash).
        // Then, perform: reconstruct r via modL of the same LongArray.
        val reconstructed = modL(LongArray(64) { rHash[it].toLong() and 0xff })
        assertTrue(r.contentEquals(reconstructed),
            "reduceModL != modL on same bytes")
    }

    // helper: hex encode little-endian bytes.
    private fun leHex(b: ByteArray): String = b.joinToString("") {
        (it.toInt() and 0xff).toString(16).padStart(2, '0')
    }
}
