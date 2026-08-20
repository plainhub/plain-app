package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.lib.crypto.hmacSha256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class ShareCryptoTest {
    private val secret = ByteArray(32) { it.toByte() }

    // ── HMAC-SHA256 RFC 4231 vectors (shared-lib primitive) ────────────────

    @Test
    fun hmac_sha256_matches_rfc4231_test_case_1() {
        // Key = 0x0b repeated 20 times, data = "Hi There"
        val key = ByteArray(20) { 0x0b.toByte() }
        val data = "Hi There".encodeToByteArray()
        val expected = hex("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7")
        assertContentEquals(expected, hmacSha256(key, data))
    }

    @Test
    fun hmac_sha256_matches_rfc4231_test_case_2() {
        // Key = "Jefe", data = "what do ya want for nothing?"
        val key = "Jefe".encodeToByteArray()
        val data = "what do ya want for nothing?".encodeToByteArray()
        val expected = hex("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843")
        assertContentEquals(expected, hmacSha256(key, data))
    }

    // ── Token derivation ─────────────────────────────────────────────────────

    @Test
    fun derived_token_is_32_bytes() {
        assertEquals(32, ShareCrypto.deriveSharedToken(secret, "share-1").size)
    }

    @Test
    fun derived_token_is_deterministic() {
        val a = ShareCrypto.deriveSharedToken(secret, "share-1")
        val b = ShareCrypto.deriveSharedToken(secret, "share-1")
        assertContentEquals(a, b)
    }

    @Test
    fun different_shared_id_produces_different_token() {
        val a = ShareCrypto.deriveSharedToken(secret, "share-1")
        val b = ShareCrypto.deriveSharedToken(secret, "share-2")
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun different_secret_produces_different_token() {
        val a = ShareCrypto.deriveSharedToken(secret, "share-1")
        val b = ShareCrypto.deriveSharedToken(ByteArray(32) { 1 }, "share-1")
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun encoded_token_round_trips_to_raw() {
        val raw = ShareCrypto.deriveSharedToken(secret, "share-1")
        val encoded = ShareCrypto.deriveSharedTokenEncoded(secret, "share-1")
        assertContentEquals(raw, Base64.UrlSafe.decode(encoded))
    }

    @Test
    fun encoded_token_uses_url_safe_base64() {
        val encoded = ShareCrypto.deriveSharedTokenEncoded(secret, "share-1")
        // Url-safe alphabet: no '+' or '/' (padding '=' is permitted).
        assertFalse(encoded.contains('+'))
        assertFalse(encoded.contains('/'))
        assertTrue(encoded.isNotEmpty())
    }

    // ── ID generation ────────────────────────────────────────────────────────

    @Test
    fun new_shared_ids_are_unique() {
        val ids = (1..100).map { ShareCrypto.newSharedId() }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun new_url_tokens_are_unique() {
        val tokens = (1..100).map { ShareCrypto.newUrlToken() }
        assertEquals(tokens.size, tokens.toSet().size)
    }

    private fun hex(s: String): ByteArray =
        s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
