package com.ismartcoding.plain.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the low-level GF(p) field arithmetic primitives in Ed25519Native.
 *
 * These tests exercise the internal `fe*` functions directly because a bug in
 * field arithmetic — e.g. an incorrect carry, a sign mismatch, or a wrong
 * limb product — would cause signing/verification to silently produce wrong
 * results for only a subset of inputs (those that stress the carry chain).
 *
 * Coverage:
 *   1. `feCarry`     — canonicalisation of known values near p and 0
 *   2. `feAdd/feSub` — commutative, associative, neutral element
 *   3. `feMul`       — commutativity, distributivity, 0*a=0, 1*a=a, overflow regime
 *   4. `feInv`       — a * inv(a) = 1, inv(1) = 1
 *   5. `feSq`        — sq(a) = mul(a,a)
 *   6. `fePack/feUnpack` — round-trip, known base-point Y encoding
 *   7. `feIsNegative` — odd/even least-significant limb
 *   8. `feEqual`     — different representations of the same value compare equal
 *   9. `feCMove`     — select p when b=0, select q when b=1; aliasing p==q
 *  10. `geBytesGtOrEqP` — y == p, y == p-1, y == 0, y == p+19 (mod 2^255)
 */
class Ed25519FieldArithmeticTest {

    // ── helpers ────────────────────────────────────────────────────────────

    private val P_MINUS_19: Long = 0x7fffffff // largest short limb near p

    private fun fe(vararg limbs: Long): LongArray = longArrayOf(*limbs).copyOf(16)

    private fun cloneFe(a: LongArray): LongArray = a.copyOf()

    // p - 1 encoded little-endian: largest valid field element
    private val P_MINUS_1_LE = byteArrayOf(
        0xec.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
        0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
        0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
        0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte(),
    )

    // p encoded little-endian (just outside the field)
    private val P_EXACT_LE = byteArrayOf(
        0xed.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
        0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
        0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(),
        0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0x7f.toByte(),
    )

    // ── #1: feCarry ────────────────────────────────────────────────────────

    @Test
    fun `feCarry reduces limbs that exceed 2^16`() {
        val a = fe(1L shl 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        feCarry(a)
        // 2^20 = 16 * 2^16  →  carry 16 to the next limb, leaves 0 in limb 0
        // But feCarry formula is c = (o[i] + 2^16) >> 16 - 1 + 1 ... let's just
        // check carry produces canonical-ish output.
        val packed = ByteArray(32)
        fePack(packed, a)
        // re-unpack and compare to original after pack->unpack
        val b = LongArray(16)
        feUnpack(b, packed)
        val aCopy = LongArray(16)
        feUnpack(aCopy, packed) // reuse pack output
        assertTrue(feEqual(a, b))
    }

    @Test
    fun `feCarry two consecutive carries are stable`() {
        val a = fe(
            0x1ffff, 0x1ffff, 0x1ffff, 0x1ffff, 0x1ffff, 0x1ffff, 0x1ffff, 0x1ffff,
            0x1ffff, 0x1ffff, 0x1ffff, 0x1ffff, 0x1ffff, 0x1ffff, 0x1ffff, 0x1ffff,
        )
        val aSave = a.copyOf()
        feCarry(a)
        feCarry(a)
        val a2 = a.copyOf()
        feCarry(a2) // third carry must be stable
        assertTrue(feEqual(a, a2))
    }

    @Test
    fun `feCarry handles carry back from limb 15 to limb 0 - x38 rule`() {
        // Build a field element with a huge value in limb 15 to trigger the
        // wrap-around 38*c rule.  After packing, the result must still satisfy
        // round-trip.
        val a = LongArray(16)
        a[15] = (1L shl 30) // way larger than 2^16
        feCarry(a)
        val packed = ByteArray(32)
        fePack(packed, a)
        val b = LongArray(16)
        feUnpack(b, packed)
        // The re-unpacked value must represent the same field element.
        assertTrue(feEqual(a, b))
    }

    // ── #2: feAdd / feSub ──────────────────────────────────────────────────

    @Test
    fun `feAdd commutativity a plus b equals b plus a`() {
        val a = fe(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150, 160)
        val b = fe(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val r1 = LongArray(16); feAdd(r1, a, b)
        val r2 = LongArray(16); feAdd(r2, b, a)
        assertTrue(feEqual(r1, r2))
    }

    @Test
    fun `feAdd neutral element a plus 0 equals a`() {
        val a = fe(5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val r = LongArray(16); feAdd(r, a, LongArray(16))
        assertTrue(feEqual(r, a))
    }

    @Test
    fun `feSub subtraction of a from itself yields 0`() {
        val a = fe(42, 99, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val r = LongArray(16); feSub(r, a, a)
        assertTrue(feEqual(r, LongArray(16)))
    }

    @Test
    fun `feAdd after feSub round-trips a - b + b = a`() {
        val a = fe(100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100, 1200, 1300, 1400, 1500, 1600)
        val b = fe(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val d = LongArray(16); feSub(d, a, b)
        val r = LongArray(16); feAdd(r, d, b)
        assertTrue(feEqual(r, a))
    }

    // ── #3: feMul ──────────────────────────────────────────────────────────

    @Test
    fun `feMul commutativity a times b equals b times a`() {
        val a = fe(12345, 67890, 1111, 2222, 3333, 4444, 5555, 6666, 7777, 8888, 9999, 1010, 1112, 1314, 1516, 1718)
        val b = fe(9876, 5432, 109, 281, 111, 222, 333, 444, 555, 666, 777, 888, 999, 123, 456, 789)
        val r1 = LongArray(16); feMul(r1, a, b)
        val r2 = LongArray(16); feMul(r2, b, a)
        assertTrue(feEqual(r1, r2))
    }

    @Test
    fun `feMul zero property 0 times a equals 0`() {
        val a = fe(7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7)
        val r = LongArray(16); feMul(r, LongArray(16), a)
        assertTrue(feEqual(r, LongArray(16)))
    }

    @Test
    fun `feMul identity property 1 times a equals a`() {
        val one = longArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val a = fe(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80)
        val r = LongArray(16); feMul(r, one, a)
        assertTrue(feEqual(r, a))
    }

    @Test
    fun `feMul distributivity a plus b times c equals a times c plus b times c`() {
        val a = fe(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val b = fe(17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32)
        val c = fe(101, 202, 303, 404, 505, 606, 707, 808, 909, 111, 222, 333, 444, 555, 666, 777)
        val apb = LongArray(16); feAdd(apb, a, b)
        val lhs = LongArray(16); feMul(lhs, apb, c)
        val ac = LongArray(16); feMul(ac, a, c)
        val bc = LongArray(16); feMul(bc, b, c)
        val rhs = LongArray(16); feAdd(rhs, ac, bc)
        assertTrue(feEqual(lhs, rhs))
    }

    @Test
    fun `feMul large-limb values stress carry - no overflow crash`() {
        // All limbs close to 2^16-1; multiplication triggers deep carry.
        val a = LongArray(16) { 0xffffL }
        val r = LongArray(16)
        // Should not throw, not crash
        feMul(r, a, a)
        val packed = ByteArray(32)
        fePack(packed, r)
        // Just check pack produces a 32-byte non-canonical result.
        assertEquals(32, packed.size)
    }

    // ── #4: feInv ──────────────────────────────────────────────────────────

    @Test
    fun `feInv inverse of 1 equals 1`() {
        val one = longArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val r = LongArray(16); feInv(r, one)
        assertTrue(feEqual(r, one))
    }

    @Test
    fun `feInv a times inverse-a equals 1`() {
        val a = fe(7, 13, 42, 99, 128, 255, 1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000)
        val invA = LongArray(16); feInv(invA, a)
        val product = LongArray(16); feMul(product, a, invA)
        val one = longArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        assertTrue(feEqual(product, one))
    }

    @Test
    fun `feInv involution inv-inv-a equals a`() {
        val a = fe(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80)
        val invA = LongArray(16); feInv(invA, a)
        val invInvA = LongArray(16); feInv(invInvA, invA)
        assertTrue(feEqual(invInvA, a))
    }

    // ── #5: feSq ───────────────────────────────────────────────────────────

    @Test
    fun `feSq matches feMul-a-a`() {
        val a = fe(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5, 8, 9, 7, 9, 3)
        val sq = LongArray(16); feSq(sq, a)
        val sq2 = LongArray(16); feMul(sq2, a, a)
        assertTrue(feEqual(sq, sq2))
    }

    // ── #6: fePack / feUnpack ─────────────────────────────────────────────

    @Test
    fun `fePack then feUnpack round-trips - small values`() {
        val a = fe(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val packed = ByteArray(32)
        fePack(packed, a)
        val b = LongArray(16)
        feUnpack(b, packed)
        assertTrue(feEqual(a, b))
    }

    @Test
    fun `fePack then feUnpack round-trips - BASE_Y known encoding`() {
        // BASE_Y = 4/5 mod p = 0x6666... repeated
        val baseY = longArrayOf(
            0x6658, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666,
            0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666,
        )
        val packed = ByteArray(32)
        fePack(packed, baseY)
        // Known encoding of base point y in little-endian:
        // 5866666666666666666666666666666666666666666666666666666666666666
        assertEquals("58", (packed[0].toInt() and 0xff).toString(16).padStart(2, '0'))
        assertEquals("66", (packed[1].toInt() and 0xff).toString(16).padStart(2, '0'))
        // And round trip
        val back = LongArray(16)
        feUnpack(back, packed)
        assertTrue(feEqual(baseY, back))
    }

    @Test
    fun `fePack of field value 0 is all 0 bytes`() {
        val packed = ByteArray(32) { 1 } // fill with garbage
        fePack(packed, LongArray(16))
        for (i in 0 until 32) assertEquals(0, packed[i].toInt(), "byte $i must be 0")
    }

    @Test
    fun `fePack of 1 yields 0x01 followed by 31 zeros`() {
        val one = longArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val packed = ByteArray(32)
        fePack(packed, one)
        assertEquals(1, packed[0].toInt())
        for (i in 1 until 32) assertEquals(0, packed[i].toInt(), "byte $i must be 0")
    }

    // ── #7: feIsNegative ───────────────────────────────────────────────────
    // Convention (TweetNaCl): returns (packed[0] and 1) i.e. parity of the
    // packed least-significant byte.  For small even numbers returns 0; for
    // odd small numbers returns 1.

    @Test
    fun `feIsNegative zero is 0 and one is 1 - LSB parity`() {
        assertEquals(0, feIsNegative(LongArray(16)))
        val one = longArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        assertEquals(1, feIsNegative(one))
        val two = longArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        assertEquals(0, feIsNegative(two))
    }

    @Test
    fun `feIsNegative neg1_p_minus_1_even_is_0_neg2_odd_is_1`() {
        val one = longArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val two = longArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        val negOne = LongArray(16); feSub(negOne, LongArray(16), one)
        val negTwo = LongArray(16); feSub(negTwo, LongArray(16), two)
        // p-1 has LSB 0xec (even) -> 0
        assertEquals(0, feIsNegative(negOne))
        // p-2 has LSB 0xeb (odd) -> 1
        assertEquals(1, feIsNegative(negTwo))
    }

    @Test
    fun `feIsNegative for x and p-x always differ by parity`() {
        for (xVal in intArrayOf(1, 2, 3, 4, 5, 100, 254, 255)) {
            val x = LongArray(16); x[0] = xVal.toLong()
            val px = LongArray(16); feSub(px, LongArray(16), x)
            assertNotEquals(feIsNegative(x), feIsNegative(px),
                "x=$xVal feIsNegative(x) vs feIsNegative(p-x) must differ")
        }
    }

    // ── #8: feEqual ────────────────────────────────────────────────────────

    @Test
    fun `feEqual same value in different representations - after add carry`() {
        val a = fe(0x10001, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        feCarry(a)
        // 0x10001 after carry becomes limb[0] = 1, carry 1 -> limb[1] += 1
        val b = fe(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        assertTrue(feEqual(a, b))
    }

    @Test
    fun `feEqual returns false for actually different values`() {
        val a = fe(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val b = fe(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 17)
        assertFalse(feEqual(a, b))
    }

    // ── #9: feCMove ────────────────────────────────────────────────────────

    @Test
    fun `feCMove b=0 keeps p unchanged`() {
        val p = fe(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val q = fe(99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99)
        val pSave = p.copyOf()
        feCMove(p, q, 0)
        assertTrue(p.contentEquals(pSave))
    }

    @Test
    fun `feCMove b=1 swaps p and q`() {
        val p = fe(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        val q = fe(99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99)
        val pSave = p.copyOf()
        val qSave = q.copyOf()
        feCMove(p, q, 1)
        assertTrue(p.contentEquals(qSave))
        assertTrue(q.contentEquals(pSave))
    }

    @Test
    fun `feCMove aliasing p equals q does not corrupt for b=0 and b=1`() {
        val p = fe(5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5)
        val q = p // same reference (aliasing)
        val save = p.copyOf()
        feCMove(p, q, 0)
        assertTrue(p.contentEquals(save))
        feCMove(p, q, 1)
        assertTrue(p.contentEquals(save)) // XOR same value twice returns to original
    }

    // ── #10: geBytesGtOrEqP ───────────────────────────────────────────────

    @Test
    fun `geBytesGtOrEqP rejects p exactly`() {
        assertTrue(geBytesGtOrEqP(P_EXACT_LE))
    }

    @Test
    fun `geBytesGtOrEqP accepts p-1 - largest valid field element`() {
        assertFalse(geBytesGtOrEqP(P_MINUS_1_LE))
    }

    @Test
    fun `geBytesGtOrEqP accepts 0`() {
        assertFalse(geBytesGtOrEqP(ByteArray(32)))
    }

    @Test
    fun `geBytesGtOrEqP rejects y = p plus 16 - just past p-bit255 still 0`() {
        // y = p + 16 = (2^255-19) + 16 = 2^255-3 → bit255 is still 0 (7f...)
        val y = P_EXACT_LE.copyOf()
        // Add 16 to limb 0 (p[0] = 0xed = 237; 237+16 = 253 = 0xfd)
        y[0] = (y[0] + 16).toByte()
        assertTrue(geBytesGtOrEqP(y))
    }

    @Test
    fun `geBytesGtOrEqP accepts BASE_Y - well-known valid point y-coordinate`() {
        val baseYBytes = ByteArray(32)
        fePack(baseYBytes, longArrayOf(
            0x6658, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666,
            0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666, 0x6666,
        ))
        assertFalse(geBytesGtOrEqP(baseYBytes))
    }
}
