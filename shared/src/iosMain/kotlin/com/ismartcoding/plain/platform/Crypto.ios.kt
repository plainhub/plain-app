@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.crypto.ECDHKeyPair
import com.ismartcoding.plain.crypto.sha256
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.lib.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.numberWithInt
import platform.Security.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual fun chaCha20Decrypt(key: ByteArray, content: ByteArray): ByteArray? = null

actual fun chaCha20Encrypt(key: ByteArray, content: ByteArray): ByteArray = ByteArray(0)

actual fun chaCha20Encrypt(key: ByteArray, content: String): ByteArray = ByteArray(0)

actual fun verifyEd25519Signature(publicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean = false

actual fun generateChaCha20Key(): String = ""

@OptIn(ExperimentalEncodingApi::class)
actual object PairingCrypto {

    actual fun generateECDHKeyPair(): ECDHKeyPair {
        val attrs = createKeyAttributes(kSecAttrKeyTypeECSECPrimeRandom, 256)
        val privateKey = SecKeyCreateRandomKey(attrs, null)
            ?: throw RuntimeException("Failed to generate ECDH key pair")
        val publicKey = SecKeyCopyPublicKey(privateKey)

        val privData = SecKeyCopyExternalRepresentation(privateKey, null)
            ?: throw RuntimeException("Failed to export private key")
        val pubData = SecKeyCopyExternalRepresentation(publicKey, null)
            ?: throw RuntimeException("Failed to export public key")

        return ECDHKeyPair(
            privateKeyEncoded = privData.toByteArray(),
            publicKeyEncoded = pubData.toByteArray(),
        )
    }

    actual fun computeECDHSharedKey(
        privateKeyEncoded: ByteArray,
        peerPublicKeyEncoded: ByteArray,
    ): String? {
        return try {
            val privAttrs = createKeyAttributes(
                kSecAttrKeyTypeECSECPrimeRandom, 256, kSecAttrKeyClassPrivate
            )
            val privateKey = SecKeyCreateWithData(
                privateKeyEncoded.toCFData(),
                privAttrs,
                null,
            ) ?: run {
                LogCat.e("Failed to reconstruct ECDH private key")
                return null
            }

            val pubAttrs = createKeyAttributes(
                kSecAttrKeyTypeECSECPrimeRandom, 256, kSecAttrKeyClassPublic
            )
            val peerPublicKey = SecKeyCreateWithData(
                peerPublicKeyEncoded.toCFData(),
                pubAttrs,
                null,
            ) ?: run {
                LogCat.e("Failed to reconstruct peer ECDH public key")
                return null
            }

            val sharedSecret = SecKeyCopyKeyExchangeResult(
                privateKey,
                kSecKeyAlgorithmECDHKeyExchangeStandard,
                peerPublicKey,
                null,
                null,
            ) ?: run {
                LogCat.e("ECDH key exchange failed")
                return null
            }

            val sharedBytes = sharedSecret.toByteArray()
            Base64.encode(sha256(sharedBytes))
        } catch (e: Exception) {
            LogCat.e("ECDH key computation failed: ${e.message}")
            null
        }
    }

    actual fun generateEd25519KeyPair(): Pair<ByteArray, ByteArray> {
        throw NotImplementedError("Ed25519 not yet supported on iOS")
    }

    actual fun signEd25519(rawPrivateKey: ByteArray, data: ByteArray): ByteArray {
        throw NotImplementedError("Ed25519 not yet supported on iOS")
    }

    actual fun verifyEd25519(
        rawPublicKey: ByteArray,
        data: ByteArray,
        signature: ByteArray,
    ): Boolean {
        throw NotImplementedError("Ed25519 not yet supported on iOS")
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun createKeyAttributes(
        keyType: CFStringRef?,
        keySize: Int,
        keyClass: CFStringRef? = null,
    ): CFDictionaryRef? {
        val attrs = NSMutableDictionary()
        attrs.setObject(keyType as NSString, forKey = kSecAttrKeyType as NSString)
        attrs.setObject(NSNumber.numberWithInt(keySize), forKey = kSecAttrKeySizeInBits as NSString)
        if (keyClass != null) {
            attrs.setObject(keyClass as NSString, forKey = kSecAttrKeyClass as NSString)
        }
        return attrs as CFDictionaryRef
    }
}

private fun CFDataRef?.toByteArray(): ByteArray {
    if (this == null) return ByteArray(0)
    return (this as NSData).toByteArray()
}

private fun ByteArray.toCFData(): CFDataRef? = toNSData() as? CFDataRef
