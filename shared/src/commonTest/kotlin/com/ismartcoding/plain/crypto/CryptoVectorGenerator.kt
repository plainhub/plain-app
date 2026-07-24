package com.ismartcoding.plain.crypto

import com.ismartcoding.plain.platform.PairingCrypto
import com.ismartcoding.plain.platform.chaCha20Encrypt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Generates cross-platform crypto test vectors that verify plain-app (Kotlin)
 * and plain-web (TypeScript / Rust) produce identical encryption / decryption
 * behavior for ECDH P-256, Ed25519, XChaCha20-Poly1305, base64, and hex.
 *
 * The test writes a JSON file to the path given by the `crypto.vectors.output`
 * system property (defaults to `build/crypto-vectors.json`). The output
 * is consumed by:
 *   - plain-web/tests/lib/crypto-vectors.test.ts (TypeScript, @noble/ciphers)
 *   - plain-web/src-tauri/src/crypto/ modules (Rust, chacha20poly1305 / p256 / ed25519-dalek)
 *
 * Vector formats (all base64 is RFC 4648, all hex is lowercase):
 *   • ECDH: secp256r1, public key = X9.63 uncompressed (65 bytes: 0x04 || X || Y),
 *     private key = raw 32 bytes, shared key = SHA-256(raw ECDH secret) as base64.
 *   • Ed25519: raw 32-byte private key, raw 32-byte public key, 64-byte signature.
 *   • XChaCha20-Poly1305: 32-byte key, 24-byte nonce. Tink produces
 *     nonce(24) || ciphertext || tag(16); the vector stores nonce and
 *     ciphertext separately (ciphertext includes the 16-byte Poly1305 tag).
 */
class CryptoVectorGenerator {

    @Test
    fun generateVectors() {
        val ecdhVectors = generateEcdhVectors()
        val ed25519Vectors = generateEd25519Vectors()
        val xchachaVectors = generateXChaChaVectors()
        val base64Vectors = generateBase64Vectors()
        val hexVectors = generateHexVectors()

        // Sanity-check the vectors before writing them.
        verifyEcdhSymmetry(ecdhVectors)
        verifyEd25519RoundTrip(ed25519Vectors)
        verifyXChaChaRoundTrip(xchachaVectors)

        val json = buildJson(
            ecdh = ecdhVectors,
            ed25519 = ed25519Vectors,
            xchacha20 = xchachaVectors,
            base64 = base64Vectors,
            hex = hexVectors,
        )

        val outputPath = System.getProperty("crypto.vectors.output")
            ?: "build/crypto-vectors.json"
        val outFile = java.io.File(outputPath)
        outFile.parentFile?.mkdirs()
        outFile.writeText(json)
        println("Crypto vectors written to: ${outFile.absolutePath}")
    }

    // ── ECDH P-256 ────────────────────────────────────────────────────────────

    private data class EcdhVectors(
        val privateKeyA: String,
        val publicKeyA: String,
        val privateKeyB: String,
        val publicKeyB: String,
        val sharedKey: String,
    )

    private fun generateEcdhVectors(): EcdhVectors {
        val pairA = PairingCrypto.generateECDHKeyPair()
        val pairB = PairingCrypto.generateECDHKeyPair()

        // ECDH(A_priv, B_pub) must equal ECDH(B_priv, A_pub).
        val sharedAB = PairingCrypto.computeECDHSharedKey(pairA.privateKeyEncoded, pairB.publicKeyEncoded)
        val sharedBA = PairingCrypto.computeECDHSharedKey(pairB.privateKeyEncoded, pairA.publicKeyEncoded)
        assertNotNull(sharedAB, "ECDH shared key A→B must not be null")
        assertNotNull(sharedBA, "ECDH shared key B→A must not be null")
        assertEquals(sharedAB, sharedBA, "ECDH shared key must be symmetric")

        return EcdhVectors(
            privateKeyA = base64(pairA.privateKeyEncoded),
            publicKeyA = base64(pairA.publicKeyEncoded),
            privateKeyB = base64(pairB.privateKeyEncoded),
            publicKeyB = base64(pairB.publicKeyEncoded),
            sharedKey = sharedAB,
        )
    }

    private fun verifyEcdhSymmetry(v: EcdhVectors) {
        val recomputed = PairingCrypto.computeECDHSharedKey(
            base64Decode(v.privateKeyA),
            base64Decode(v.publicKeyB),
        )
        assertEquals(v.sharedKey, recomputed, "Recomputed ECDH shared key must match vector")
    }

    // ── Ed25519 ───────────────────────────────────────────────────────────────

    private data class Ed25519Vectors(
        val privateKey: String,
        val publicKey: String,
        val message: String,
        val signature: String,
    )

    private fun generateEd25519Vectors(): Ed25519Vectors {
        val (priv, pub) = PairingCrypto.generateEd25519KeyPair()
        val message = "the quick brown fox jumps over the lazy dog"
        val signature = PairingCrypto.signEd25519(priv, message.toByteArray())

        assertEquals(32, priv.size, "Ed25519 private key must be 32 bytes")
        assertEquals(32, pub.size, "Ed25519 public key must be 32 bytes")
        assertEquals(64, signature.size, "Ed25519 signature must be 64 bytes")

        return Ed25519Vectors(
            privateKey = base64(priv),
            publicKey = base64(pub),
            message = message,
            signature = base64(signature),
        )
    }

    private fun verifyEd25519RoundTrip(v: Ed25519Vectors) {
        val ok = PairingCrypto.verifyEd25519(
            base64Decode(v.publicKey),
            v.message.toByteArray(),
            base64Decode(v.signature),
        )
        assertTrue(ok, "Ed25519 signature must verify against its own public key")
    }

    // ── XChaCha20-Poly1305 ────────────────────────────────────────────────────

    private data class XChaChaVectors(
        val key: String,
        val nonce: String,
        val plaintext: String,
        val ciphertext: String,
    )

    private fun generateXChaChaVectors(): XChaChaVectors {
        // Use a fixed key so the vector is reproducible on the decrypt side.
        val key = ByteArray(32) { ((it * 7 + 3) and 0xFF).toByte() }
        val plaintext = "hello xchacha20-poly1305 cross-platform test"

        // Tink's XChaCha20Poly1305.encrypt() returns nonce(24) || ciphertext || tag(16).
        val blob = chaCha20Encrypt(key, plaintext)
        assertEquals(24, blob.size - plaintext.toByteArray().size - 16,
            "Blob overhead must be 24-byte nonce + 16-byte tag")

        val nonce = blob.copyOfRange(0, 24)
        val ciphertext = blob.copyOfRange(24, blob.size)

        return XChaChaVectors(
            key = base64(key),
            nonce = base64(nonce),
            plaintext = plaintext,
            ciphertext = base64(ciphertext),
        )
    }

    private fun verifyXChaChaRoundTrip(v: XChaChaVectors) {
        val key = base64Decode(v.key)
        val nonce = base64Decode(v.nonce)
        val ciphertext = base64Decode(v.ciphertext)
        val blob = ByteArray(nonce.size + ciphertext.size)
        System.arraycopy(nonce, 0, blob, 0, nonce.size)
        System.arraycopy(ciphertext, 0, blob, nonce.size, ciphertext.size)

        val decrypted = com.ismartcoding.plain.platform.chaCha20Decrypt(key, blob)
        assertNotNull(decrypted, "XChaCha20 decrypt of reconstructed blob must succeed")
        assertEquals(v.plaintext, decrypted.decodeToString(), "Decrypted plaintext must match")
    }

    // ── base64 / hex known-answer vectors ─────────────────────────────────────

    private data class Base64Vectors(
        val input: List<Int>,
        val output: String,
    )

    private fun generateBase64Vectors(): Base64Vectors {
        val input = byteArrayOf(104, 101, 108, 108, 111) // "hello"
        return Base64Vectors(
            input = input.toList().map { it.toInt() and 0xFF },
            output = base64(input),
        )
    }

    private data class HexVectors(
        val input: List<Int>,
        val output: String,
    )

    private fun generateHexVectors(): HexVectors {
        val input = byteArrayOf(0xFF.toByte(), 0x00, 0x80.toByte())
        return HexVectors(
            input = input.toList().map { it.toInt() and 0xFF },
            output = input.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') },
        )
    }

    // ── JSON builder ──────────────────────────────────────────────────────────

    private fun buildJson(
        ecdh: EcdhVectors,
        ed25519: Ed25519Vectors,
        xchacha20: XChaChaVectors,
        base64: Base64Vectors,
        hex: HexVectors,
    ): String = buildString {
        appendLine("{")
        appendLine("  \"ecdh\": {")
        appendLine("    \"privateKeyA\": ${jsonStr(ecdh.privateKeyA)},")
        appendLine("    \"publicKeyA\": ${jsonStr(ecdh.publicKeyA)},")
        appendLine("    \"privateKeyB\": ${jsonStr(ecdh.privateKeyB)},")
        appendLine("    \"publicKeyB\": ${jsonStr(ecdh.publicKeyB)},")
        appendLine("    \"sharedKey\": ${jsonStr(ecdh.sharedKey)}")
        appendLine("  },")
        appendLine("  \"ed25519\": {")
        appendLine("    \"privateKey\": ${jsonStr(ed25519.privateKey)},")
        appendLine("    \"publicKey\": ${jsonStr(ed25519.publicKey)},")
        appendLine("    \"message\": ${jsonStr(ed25519.message)},")
        appendLine("    \"signature\": ${jsonStr(ed25519.signature)}")
        appendLine("  },")
        appendLine("  \"xchacha20\": {")
        appendLine("    \"key\": ${jsonStr(xchacha20.key)},")
        appendLine("    \"nonce\": ${jsonStr(xchacha20.nonce)},")
        appendLine("    \"plaintext\": ${jsonStr(xchacha20.plaintext)},")
        appendLine("    \"ciphertext\": ${jsonStr(xchacha20.ciphertext)}")
        appendLine("  },")
        appendLine("  \"base64\": {")
        appendLine("    \"input\": ${base64.input},")
        appendLine("    \"output\": ${jsonStr(base64.output)}")
        appendLine("  },")
        appendLine("  \"hex\": {")
        appendLine("    \"input\": ${hex.input},")
        appendLine("    \"output\": ${jsonStr(hex.output)}")
        appendLine("  }")
        appendLine("}")
    }

    private fun jsonStr(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun base64(bytes: ByteArray): String =
        java.util.Base64.getEncoder().encodeToString(bytes)

    private fun base64Decode(s: String): ByteArray =
        java.util.Base64.getDecoder().decode(s)
}
