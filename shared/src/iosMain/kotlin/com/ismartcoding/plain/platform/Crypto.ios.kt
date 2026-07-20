@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.crypto.ECDHKeyPair
import com.ismartcoding.plain.crypto.sha256
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.toByteArray
import com.ismartcoding.plain.lib.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFStringRef
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.numberWithInt
import platform.Security.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Apple's `kSecAttrKeyTypeEd25519` and `kSecKeyAlgorithmEdDSASignatureMessageEd25519`
 * constants (added in iOS 13 / macOS 10.15) are NOT exposed through the
 * Kotlin/Native cinterop bindings for `platform.Security` shipped with this
 * toolchain. They follow Apple's `SEC_CONST_DECL(k, v)` convention where the
 * CFString value equals the symbol name minus its `kSecAttrKeyType` /
 * `kSecKeyAlgorithm` prefix, so we construct the equivalent CFStrings here.
 *
 * NSString and CFStringRef are toll-free bridged on Apple platforms, so
 * constructing via NSString and casting gives a value that SecKey APIs accept.
 */
private val kSecAttrKeyTypeEd25519Compat: CFStringRef =
    ("Ed25519" as NSString) as CFStringRef

private val kSecKeyAlgorithmEdDSASignatureMessageEd25519Compat: CFStringRef =
    ("EdDSASignatureMessageEd25519" as NSString) as CFStringRef

/**
 * iOS XChaCha20-Poly1305 is not implemented: Apple's Security framework does not
 * expose ChaCha20-Poly1305 AEAD through Kotlin/Native cinterop bindings, and
 * CommonCrypto is not auto-imported. Callers that need transport encryption
 * should remain on Android. These stubs keep the KMP contract compiling on iOS
 * and intentionally return empty/null so misuse is detectable.
 */
actual fun chaCha20Decrypt(key: ByteArray, content: ByteArray): ByteArray? = null

actual fun chaCha20Encrypt(key: ByteArray, content: ByteArray): ByteArray = ByteArray(0)

actual fun chaCha20Encrypt(key: ByteArray, content: String): ByteArray = ByteArray(0)

actual fun verifyEd25519Signature(publicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean =
    PairingCrypto.verifyEd25519(publicKey, data, signature)

actual fun generateChaCha20Key(): String {
    val bytes = ByteArray(32)
    if (!fillSecureRandom(bytes)) {
        throw RuntimeException("SecRandomCopyBytes failed")
    }
    return Base64.encode(bytes)
}

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

    /**
     * Generate a raw Ed25519 key pair using Apple Security framework.
     * Returns 32-byte private key and 32-byte public key (RFC 8032 raw format),
     * matching Android's Tink-based output for cross-platform compatibility.
     */
    actual fun generateEd25519KeyPair(): Pair<ByteArray, ByteArray> {
        val attrs = createKeyAttributes(kSecAttrKeyTypeEd25519Compat, 256)
        val privateKey = SecKeyCreateRandomKey(attrs, null)
            ?: throw RuntimeException("Failed to generate Ed25519 key pair")
        val publicKey = SecKeyCopyPublicKey(privateKey)

        val privData = SecKeyCopyExternalRepresentation(privateKey, null)
            ?: throw RuntimeException("Failed to export Ed25519 private key")
        val pubData = SecKeyCopyExternalRepresentation(publicKey, null)
            ?: throw RuntimeException("Failed to export Ed25519 public key")

        return Pair(privData.toByteArray(), pubData.toByteArray())
    }

    /**
     * Sign [data] with raw 32-byte Ed25519 [rawPrivateKey] using Apple Security framework.
     * Returns a 64-byte Ed25519 signature (RFC 8032).
     */
    actual fun signEd25519(rawPrivateKey: ByteArray, data: ByteArray): ByteArray {
        require(rawPrivateKey.size == 32) {
            "Ed25519 private key must be 32 bytes, got ${rawPrivateKey.size}"
        }
        val privAttrs = createKeyAttributes(
            kSecAttrKeyTypeEd25519Compat, 256, kSecAttrKeyClassPrivate
        )
        val privateKey = SecKeyCreateWithData(rawPrivateKey.toCFData(), privAttrs, null)
            ?: throw RuntimeException("Failed to reconstruct Ed25519 private key")

        val signature = memScoped {
            val errorPtr = alloc<CFErrorRefVar>().ptr
            SecKeyCreateSignature(
                privateKey,
                kSecKeyAlgorithmEdDSASignatureMessageEd25519Compat,
                data.toNSData() as CFDataRef,
                errorPtr,
            )
        } ?: throw RuntimeException("SecKeyCreateSignature returned null")

        return signature.toByteArray()
    }

    /**
     * Verify an Ed25519 [signature] over [data] with raw 32-byte [rawPublicKey]
     * using Apple Security framework.
     */
    actual fun verifyEd25519(
        rawPublicKey: ByteArray,
        data: ByteArray,
        signature: ByteArray,
    ): Boolean {
        require(rawPublicKey.size == 32) {
            "Ed25519 public key must be 32 bytes, got ${rawPublicKey.size}"
        }
        return try {
            val pubAttrs = createKeyAttributes(
                kSecAttrKeyTypeEd25519Compat, 256, kSecAttrKeyClassPublic
            )
            val publicKey = SecKeyCreateWithData(rawPublicKey.toCFData(), pubAttrs, null)
                ?: run {
                    LogCat.e("Failed to reconstruct Ed25519 public key")
                    return false
                }

            memScoped {
                val errorPtr = alloc<CFErrorRefVar>().ptr
                SecKeyVerifySignature(
                    publicKey,
                    kSecKeyAlgorithmEdDSASignatureMessageEd25519Compat,
                    data.toNSData() as CFDataRef,
                    signature.toNSData() as CFDataRef,
                    errorPtr,
                )
            }
        } catch (e: Exception) {
            LogCat.e("Ed25519 verify failed: ${e.message}")
            false
        }
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

/**
 * Fill [buffer] with cryptographically secure random bytes via `SecRandomCopyBytes`.
 * Returns false if the call fails (e.g. invalid parameters).
 */
private fun fillSecureRandom(buffer: ByteArray): Boolean {
    if (buffer.isEmpty()) return true
    // SecRandomCopyBytes returns errSecSuccess (0) on success.
    // count parameter is ULong in Kotlin/Native Security bindings.
    val status = buffer.usePinned { pinned ->
        SecRandomCopyBytes(kSecRandomDefault, buffer.size.toULong(), pinned.addressOf(0))
    }
    return status == 0
}
