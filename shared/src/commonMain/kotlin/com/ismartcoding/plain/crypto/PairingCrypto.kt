package com.ismartcoding.plain.crypto

expect object PairingCrypto {
    fun generateECDHKeyPair(): ECDHKeyPair

    fun computeECDHSharedKey(privateKeyEncoded: ByteArray, peerPublicKeyEncoded: ByteArray): String?

    fun generateEd25519KeyPair(): Pair<ByteArray, ByteArray>

    fun signEd25519(rawPrivateKey: ByteArray, data: ByteArray): ByteArray

    fun verifyEd25519(rawPublicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean
}
