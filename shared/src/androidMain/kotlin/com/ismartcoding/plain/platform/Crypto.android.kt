package com.ismartcoding.plain.platform

import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.signature.SignatureConfig
import com.google.crypto.tink.subtle.Ed25519Sign
import com.google.crypto.tink.subtle.Ed25519Verify
import com.google.crypto.tink.subtle.XChaCha20Poly1305
import com.ismartcoding.plain.crypto.ECDHKeyPair
import com.ismartcoding.plain.lib.logcat.LogCat
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.KeyAgreement
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// Cache XChaCha20Poly1305 instances per key to avoid expensive re-construction on every encrypt/decrypt.
// Key is the hex representation of the raw key bytes.
private val aeadCache = ConcurrentHashMap<String, XChaCha20Poly1305>()

private fun getAead(key: ByteArray): XChaCha20Poly1305 {
    val keyHex = key.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
    return aeadCache.getOrPut(keyHex) { XChaCha20Poly1305(key) }
}

private val tinkInitLock = Any()

@Volatile
private var tinkInitialized = false

private fun initializeTink() {
    if (!tinkInitialized) {
        synchronized(tinkInitLock) {
            if (!tinkInitialized) {
                try {
                    AeadConfig.register()
                    SignatureConfig.register()
                    tinkInitialized = true
                    LogCat.d("Google Tink initialized successfully (Ed25519 signatures + XChaCha20-Poly1305 AEAD)")
                } catch (ex: Exception) {
                    LogCat.e("Failed to initialize Google Tink: ${ex.message}")
                    throw ex
                }
            }
        }
    }
}

actual fun chaCha20Decrypt(key: ByteArray, content: ByteArray): ByteArray? =
    try {
        getAead(key).decrypt(content, null)
    } catch (ex: Exception) {
        null
    }

actual fun chaCha20Encrypt(key: ByteArray, content: ByteArray): ByteArray =
    getAead(key).encrypt(content, null)

actual fun chaCha20Encrypt(key: ByteArray, content: String): ByteArray =
    chaCha20Encrypt(key, content.toByteArray())

actual fun verifyEd25519Signature(publicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean {
    initializeTink()
    return try {
        Ed25519Verify(publicKey).verify(signature, data)
        true
    } catch (ex: Exception) {
        false
    }
}

actual fun generateChaCha20Key(): String {
    val bytes = ByteArray(32) // XChaCha20 uses 32-byte keys
    SecureRandom.getInstanceStrong().nextBytes(bytes)
    return Base64.encode(bytes)
}

@OptIn(ExperimentalEncodingApi::class)
actual object PairingCrypto {

    actual fun generateECDHKeyPair(): ECDHKeyPair {
        val keyPairGen = KeyPairGenerator.getInstance("EC")
        keyPairGen.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = keyPairGen.generateKeyPair()
        val pub = keyPair.public as ECPublicKey
        val priv = keyPair.private as ECPrivateKey
        return ECDHKeyPair(
            privateKeyEncoded = encodePrivateKeyBytes(priv),
            publicKeyEncoded = encodePublicKeyX963(pub),
        )
    }

    actual fun computeECDHSharedKey(
        privateKeyEncoded: ByteArray,
        peerPublicKeyEncoded: ByteArray,
    ): String? {
        return try {
            val params = getSecp256r1Params()
            val privSpec = ECPrivateKeySpec(BigInteger(1, privateKeyEncoded), params)
            val privateKey = KeyFactory.getInstance("EC").generatePrivate(privSpec)

            val peerPoint = decodeX963(peerPublicKeyEncoded)
            val pubSpec = ECPublicKeySpec(peerPoint, params)
            val peerPublicKey = KeyFactory.getInstance("EC").generatePublic(pubSpec)

            // Reconstruct the public key from bytes and perform ECDH key agreement
            val keyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(privateKey)
            keyAgreement.doPhase(peerPublicKey, true)

            val sharedSecret = keyAgreement.generateSecret()

            // Derive XChaCha20 key from shared secret via SHA-256
            val xChaCha20KeyBytes = MessageDigest.getInstance("SHA-256").digest(sharedSecret)
            Base64.encode(xChaCha20KeyBytes)
        } catch (e: Exception) {
            LogCat.e("ECDH key computation failed: ${e.message}")
            null
        }
    }

    actual fun generateEd25519KeyPair(): Pair<ByteArray, ByteArray> {
        initializeTink()
        val keyPair = Ed25519Sign.KeyPair.newKeyPair()
        return Pair(keyPair.privateKey, keyPair.publicKey)
    }

    actual fun signEd25519(rawPrivateKey: ByteArray, data: ByteArray): ByteArray {
        require(rawPrivateKey.size == 32) { "Ed25519 private key must be 32 bytes, got ${rawPrivateKey.size}" }
        initializeTink()
        return Ed25519Sign(rawPrivateKey).sign(data)
    }

    actual fun verifyEd25519(
        rawPublicKey: ByteArray,
        data: ByteArray,
        signature: ByteArray,
    ): Boolean {
        initializeTink()
        return try {
            Ed25519Verify(rawPublicKey).verify(signature, data)
            true
        } catch (ex: Exception) {
            false
        }
    }

    private fun encodePublicKeyX963(pub: ECPublicKey): ByteArray {
        val x = bigIntegerToFixedBytes(pub.w.affineX, 32)
        val y = bigIntegerToFixedBytes(pub.w.affineY, 32)
        return ByteArray(65).also { out ->
            out[0] = 0x04
            System.arraycopy(x, 0, out, 1, 32)
            System.arraycopy(y, 0, out, 33, 32)
        }
    }

    private fun encodePrivateKeyBytes(priv: ECPrivateKey): ByteArray {
        return bigIntegerToFixedBytes(priv.s, 32)
    }

    private fun decodeX963(bytes: ByteArray): ECPoint {
        require(bytes.size == 65 && bytes[0].toInt() == 0x04) {
            "Invalid X9.63 public key format: expected 65 bytes with 0x04 prefix"
        }
        val x = BigInteger(1, bytes.copyOfRange(1, 33))
        val y = BigInteger(1, bytes.copyOfRange(33, 65))
        return ECPoint(x, y)
    }

    private fun bigIntegerToFixedBytes(value: BigInteger, length: Int): ByteArray {
        val raw = value.toByteArray()
        return when {
            raw.size == length -> raw
            raw.size == length + 1 && raw[0].toInt() == 0 -> raw.copyOfRange(1, raw.size)
            raw.size < length -> ByteArray(length).also { System.arraycopy(raw, 0, it, length - raw.size, raw.size) }
            else -> raw.copyOfRange(raw.size - length, raw.size)
        }
    }

    private var cachedParams: ECParameterSpec? = null

    private fun getSecp256r1Params(): ECParameterSpec {
        cachedParams?.let { return it }
        val keyPairGen = KeyPairGenerator.getInstance("EC")
        keyPairGen.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = keyPairGen.generateKeyPair()
        val params = (keyPair.public as ECPublicKey).params
        cachedParams = params
        return params
    }
}
