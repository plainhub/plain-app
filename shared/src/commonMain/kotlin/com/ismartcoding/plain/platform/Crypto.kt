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

/** Generate a fresh ECDH (secp256r1) key pair for pairing key exchange. */
expect fun generateECDHKeyPair(): ECDHKeyPair

/**
 * Compute the ECDH shared key from [privateKeyEncoded] and [peerPublicKeyEncoded].
 * Returns a Base64-encoded derived key, or null if computation fails.
 */
expect fun computeECDHSharedKey(privateKeyEncoded: ByteArray, peerPublicKeyEncoded: ByteArray): String?

/** Generate a raw Ed25519 key pair: (32-byte private key, 32-byte public key). */
expect fun generateEd25519KeyPair(): Pair<ByteArray, ByteArray>

/** Sign [data] with a raw 32-byte Ed25519 [rawPrivateKey]. Returns a 64-byte signature. */
expect fun signEd25519(rawPrivateKey: ByteArray, data: ByteArray): ByteArray

/** Verify an Ed25519 [signature] over [data] with a raw 32-byte [rawPublicKey]. */
expect fun verifyEd25519(rawPublicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean

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
