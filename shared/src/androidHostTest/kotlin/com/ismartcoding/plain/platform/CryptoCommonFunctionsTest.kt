package com.ismartcoding.plain.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the commonMain [sha1], [sha512] and [randomPassword] helpers.
 *
 * These functions were previously `expect` declarations with platform-specific
 * implementations (Android used `java.security.MessageDigest`, iOS returned
 * empty strings). They now live in `commonMain` and delegate to the pure-Kotlin
 * `crypto.sha1` / `crypto.sha512` implementations, so the contract is identical
 * on every platform and worth pinning down with regression tests.
 */
class CryptoCommonFunctionsTest {

    @Test
    fun sha1ReturnsLowercaseHexDigest() {
        assertEquals(
            "a9993e364706816aba3e25717850c26c9cd0d89d",
            sha1("abc".encodeToByteArray()),
        )
    }

    @Test
    fun sha512ReturnsLowercaseHexDigest() {
        assertEquals(
            "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
                "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f",
            sha512("abc".encodeToByteArray()),
        )
    }

    @Test
    fun sha1OfEmptyInputMatchesKnownConstant() {
        assertEquals(
            "da39a3ee5e6b4b0d3255bfef95601890afd80709",
            sha1(ByteArray(0)),
        )
    }

    @Test
    fun sha512OfEmptyInputMatchesKnownConstant() {
        assertEquals(
            "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce" +
                "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
            sha512(ByteArray(0)),
        )
    }

    @Test
    fun randomPasswordHasRequestedLength() {
        assertEquals(0, randomPassword(0).length)
        assertEquals(1, randomPassword(1).length)
        assertEquals(32, randomPassword(32).length)
        assertEquals(64, randomPassword(64).length)
    }

    @Test
    fun randomPasswordUsesUnambiguousCharset() {
        // Ambiguous characters (0, O, 1, I, l) are explicitly excluded.
        val forbidden = setOf('0', 'O', '1', 'I', 'l')
        repeat(50) {
            val password = randomPassword(32)
            assertTrue(
                password.none { it in forbidden },
                "password must not contain ambiguous chars: $password",
            )
        }
    }

    @Test
    fun randomPasswordIsReasonablyRandom() {
        // Two consecutive calls with the same length should almost never
        // produce identical strings. We don't assert full cryptographic
        // randomness — only that the output distribution is not degenerate.
        val a = randomPassword(32)
        val b = randomPassword(32)
        assertNotEquals(a, b, "two consecutive calls must not return identical strings")
    }
}
