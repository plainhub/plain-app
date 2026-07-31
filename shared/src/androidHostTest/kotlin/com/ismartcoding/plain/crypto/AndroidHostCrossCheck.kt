package com.ismartcoding.plain.crypto

import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Ed25519Verify
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Run on ANDROID HOST to produce expected signed outputs for fixed seeds, so we
 * can compare against the iOS pure-Kotlin implementation byte-by-byte.
 */
class AndroidHostCrossCheck {
    private fun toHex(b: ByteArray): String =
        b.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    private fun hex(s: String): ByteArray = ByteArray(s.length / 2) {
        s.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }

    private fun signTink(sk: ByteArray, msg: ByteArray): ByteArray = Ed25519Sign(sk).sign(msg)
    private fun derivePkTink(sk: ByteArray): ByteArray = Ed25519Sign.KeyPair.newKeyPair().let {
        // Ignore fresh kp, instead reconstruct public from the provided seed by signing.
        // Tink's public key isn't directly exposed from the signer; use KeyPair of the seed?
        // No easy way. Just sign + hard work: Use BouncyCastle-less trick by signing empty?
        // Better: use the derivePublic helper via Tink? We'll use the native Tink test
        // framework: actually Ed25519Sign ctor takes 32-byte seed and the java class has
        // a public 32-byte public key? Not public. So we reconstruct by trying to verify.
        // For simplicity, do a full Ed25519Sign->verification cycle and write out raw bytes.
        // Since the iOS side needs to produce a sign result that Tink Verify accepts, we
        // capture iOS pk by deriving it in code. Here we just produce the Tink SIGN result
        // for the same sk+msg (the golden output) for comparison against iOS sign().
        return ByteArray(32) // placeholder; replaced below
    }.also { /* placeholder, we don't really use derivePkTink output below */ }

    @Test
    fun generate_expected_signatures_for_iOS_comparison() {
        val skA = ByteArray(32); Random(42).nextBytes(skA)
        println("=== skA Random(42) ===")
        println("skA = ${toHex(skA)}")
        val cases = listOf(
            "empty" to ByteArray(0),
            "0x00" to byteArrayOf(0x00),
            "0xff" to byteArrayOf(0xff.toByte()),
        )
        for ((name, msg) in cases) {
            val sig = signTink(skA, msg)
            println("Tink sig[$name] R = ${toHex(sig.copyOfRange(0,32))}")
            println("Tink sig[$name] S = ${toHex(sig.copyOfRange(32,64))}")
        }

        println("\n=== RFC test 2 (SK=4ccd...8a6fb, msg=0x72) ===")
        val sk2 = hex("4ccd089b28ff96da9db6c346ec114e0f5b8a319f35aba624da8cf6ed4fb8a6fb")
        val sig2 = signTink(sk2, byteArrayOf(0x72))
        println("Tink sig2 R = ${toHex(sig2.copyOfRange(0,32))}")
        println("Tink sig2 S = ${toHex(sig2.copyOfRange(32,64))}")
        println("Expected  R = 92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da")
        println("Expected  S = 085ac1e43e15996e458f3613d0f11d8c387b2eaeb4302aeeb00d291612bb0c00")
        assertEquals(
            "92a009a9f0d4cab8720e820b5f642540a2b27b5416503f8fb3762223ebdb69da",
            toHex(sig2.copyOfRange(0,32)), "RFC 2 R matches Tink (sanity for Tink itself)"
        )
    }
}
