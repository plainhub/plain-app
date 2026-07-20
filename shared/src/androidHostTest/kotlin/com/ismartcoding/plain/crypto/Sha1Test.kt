package com.ismartcoding.plain.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the pure-Kotlin [sha1] implementation.
 *
 * Test vectors are the official NIST FIPS 180-4 SHA-1 examples plus a few
 * edge cases that matter for the code paths in this repo:
 *  - empty input (used by some feed-content fallbacks)
 *  - multi-block input (56-byte message → two-block padding path)
 *  - long message (1M 'a' characters → many-block stress test)
 *  - non-ASCII UTF-8 input (matches what `Feed.android.kt` and
 *    `DownloadHelper` feed in for non-ASCII URLs)
 */
class Sha1Test {

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private fun sha1Hex(input: String): String = hex(sha1(input.encodeToByteArray()))

    private fun sha1Hex(input: ByteArray): String = hex(sha1(input))

    @Test
    fun emptyString() {
        // NIST FIPS 180-4 / RFC 6234 example
        assertEquals(
            "da39a3ee5e6b4b0d3255bfef95601890afd80709",
            sha1Hex(""),
        )
    }

    @Test
    fun abc() {
        // NIST example: SHA-1("abc")
        assertEquals(
            "a9993e364706816aba3e25717850c26c9cd0d89d",
            sha1Hex("abc"),
        )
    }

    @Test
    fun twoBlockMessage() {
        // NIST example: 448-bit (56-byte) message → two-block padding path
        assertEquals(
            "84983e441c3bd26ebaae4aa1f95129e5e54670f1",
            sha1Hex("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"),
        )
    }

    @Test
    fun millionACharacters() {
        // NIST long-message sanity check — exercises many blocks.
        val input = ByteArray(1_000_000) { 'a'.code.toByte() }
        assertEquals(
            "34aa973cd4c4daa4f61eeb2bdbad27316534016f",
            sha1Hex(input),
        )
    }

    @Test
    fun utf8Chinese() {
        // "中国" UTF-8 = E4 B8 AD E5 9B BD
        assertEquals(
            "101806f57c322fb403a9788c4c24b79650d02e77",
            sha1Hex("中国"),
        )
    }

    @Test
    fun returns20Bytes() {
        val digest = sha1("hello".encodeToByteArray())
        assertEquals(20, digest.size)
    }

    @Test
    fun deterministicAcrossCalls() {
        val a = sha1Hex("deterministic input")
        val b = sha1Hex("deterministic input")
        assertEquals(a, b)
    }

    @Test
    fun differentInputsProduceDifferentDigests() {
        val a = sha1Hex("input one")
        val b = sha1Hex("input two")
        assertTrue(a != b, "different inputs must produce different digests")
    }

    @Test
    fun rawByteArrayMatchesHexDecode() {
        val expectedHex = "a9993e364706816aba3e25717850c26c9cd0d89d"
        val expected = expectedHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        assertContentEquals(expected, sha1("abc".encodeToByteArray()))
    }
}
