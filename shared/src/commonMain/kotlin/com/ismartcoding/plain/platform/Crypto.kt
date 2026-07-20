package com.ismartcoding.plain.platform

import com.ismartcoding.plain.crypto.ECDHKeyPair
import com.ismartcoding.plain.crypto.sha1 as sha1Bytes
import com.ismartcoding.plain.crypto.sha512 as sha512Bytes
import com.ismartcoding.plain.lib.extensions.toHexString
import kotlin.random.Random

/**
 * Decrypt [content] with XChaCha20-Poly1305 using raw 32-byte [key].
 * Returns null if the ciphertext is invalid or authentication fails.
 */
expect fun chaCha20Decrypt(key: ByteArray, content: ByteArray): ByteArray?

/**
 * Encrypt [content] with XChaCha20-Poly1305 using raw 32-byte [key].
 */
expect fun chaCha20Encrypt(key: ByteArray, content: ByteArray): ByteArray

/**
 * Encrypt [content] (UTF-8 encoded) with XChaCha20-Poly1305 using raw 32-byte [key].
 */
expect fun chaCha20Encrypt(key: ByteArray, content: String): ByteArray

/**
 * Verify an Ed25519 signature over [data] using raw 32-byte [publicKey].
 */
expect fun verifyEd25519Signature(publicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean

/**
 * Generate a fresh Base64-encoded 32-byte XChaCha20 key.
 */
expect fun generateChaCha20Key(): String

/** Pairing crypto primitives shared across platforms (Ed25519 + ECDH). */
expect object PairingCrypto {
    fun generateECDHKeyPair(): ECDHKeyPair

    fun computeECDHSharedKey(privateKeyEncoded: ByteArray, peerPublicKeyEncoded: ByteArray): String?

    fun generateEd25519KeyPair(): Pair<ByteArray, ByteArray>

    fun signEd25519(rawPrivateKey: ByteArray, data: ByteArray): ByteArray

    fun verifyEd25519(rawPublicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean
}

/** SHA-1 of [input] as a lowercase hex string. Use only for non-security-critical hashing. */
fun sha1(input: ByteArray): String = sha1Bytes(input).toHexString()

/** SHA-512 of [input] as a lowercase hex string. */
fun sha512(input: ByteArray): String = sha512Bytes(input).toHexString()

/**
 * Generate a random alphanumeric password of length [n] using a human-readable
 * charset that excludes ambiguous characters (0/O, 1/I/l).
 */
fun randomPassword(n: Int): String {
    val characterSet = "23456789abcdefghijkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ"
    val random = Random.Default
    val password = StringBuilder()
    for (i in 0 until n) {
        val rIndex = random.nextInt(characterSet.length)
        password.append(characterSet[rIndex])
    }
    return password.toString()
}
