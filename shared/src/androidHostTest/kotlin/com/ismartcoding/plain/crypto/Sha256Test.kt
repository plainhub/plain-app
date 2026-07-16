package com.ismartcoding.plain.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the pure-Kotlin [sha256] implementation.
 *
 * Test vectors are the official NIST FIPS 180-4 SHA-256 examples plus a few
 * edge cases that matter for the pairing/feed code paths:
 *  - empty input (used when computing the rawId of a degenerate feed entry)
 *  - multi-block input (> 56 bytes, exercises the two-block padding path)
 *  - non-ASCII UTF-8 input (matches what `Extensions.toDFeedEntry` feeds in)
 */
class Sha256Test {

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private fun sha256Hex(input: String): String = hex(sha256(input.encodeToByteArray()))

    private fun sha256Hex(input: ByteArray): String = hex(sha256(input))

    @Test
    fun emptyString() {
        // NIST FIPS 180-4 / RFC 6234 example
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            sha256Hex(""),
        )
    }

    @Test
    fun abc() {
        // NIST example: SHA-256("abc")
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            sha256Hex("abc"),
        )
    }

    @Test
    fun twoBlockMessage() {
        // NIST example: 448-bit (56-byte) message -> two-block padding path
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            sha256Hex("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"),
        )
    }

    @Test
    fun millionACharacters() {
        // NIST long-message sanity check — exercises many blocks.
        val input = ByteArray(1_000_000) { 'a'.code.toByte() }
        assertEquals(
            "cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0",
            sha256Hex(input),
        )
    }

    @Test
    fun utf8Chinese() {
        // "中国" UTF-8 = E4 B8 AD E5 9B BD
        assertEquals(
            "f0e9521611bb290d7b09b8cd14a63c3fe7cbf9a2f4e0090d8238d22403d35182",
            sha256Hex("中国"),
        )
    }

    @Test
    fun returns32Bytes() {
        val digest = sha256("hello".encodeToByteArray())
        assertEquals(32, digest.size)
    }

    @Test
    fun deterministicAcrossCalls() {
        val a = sha256Hex("deterministic input")
        val b = sha256Hex("deterministic input")
        assertEquals(a, b)
    }

    @Test
    fun differentInputsProduceDifferentDigests() {
        val a = sha256Hex("input one")
        val b = sha256Hex("input two")
        assertTrue(a != b, "different inputs must produce different digests")
    }

    @Test
    fun rawByteArrayMatchesHexDecode() {
        val expectedHex = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        val expected = expectedHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        assertContentEquals(expected, sha256("abc".encodeToByteArray()))
    }
}
