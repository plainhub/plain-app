package com.ismartcoding.plain.platform

import com.ismartcoding.plain.crypto.ECDHKeyPair

expect fun chaCha20Decrypt(key: ByteArray, content: ByteArray): ByteArray?

expect fun chaCha20Encrypt(key: ByteArray, content: ByteArray): ByteArray

expect fun chaCha20Encrypt(key: ByteArray, content: String): ByteArray

expect fun verifyEd25519Signature(publicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean

expect fun generateChaCha20Key(): String

expect object PairingCrypto {
    fun generateECDHKeyPair(): ECDHKeyPair

    fun computeECDHSharedKey(privateKeyEncoded: ByteArray, peerPublicKeyEncoded: ByteArray): String?

    fun generateEd25519KeyPair(): Pair<ByteArray, ByteArray>

    fun signEd25519(rawPrivateKey: ByteArray, data: ByteArray): ByteArray

    fun verifyEd25519(rawPublicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean
}
