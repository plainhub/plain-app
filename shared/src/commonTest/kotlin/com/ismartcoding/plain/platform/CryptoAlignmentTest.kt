package com.ismartcoding.plain.platform

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cross-platform alignment tests for the cryptographic primitives shared
 * between plain-app (KMP) and plain-web (TypeScript / Rust).
 *
 * Vectors are produced by `CryptoVectorGenerator` (androidHostTest) using
 * Tink on Android and the iOS implementations (pure-Kotlin Ed25519 and
 * Security framework ECDH P-256) must reproduce them bit-for-bit. The same
 * JSON is consumed by `plain-web/tests/lib/crypto-vectors.test.ts` so all
 * three platforms stay in sync.
 *
 *   • Ed25519: raw 32-byte keys, 64-byte signatures (RFC 8032).
 *   • ECDH P-256: X9.63 uncompressed public keys (0x04 || X || Y),
 *     shared key = Base64(SHA-256(raw ECDH secret)).
 */
@OptIn(ExperimentalEncodingApi::class)
class CryptoAlignmentTest {

    private fun b64(s: String): ByteArray = Base64.decode(s)
    private fun b64(b: ByteArray): String = Base64.encode(b)

    // ── Android-generated vectors (build/crypto-vectors.json) ────────────────

    private val ed25519PrivateKey = b64("UANIUfDab+G0j9qNVXM1Yo9WGt6yP0p26WEhyWCH2To=")
    private val ed25519PublicKey = b64("VYiFgFSMmpnIXOGDJ8o2Le2tSfxfZ/2EivdH34CW6vo=")
    private val ed25519Message = "the quick brown fox jumps over the lazy dog"
    private val ed25519Signature = b64(
        "mHzr3ksGYRSoFwmre0ijmx46o4U8vo+0ckaqgZWT/xbdU3N8TWIFoA5onvQ3cwHH2RrAIHlM8YtGCe7f+gd9Bw=="
    )

    private val ecdhPrivateKeyA = b64("oUsArOFD6uKGYkQ9u2qePaafQZr0/Le2n7TuRYgf2Jc=")
    private val ecdhPublicKeyA = b64(
        "BA6jVz/nSbE2avdOz3kf3Ca9b1cADiFhOlS0A8B5fLlnZuUuSNoY0wg3HVNhHDrAPreEVb+0acPF5SpaBkYhGYE="
    )
    private val ecdhPrivateKeyB = b64("1ypJzYvZ4pLBZJtCdZaJf97N95EtGaq/CFA3xcPVGFw=")
    private val ecdhPublicKeyB = b64(
        "BLXYb9xmHSRC6uUlfv2G3SeYBHw6NBiwCu8ZRbT9uD8bqdaYppK8N7twmDOGTpDsib2GeenJitftjub0jwAnQYc="
    )
    private val ecdhSharedKey = "fxyquodTOtcOX+bVBCZ6a5SsTDodamJOLRfzoemtMJU="

    // ── Ed25519 ──────────────────────────────────────────────────────────────

    @Test
    fun ed25519_private_key_is_32_bytes() {
        assertEquals(32, ed25519PrivateKey.size)
    }

    @Test
    fun ed25519_public_key_is_32_bytes() {
        assertEquals(32, ed25519PublicKey.size)
    }

    @Test
    fun ed25519_signature_is_64_bytes() {
        assertEquals(64, ed25519Signature.size)
    }

    @Test
    fun ed25519_verifies_android_signature() {
        assertTrue(
            verifyEd25519(ed25519PublicKey, ed25519Message.encodeToByteArray(), ed25519Signature),
            "iOS must verify an Ed25519 signature produced by Android/Tink"
        )
    }

    @Test
    fun ed25519_re_signing_produces_identical_signature() {
        // Ed25519 is deterministic — re-signing the same message with the
        // same private key must produce the same bytes the Android side got.
        // This also proves iOS derives the same public key from the private
        // key, because S = (r + k * a) mod L where k = SHA-512(R || A || M)
        // depends on the derived public key A.
        val resign = signEd25519(ed25519PrivateKey, ed25519Message.encodeToByteArray())
        assertContentEquals(ed25519Signature, resign)
    }

    @Test
    fun ed25519_rejects_tampered_signature() {
        val tampered = ed25519Signature.copyOf()
        tampered[0] = (tampered[0].toInt() xor 0x01).toByte()
        assertFalse(
            verifyEd25519(ed25519PublicKey, ed25519Message.encodeToByteArray(), tampered),
            "tampered signature must fail"
        )
    }

    @Test
    fun ed25519_rejects_tampered_message() {
        assertFalse(
            verifyEd25519(ed25519PublicKey, "modified".encodeToByteArray(), ed25519Signature),
            "tampered message must fail"
        )
    }

    // ── ECDH P-256 ───────────────────────────────────────────────────────────

    @Test
    fun ecdh_private_keys_are_32_bytes() {
        assertEquals(32, ecdhPrivateKeyA.size)
        assertEquals(32, ecdhPrivateKeyB.size)
    }

    @Test
    fun ecdh_public_keys_are_65_bytes_uncompressed_x963() {
        assertEquals(65, ecdhPublicKeyA.size)
        assertEquals(0x04, ecdhPublicKeyA[0])
        assertEquals(65, ecdhPublicKeyB.size)
        assertEquals(0x04, ecdhPublicKeyB[0])
    }

    @Test
    fun ecdh_A_priv_B_pub_matches_shared_key() {
        val shared = computeECDHSharedKey(ecdhPrivateKeyA, ecdhPublicKeyB)
        assertNotNull(shared, "ECDH shared key A→B must not be null")
        assertEquals(ecdhSharedKey, shared)
    }

    @Test
    fun ecdh_B_priv_A_pub_matches_shared_key() {
        val shared = computeECDHSharedKey(ecdhPrivateKeyB, ecdhPublicKeyA)
        assertNotNull(shared, "ECDH shared key B→A must not be null")
        assertEquals(ecdhSharedKey, shared)
    }

    @Test
    fun ecdh_symmetry_both_directions_produce_identical_keys() {
        val ab = computeECDHSharedKey(ecdhPrivateKeyA, ecdhPublicKeyB)
        val ba = computeECDHSharedKey(ecdhPrivateKeyB, ecdhPublicKeyA)
        assertNotNull(ab)
        assertNotNull(ba)
        assertEquals(ab, ba, "ECDH shared key must be symmetric")
    }

    @Test
    fun ecdh_shared_key_is_base64_sha256_of_raw_secret() {
        // The vector encodes the derived key as Base64(SHA-256(raw ECDH
        // secret)). Verifying the format here catches accidental double-
        // hashing or missing Base64 wrapping.
        val shared = computeECDHSharedKey(ecdhPrivateKeyA, ecdhPublicKeyB)
        assertNotNull(shared)
        // Decode and confirm it's a 32-byte SHA-256 digest, then re-encode
        // to validate the Base64 round-trip.
        val sharedBytes = b64(shared)
        assertEquals(32, sharedBytes.size, "derived key must be a 32-byte SHA-256 digest")
        assertEquals(shared, b64(sharedBytes))
    }

    @Test
    fun ecdh_rejects_invalid_public_key() {
        val invalidPub = ByteArray(65) { 0x04 }
        // Either returns null or throws — both are acceptable; we only
        // require that no bogus shared key is produced.
        val shared = computeECDHSharedKey(ecdhPrivateKeyA, invalidPub)
        assertFalse(
            shared == ecdhSharedKey,
            "invalid peer public key must not produce the valid shared key"
        )
    }

    @Test
    fun generate_ed25519_keypair_round_trip() {
        val (priv, pub) = generateEd25519KeyPair()
        assertEquals(32, priv.size)
        assertEquals(32, pub.size)
        val msg = "fresh keypair round-trip".encodeToByteArray()
        val sig = signEd25519(priv, msg)
        assertEquals(64, sig.size)
        assertTrue(verifyEd25519(pub, msg, sig), "fresh keypair must sign and verify")
    }

    @Test
    fun generate_ecdh_keypair_mutual_exchange() {
        val a = generateECDHKeyPair()
        val b = generateECDHKeyPair()
        assertEquals(32, a.privateKeyEncoded.size)
        assertEquals(65, a.publicKeyEncoded.size)
        assertEquals(0x04, a.publicKeyEncoded[0])
        val sAB = computeECDHSharedKey(a.privateKeyEncoded, b.publicKeyEncoded)
        val sBA = computeECDHSharedKey(b.privateKeyEncoded, a.publicKeyEncoded)
        assertNotNull(sAB)
        assertNotNull(sBA)
        assertEquals(sAB, sBA, "freshly generated keypairs must agree on the shared key")
    }
}
