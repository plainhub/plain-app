package com.ismartcoding.plain.crypto

import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Ed25519Verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Produces the correct 1082-byte MSG3 for RFC 8032 §7.1 TEST 3,
 * then verifies against the RFC-provided SIG3 hex.  Also dumps MSG3_HEX
 * so the iOS side can copy-paste it.
 */
class Rfc8032Test3MsgGenerator {
    private fun toHex(b: ByteArray): String =
        b.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    private fun hex(s: String): ByteArray = ByteArray(s.length / 2) {
        s.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }

    @Test
    fun generate_and_verify_rfc_test3() {
        val SK3 = hex("c5aa8df43f9f837bedb7442f31dcb7b166d38535076f094b85ce3a2e0b44d8ec")
        val PK3 = hex("fc51cd8e6218a1a38da47ed00230f0580816ed13ba3303ac5deb911548908025")
        val EXPECTED_SIG3 = hex(
            "6291d657deec24024827e69c3abe01a3" +
                "0ce548a284743a445e3680d7db5ac3ac" +
                "18ff9b538d16f290ae67f760984dc659" +
                "4a7c15e9716ed28dc027beceea1ec40a"
        )

        // MSG3 is defined in RFC 8032 §7.1 TEST 3 as:
        // MESSAGE (length 1082 bytes): 000102030405060708090a0b... up to 1082 bytes
        // i.e., byte[i] = (i mod 256) for i in [0, 1081]
        val MSG3_BUILT = ByteArray(1082) { (it and 0xff).toByte() }
        assertEquals(1082, MSG3_BUILT.size)

        println("=== First 32 bytes of MSG3: ${toHex(MSG3_BUILT.copyOfRange(0,32))}")
        println("=== Last  16 bytes of MSG3: ${toHex(MSG3_BUILT.copyOfRange(1082-16,1082))}")

        val tinkSig = Ed25519Sign(SK3).sign(MSG3_BUILT)
        println("Tink SIG3 actual  R = ${toHex(tinkSig.copyOfRange(0,32))}")
        println("RFC  SIG3 expect  R = ${toHex(EXPECTED_SIG3.copyOfRange(0,32))}")
        println("Tink SIG3 actual  S = ${toHex(tinkSig.copyOfRange(32,64))}")
        println("RFC  SIG3 expect  S = ${toHex(EXPECTED_SIG3.copyOfRange(32,64))}")
        assertEquals(toHex(EXPECTED_SIG3), toHex(tinkSig), "Tink SIG3 must match RFC")

        // Verify via Tink Verify too
        Ed25519Verify(PK3).verify(EXPECTED_SIG3, MSG3_BUILT) // throws on bad sig
        assertTrue(true, "Tink verify OK")

        // DUMP MSG3_HEX for iOS side copy:
        println()
        println("=== MSG3_HEX Kotlin literal (1082 bytes, 2164 hex chars): ===")
        val h = toHex(MSG3_BUILT)
        // Print in chunks of 100 chars per line for easy paste
        var i = 0
        while (i < h.length) {
            val end = minOf(i + 100, h.length)
            println("append(\"${h.substring(i, end)}\")")
            i = end
        }
    }
}
