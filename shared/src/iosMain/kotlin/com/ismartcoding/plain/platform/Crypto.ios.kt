@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.crypto.ECDHKeyPair
import com.ismartcoding.plain.crypto.sha256
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFErrorCopyDescription
import platform.CoreFoundation.CFErrorGetCode
import platform.CoreFoundation.CFErrorRef
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFNumberCreate
import platform.CoreFoundation.CFStringGetCString
import platform.CoreFoundation.CFStringGetCStringPtr
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.kCFNumberSInt32Type
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Security.*
import platform.posix.memcpy
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual fun chaCha20Decrypt(key: ByteArray, content: ByteArray): ByteArray? =
    com.ismartcoding.plain.crypto.xChaCha20Poly1305Decrypt(key, content)

actual fun chaCha20Encrypt(key: ByteArray, content: ByteArray): ByteArray {
    val nonce = ByteArray(24)
    fillSecureRandom(nonce)
    return com.ismartcoding.plain.crypto.xChaCha20Poly1305Encrypt(key, nonce, content)
}

actual fun chaCha20Encrypt(key: ByteArray, content: String): ByteArray =
    chaCha20Encrypt(key, content.encodeToByteArray())

actual fun verifyEd25519Signature(publicKey: ByteArray, data: ByteArray, signature: ByteArray): Boolean =
    verifyEd25519(publicKey, data, signature)

actual fun generateChaCha20Key(): String {
    val bytes = ByteArray(32)
    if (!fillSecureRandom(bytes)) {
        throw RuntimeException("SecRandomCopyBytes failed")
    }
    return Base64.encode(bytes)
}

actual fun generateECDHKeyPair(): ECDHKeyPair = memScoped {
    val error = alloc<CFErrorRefVar>()
    error.value = null

    val attrs = createKeyAttributes("private")
    val privateKey = SecKeyCreateRandomKey(attrs, error.ptr)
        ?: throw RuntimeException("SecKeyCreateRandomKey failed: ${cfErrorInfo(error.value)}")
    val publicKey = SecKeyCopyPublicKey(privateKey)

    val privData = SecKeyCopyExternalRepresentation(privateKey, null)
        ?: throw RuntimeException("Failed to export ECDH private key")
    val pubData = SecKeyCopyExternalRepresentation(publicKey, null)
        ?: throw RuntimeException("Failed to export ECDH public key")

    val privBytes = privData.toByteArray()
    val pubBytes = pubData.toByteArray()
    // Apple exports EC private keys as X9.63: 04 || X(32) || Y(32) || D(32) = 97 bytes.
    // The raw scalar D is the last 32 bytes — that's what Android produces.
    val rawScalar = if (privBytes.size == 97 && privBytes[0] == 0x04.toByte()) {
        privBytes.copyOfRange(65, 97)
    } else {
        privBytes
    }

    ECDHKeyPair(
        privateKeyEncoded = rawScalar,
        publicKeyEncoded = pubBytes,
    )
}

@OptIn(ExperimentalEncodingApi::class)
actual fun computeECDHSharedKey(
    privateKeyEncoded: ByteArray,
    peerPublicKeyEncoded: ByteArray,
): String? {
    return try {
        memScoped {
            val error = alloc<CFErrorRefVar>()
            error.value = null

            val privAttrs = createKeyAttributes("private")
            val privateKey = SecKeyCreateWithData(
                wrapEcdhPrivateKeyX963(privateKeyEncoded).toCFData(),
                privAttrs,
                error.ptr,
            ) ?: run {
                LogCat.e("Failed to reconstruct ECDH private key: ${cfErrorInfo(error.value)}")
                return@memScoped null
            }

            error.value = null
            val pubAttrs = createKeyAttributes("public")
            val peerPublicKey = SecKeyCreateWithData(
                peerPublicKeyEncoded.toCFData(),
                pubAttrs,
                error.ptr,
            ) ?: run {
                LogCat.e("Failed to reconstruct peer ECDH public key: ${cfErrorInfo(error.value)}")
                return@memScoped null
            }

            error.value = null
            val sharedSecret = SecKeyCopyKeyExchangeResult(
                privateKey,
                kSecKeyAlgorithmECDHKeyExchangeStandard,
                peerPublicKey,
                null,
                error.ptr,
            ) ?: run {
                LogCat.e("ECDH key exchange failed: ${cfErrorInfo(error.value)}")
                return@memScoped null
            }

            val sharedBytes = sharedSecret.toByteArray()
            Base64.encode(sha256(sharedBytes))
        }
    } catch (e: Exception) {
        LogCat.e("ECDH key computation failed: ${e.message}")
        null
    }
}

actual fun generateEd25519KeyPair(): Pair<ByteArray, ByteArray> =
    com.ismartcoding.plain.crypto.ed25519GenerateKeyPair()

actual fun signEd25519(rawPrivateKey: ByteArray, data: ByteArray): ByteArray =
    com.ismartcoding.plain.crypto.ed25519Sign(rawPrivateKey, data)

actual fun verifyEd25519(
    rawPublicKey: ByteArray,
    data: ByteArray,
    signature: ByteArray,
): Boolean = com.ismartcoding.plain.crypto.ed25519Verify(rawPublicKey, data, signature)

private fun createKeyAttributes(keyClass: String?): CFDictionaryRef? = memScoped {
    val dict = CFDictionaryCreateMutable(null, 0, null, null)
        ?: return@memScoped null

    CFDictionaryAddValue(dict, kSecAttrKeyType, kSecAttrKeyTypeECSECPrimeRandom)

    val sizeVar = alloc<IntVar>()
    sizeVar.value = 256
    val sizeNumber = CFNumberCreate(null, kCFNumberSInt32Type, sizeVar.ptr)
    if (sizeNumber != null) {
        CFDictionaryAddValue(dict, kSecAttrKeySizeInBits, sizeNumber)
    }

    val classValue = when (keyClass) {
        "private" -> kSecAttrKeyClassPrivate
        "public" -> kSecAttrKeyClassPublic
        else -> null
    }
    if (classValue != null) {
        CFDictionaryAddValue(dict, kSecAttrKeyClass, classValue)
    }

    dict as CFDictionaryRef
}

private fun CFDataRef?.toByteArray(): ByteArray {
    if (this == null) return ByteArray(0)
    val length = CFDataGetLength(this).toInt()
    if (length == 0) return ByteArray(0)
    return ByteArray(length).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), CFDataGetBytePtr(this@toByteArray), length.toULong())
        }
    }
}

private fun ByteArray.toCFData(): CFDataRef? = usePinned { pinned ->
    CFDataCreate(null, pinned.addressOf(0).reinterpret<UByteVar>(), size.toLong())
}

/**
 * Wrap a raw 32-byte P-256 private key scalar in Apple's X9.63 private key
 * format: `04 || X || Y || D` (97 bytes). Apple's `SecKeyCreateWithData`
 * requires this format for EC private keys — the raw 32-byte scalar alone is
 * rejected, and so is SEC1 DER.
 *
 * The public key (X, Y) in this format is NOT validated against the scalar D
 * and is NOT used by `SecKeyCopyKeyExchangeResult`. We use the P-256 generator
 * point G as a placeholder — only the scalar D matters for ECDH.
 */
private fun wrapEcdhPrivateKeyX963(rawScalar: ByteArray): ByteArray {
    require(rawScalar.size == 32) { "P-256 private key must be 32 bytes, got ${rawScalar.size}" }
    // P-256 generator point G (uncompressed) as placeholder public key.
    // 04 || Gx(32) || Gy(32) = 65 bytes, then || D(32) = 97 bytes total.
    val g = hexToBytes(
        "04" +
            "6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296" +
            "4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5"
    )
    return g + rawScalar
}

private fun hexToBytes(hex: String): ByteArray {
    require(hex.length % 2 == 0)
    return ByteArray(hex.length / 2) { i ->
        val hi = hexNibble(hex[i * 2])
        val lo = hexNibble(hex[i * 2 + 1])
        ((hi shl 4) or lo).toByte()
    }
}

private fun hexNibble(c: Char): Int = when (c) {
    in '0'..'9' -> c - '0'
    in 'a'..'f' -> c - 'a' + 10
    in 'A'..'F' -> c - 'A' + 10
    else -> throw IllegalArgumentException("invalid hex char: $c")
}

private fun cfErrorInfo(error: CFErrorRef?): String {
    if (error == null) return "no error"
    val code = CFErrorGetCode(error)
    val desc = CFErrorCopyDescription(error)
    val descStr = cfStringToString(desc) ?: "no description"
    return "code=$code, desc=$descStr"
}

private fun cfStringToString(cfString: CFStringRef?): String? {
    if (cfString == null) return null
    val ptr = CFStringGetCStringPtr(cfString, kCFStringEncodingUTF8)
    if (ptr != null) return ptr.toKString()
    val buffer = ByteArray(256)
    buffer.usePinned { pinned ->
        if (CFStringGetCString(cfString, pinned.addressOf(0), buffer.size.toLong(), kCFStringEncodingUTF8)) {
            return pinned.addressOf(0).toKString()
        }
    }
    return null
}

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
