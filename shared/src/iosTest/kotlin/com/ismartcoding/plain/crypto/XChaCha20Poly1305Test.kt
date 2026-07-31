package com.ismartcoding.plain.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XChaCha20Poly1305Test {

    private fun hex(s: String): ByteArray {
        require(s.length % 2 == 0)
        return ByteArray(s.length / 2) { i ->
            val hi = s[i * 2].digitToInt(16)
            val lo = s[i * 2 + 1].digitToInt(16)
            ((hi shl 4) or lo).toByte()
        }
    }

    private fun bytesToHex(b: ByteArray): String =
        b.joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    @Test
    fun `RFC 8439 Section 2_4_2 ChaCha20 encryption test vector`() {
        val key = hex("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")
        val nonce12 = hex("000000090000004a00000000")
        val plaintext = ("Ladies and Gentlemen of the class of '99: If I could offer you " +
            "only one tip for the future, sunscreen would be it.").encodeToByteArray()

        val ciphertext = chacha20EncryptTestHelper(key, nonce12, 1, plaintext)
        val expectedHex = "5c90838db44879743e6bfd58c64e05a8" +
            "a2bc91a913af0e23704acfbaa0b80d3d" +
            "a1a20b2027b893302ee29e63f9c222c1" +
            "da67f0b5fe7928dfaea2a391cd251c21" +
            "64e4fa5756b9da6e8ca5dc908c44cbf6" +
            "e93ea6b4cc406988d7da69bf795bf19b" +
            "84539df73bd9b3e9ca4d03bc0a586ff5" +
            "28dc"
        assertEquals(expectedHex, bytesToHex(ciphertext))
    }

    @Test
    fun `RFC 7539 Poly1305 test vector`() {
        val key = hex("85d6be7857556d337f4452fe42d506a80103808afb0db2fd4abff6af4149f51b")
        val data = ("Cryptographic Forum Research Group").encodeToByteArray()
        val tag = poly1305MacTestHelper(key, data)
        val expectedHex = "a8061dc1305136c6c22b8baf0c0127a9"
        assertEquals(expectedHex, bytesToHex(tag))
    }

    @Test
    fun `RFC 8439 Section 2_8_2 AEAD encrypt test vector`() {
        val key = hex("808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9f")
        val nonce12 = hex("070000004041424344454647")
        val plaintext = ("Ladies and Gentlemen of the class of '99: If I could offer you " +
            "only one tip for the future, sunscreen would be it.").encodeToByteArray()
        val aad = hex("50515253c0c1c2c3c4c5c6c7")

        val result = chacha20Poly1305AeadEncrypt(key, nonce12, plaintext, aad)
        val expectedCiphertext = "d31a8d34648e60db7b86afbc53ef7ec2" +
            "a4aded51296e08fea9e2b5a736ee62d6" +
            "3dbea45e8ca9671282fafb69da92728b" +
            "1a71de0a9e060b2905d6a5b67ecd3b36" +
            "92ddbd7f2d778b8c9803aee328091b58" +
            "fab324e4fad675945585808b4831d7bc" +
            "3ff4def08e4b7a9de576d26586cec64b" +
            "6116"
        val expectedTag = "1ae10b594f09e26a7e902ecbd0600691"

        val ct = result.copyOfRange(0, result.size - 16)
        val tag = result.copyOfRange(result.size - 16, result.size)
        assertEquals(expectedCiphertext, bytesToHex(ct))
        assertEquals(expectedTag, bytesToHex(tag))
    }

    @Test
    fun `encrypt-decrypt roundtrip with random nonce`() {
        val key = hex("0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20")
        val plaintext = "Hello, XChaCha20-Poly1305 on iOS!".encodeToByteArray()
        val nonce = hex("070000004041424344454647484950515253545556575859")
        val aad = hex("50515253c0c1c2c3c4c5c6c7")

        val encrypted = xChaCha20Poly1305Encrypt(key, nonce, plaintext, aad)
        assertEquals(24 + plaintext.size + 16, encrypted.size)

        val decrypted = xChaCha20Poly1305Decrypt(key, encrypted, aad)
        assertNotNull(decrypted)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun `encrypt-decrypt roundtrip no AAD`() {
        val key = ByteArray(32) { it.toByte() }
        val plaintext = "Test message for XChaCha20".encodeToByteArray()
        val nonce = ByteArray(24) { (it + 1).toByte() }

        val encrypted = xChaCha20Poly1305Encrypt(key, nonce, plaintext)
        val decrypted = xChaCha20Poly1305Decrypt(key, encrypted)
        assertNotNull(decrypted)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun `empty plaintext roundtrip`() {
        val key = ByteArray(32) { (it * 3).toByte() }
        val nonce = ByteArray(24) { (it * 5).toByte() }
        val plaintext = ByteArray(0)

        val encrypted = xChaCha20Poly1305Encrypt(key, nonce, plaintext)
        assertEquals(24 + 0 + 16, encrypted.size)
        val decrypted = xChaCha20Poly1305Decrypt(key, encrypted)
        assertNotNull(decrypted)
        assertEquals(0, decrypted.size)
    }

    @Test
    fun `large plaintext roundtrip`() {
        val key = ByteArray(32) { (it * 7 + 1).toByte() }
        val nonce = ByteArray(24) { (it * 11 + 3).toByte() }
        val plaintext = ByteArray(100_000) { (it and 0xff).toByte() }

        val encrypted = xChaCha20Poly1305Encrypt(key, nonce, plaintext)
        val decrypted = xChaCha20Poly1305Decrypt(key, encrypted)
        assertNotNull(decrypted)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun `decrypt rejects wrong key`() {
        val key = ByteArray(32) { it.toByte() }
        val wrongKey = ByteArray(32) { (it + 1).toByte() }
        val nonce = ByteArray(24) { (it + 1).toByte() }
        val plaintext = "secret".encodeToByteArray()

        val encrypted = xChaCha20Poly1305Encrypt(key, nonce, plaintext)
        val result = xChaCha20Poly1305Decrypt(wrongKey, encrypted)
        assertNull(result)
    }

    @Test
    fun `decrypt rejects tampered ciphertext`() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(24) { (it + 1).toByte() }
        val plaintext = "secret message".encodeToByteArray()

        val encrypted = xChaCha20Poly1305Encrypt(key, nonce, plaintext)
        encrypted[30] = (encrypted[30].toInt() xor 0xff).toByte()
        val result = xChaCha20Poly1305Decrypt(key, encrypted)
        assertNull(result)
    }

    @Test
    fun `decrypt rejects tampered tag`() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(24) { (it + 1).toByte() }
        val plaintext = "secret message".encodeToByteArray()

        val encrypted = xChaCha20Poly1305Encrypt(key, nonce, plaintext)
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1].toInt() xor 0x01).toByte()
        val result = xChaCha20Poly1305Decrypt(key, encrypted)
        assertNull(result)
    }

    @Test
    fun `decrypt rejects wrong AAD`() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(24) { (it + 1).toByte() }
        val aad = hex("01020304")
        val wrongAad = hex("01020305")
        val plaintext = "secret".encodeToByteArray()

        val encrypted = xChaCha20Poly1305Encrypt(key, nonce, plaintext, aad)
        val result = xChaCha20Poly1305Decrypt(key, encrypted, wrongAad)
        assertNull(result)
    }

    @Test
    fun `decrypt rejects too-short input`() {
        val key = ByteArray(32) { it.toByte() }
        assertNull(xChaCha20Poly1305Decrypt(key, ByteArray(10)))
        assertNull(xChaCha20Poly1305Decrypt(key, ByteArray(39))) // 24 + 15 < 24 + 16
    }

    @Test
    fun `HChaCha20 test vector from draft-irtf-cfrg-xchacha`() {
        val key = hex("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f")
        val nonce16 = hex("000000090000004a0000000031415927")
        val subKey = hChaCha20TestHelper(key, nonce16)
        val expected = "82413b4227b27bfed30e42508a877d73" +
            "a0f9e4d58a74a853c12ec41326d3ecdc"
        assertEquals(expected, bytesToHex(subKey))
    }

    @Test
    fun `cross-platform compatible with frontend format`() {
        val key = ByteArray(32) { (it * 2 + 1).toByte() }
        val message = """{"query":"query { chats { id } }"}""".encodeToByteArray()
        val nonce = ByteArray(24) { (it + 10).toByte() }

        val encrypted = xChaCha20Poly1305Encrypt(key, nonce, message)
        assertTrue(encrypted.size == 24 + message.size + 16)

        val nonceExtracted = encrypted.copyOfRange(0, 24)
        assertContentEquals(nonce, nonceExtracted)

        val ct = encrypted.copyOfRange(24, 24 + message.size)
        val tag = encrypted.copyOfRange(24 + message.size, encrypted.size)
        assertEquals(16, tag.size)

        val decrypted = xChaCha20Poly1305Decrypt(key, encrypted)
        assertNotNull(decrypted)
        assertContentEquals(message, decrypted)
    }

    @Test
    fun `cross-compatibility with PyNaCl output`() {
        val key = hex("0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20")
        val nonce = hex("070000004041424344454647484950515253545556575859")
        val plaintext = "Hello, XChaCha20-Poly1305 on iOS!".encodeToByteArray()
        val pynaclFull = hex(
            "070000004041424344454647484950515253545556575859" +
                "023caad77752ce1384a31d89eaacbaaa46b12c7bc8ba66a0e88f84c0b002d1e834" +
                "4338f82b5a5f8af3a7a0757b5cbb0882"
        )

        val decrypted = xChaCha20Poly1305Decrypt(key, pynaclFull)
        assertNotNull(decrypted)
        assertContentEquals(plaintext, decrypted)

        val encrypted = xChaCha20Poly1305Encrypt(key, nonce, plaintext)
        assertContentEquals(pynaclFull, encrypted)
    }

    // ── Helpers to access internal functions ──────────────────────────

    private fun chacha20EncryptTestHelper(key: ByteArray, nonce12: ByteArray, counter: Int, data: ByteArray): ByteArray {
        // Use reflection-free approach: call the internal chacha20Encrypt
        // which is package-private in XChaCha20Poly1305.kt
        return chacha20EncryptInternal(key, nonce12, counter, data)
    }

    private fun poly1305MacTestHelper(key: ByteArray, data: ByteArray): ByteArray {
        return poly1305MacInternal(key, data)
    }

    private fun chacha20Poly1305AeadEncrypt(key: ByteArray, nonce12: ByteArray, plaintext: ByteArray, aad: ByteArray): ByteArray {
        // Use ChaCha20-Poly1305 (not XChaCha20) with explicit 12-byte nonce
        return chaCha20Poly1305EncryptInternal(key, nonce12, plaintext, aad)
    }

    private fun hChaCha20TestHelper(key: ByteArray, nonce16: ByteArray): ByteArray {
        return hChaCha20Internal(key, nonce16)
    }
}
