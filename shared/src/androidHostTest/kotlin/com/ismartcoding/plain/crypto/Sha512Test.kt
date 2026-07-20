package com.ismartcoding.plain.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the pure-Kotlin [sha512] implementation.
 *
 * Test vectors are the official NIST FIPS 180-4 SHA-512 examples plus a few
 * edge cases. The pure-Kotlin implementation exists because iOS does not
 * expose CommonCrypto through the default Kotlin/Native cinterop bindings,
 * so [com.ismartcoding.plain.platform.sha512] (commonMain) delegates to
 * this implementation on every platform.
 */
class Sha512Test {

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private fun sha512Hex(input: String): String = hex(sha512(input.encodeToByteArray()))

    private fun sha512Hex(input: ByteArray): String = hex(sha512(input))

    @Test
    fun emptyString() {
        // NIST FIPS 180-4 / RFC 6234 example
        assertEquals(
            "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce" +
                "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
            sha512Hex(""),
        )
    }

    @Test
    fun abc() {
        // NIST example: SHA-512("abc")
        assertEquals(
            "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
                "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            sha512Hex("abc"),
        )
    }

    @Test
    fun utf8Chinese() {
        // "中国" UTF-8 = E4 B8 AD E5 9B BD
        // Verified via `printf '中国' | shasum -a 512` on macOS
        assertEquals(
            "6a169e7d5b7526651086d0d37d6e7686c7e75ff7039d063ad100aefab1057a4c" +
                "1db1f1e5d088c9585db1d7531a461ab3f4490cc63809c08cc074574b3fff759a",
            sha512Hex("中国"),
        )
    }

    @Test
    fun returns64Bytes() {
        val digest = sha512("hello".encodeToByteArray())
        assertEquals(64, digest.size)
    }

    @Test
    fun deterministicAcrossCalls() {
        val a = sha512Hex("deterministic input")
        val b = sha512Hex("deterministic input")
        assertEquals(a, b)
    }

    @Test
    fun differentInputsProduceDifferentDigests() {
        val a = sha512Hex("input one")
        val b = sha512Hex("input two")
        assertTrue(a != b, "different inputs must produce different digests")
    }

    @Test
    fun rawByteArrayMatchesHexDecode() {
        val expectedHex =
            "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
                "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f"
        val expected = expectedHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        assertContentEquals(expected, sha512("abc".encodeToByteArray()))
    }
}
