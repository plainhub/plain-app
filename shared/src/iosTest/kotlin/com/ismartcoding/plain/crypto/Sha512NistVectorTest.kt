package com.ismartcoding.plain.crypto

import com.ismartcoding.plain.lib.crypto.sha512
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

/**
 * NIST FIPS 180-4 SHA-512 test vectors — iOS port of androidHostTest/Sha512Test.
 *
 * SHA-512 is used by Ed25519 signing/verification at three points:
 *   1. H(privateKey) → scalar a || prefix
 *   2. H(prefix || message) → r (scalar)
 *   3. H(R || A || message) → k (scalar)
 * Any bug in the pure-Kotlin SHA-512 implementation (byte-order, padding,
 * schedule computation, initial hash constants, rotation counts) will
 * silently cause wrong signatures for a subset of messages.  These NIST
 * vectors plus the million-char exercise both one-block and multi-block
 * code paths as well as length overflow in the 128-byte length field.
 */
class Sha512NistVectorTest {

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private fun sha512Hex(input: String): String = hex(sha512(input.encodeToByteArray()))
    private fun sha512Hex(input: ByteArray): String = hex(sha512(input))

    @Test
    fun `NIST empty string`() {
        assertEquals(
            "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce" +
                "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
            sha512Hex(""),
        )
    }

    @Test
    fun `NIST abc single block`() {
        assertEquals(
            "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
                "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            sha512Hex("abc"),
        )
    }

    @Test
    fun `NIST two-block message 112 bytes`() {
        // NIST FIPS 180-4 §7.4: "abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu"
        val msg = "abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu"
        assertEquals(
            "8e959b75dae313da8cf4f72814fc143f8f7779c6eb9f7fa17299aeadb6889018" +
                "501d289e4900f7e4331b99dec4b5433ac7d329eeb6dd26545e96e55b874be909",
            sha512Hex(msg),
        )
    }

    @Test
    fun `NIST one million 'a' characters - multi-block-length overflow path`() {
        val input = ByteArray(1_000_000) { 'a'.code.toByte() }
        assertEquals(
            "e718483d0ce769644e2e42c7bc15b4638e1f98b13b2044285632a803afa973eb" +
                "de0ff244877ea60a4cb0432ce577c31beb009c5c2c49aa2e4eadb217ad8cc09b",
            sha512Hex(input),
        )
    }

    @Test
    fun `returns exactly 64 bytes`() {
        assertEquals(64, sha512("any".encodeToByteArray()).size)
    }

    @Test
    fun `deterministic across repeated calls`() {
        val a = sha512Hex("deterministic input string")
        val b = sha512Hex("deterministic input string")
        assertEquals(a, b)
    }

    @Test
    fun `different inputs produce different digests`() {
        val a = sha512Hex("input a")
        val b = sha512Hex("input b")
        assertTrue(a != b)
    }

    @Test
    fun `raw bytes match hex decode for abc vector`() {
        val expectedHex =
            "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
                "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f"
        val expected = expectedHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        assertContentEquals(expected, sha512("abc".encodeToByteArray()))
    }

    @Test
    fun `chinese utf8 E4B8AD E59BBD matches shasum reference`() {
        assertEquals(
            "6a169e7d5b7526651086d0d37d6e7686c7e75ff7039d063ad100aefab1057a4c" +
                "1db1f1e5d088c9585db1d7531a461ab3f4490cc63809c08cc074574b3fff759a",
            sha512Hex("中国"),
        )
    }
}
