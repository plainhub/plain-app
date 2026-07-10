package com.ismartcoding.plain.crypto

import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.Security.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual object PairingCrypto {

    actual fun generateECDHKeyPair(): ECDHKeyPair {
        memScoped {
            val error = alloc<CFErrorRefVar>()
            val attributes = mapOf(
                kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
                kSecAttrKeySizeInBits to 256,
            )
            val privateKey = SecKeyCreateRandomKey(attributes.asCFDictionary(), error.ptr)
                ?: throw RuntimeException("Failed to generate ECDH key pair: ${error.value}")
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
    }

    actual fun computeECDHSharedKey(
        privateKeyEncoded: ByteArray,
        peerPublicKeyEncoded: ByteArray,
    ): String? {
        return try {
            memScoped {
                val error = alloc<CFErrorRefVar>()

                val privAttrs = mapOf(
                    kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
                    kSecAttrKeyClass to kSecAttrKeyClassPrivate,
                    kSecAttrKeySizeInBits to 256,
                )
                val privateKey = SecKeyCreateWithData(
                    privateKeyEncoded.toNSData(),
                    privAttrs.asCFDictionary(),
                    error.ptr,
                ) ?: run {
                    LogCat.e("Failed to reconstruct ECDH private key: ${error.value}")
                    return@memScoped null
                }

                val pubAttrs = mapOf(
                    kSecAttrKeyType to kSecAttrKeyTypeECSECPrimeRandom,
                    kSecAttrKeyClass to kSecAttrKeyClassPublic,
                    kSecAttrKeySizeInBits to 256,
                )
                val peerPublicKey = SecKeyCreateWithData(
                    peerPublicKeyEncoded.toNSData(),
                    pubAttrs.asCFDictionary(),
                    error.ptr,
                ) ?: run {
                    LogCat.e("Failed to reconstruct peer ECDH public key: ${error.value}")
                    return@memScoped null
                }

                val sharedSecret = SecKeyCopyKeyExchangeResult(
                    privateKey,
                    peerPublicKey,
                    kSecKeyAlgorithmECDHKeyExchangeStandard,
                    null,
                    error.ptr,
                ) ?: run {
                    LogCat.e("ECDH key exchange failed: ${error.value}")
                    return@memScoped null
                }

                val sharedBytes = sharedSecret.toByteArray()
                val hash = sha256(sharedBytes)
                Base64.encode(hash)
            }
        } catch (e: Exception) {
            LogCat.e("ECDH key computation failed: ${e.message}")
            null
        }
    }

    actual fun generateEd25519KeyPair(): Pair<ByteArray, ByteArray> {
        memScoped {
            val error = alloc<CFErrorRefVar>()
            val attributes = mapOf(
                kSecAttrKeyType to kSecAttrKeyTypeEd25519,
                kSecAttrKeySizeInBits to 256,
            )
            val privateKey = SecKeyCreateRandomKey(attributes.asCFDictionary(), error.ptr)
                ?: throw RuntimeException("Failed to generate Ed25519 key pair: ${error.value}")
            val publicKey = SecKeyCopyPublicKey(privateKey)

            val privData = SecKeyCopyExternalRepresentation(privateKey, null)
                ?: throw RuntimeException("Failed to export Ed25519 private key")
            val pubData = SecKeyCopyExternalRepresentation(publicKey, null)
                ?: throw RuntimeException("Failed to export Ed25519 public key")

            return Pair(privData.toByteArray(), pubData.toByteArray())
        }
    }

    actual fun signEd25519(rawPrivateKey: ByteArray, data: ByteArray): ByteArray {
        memScoped {
            val error = alloc<CFErrorRefVar>()
            val attrs = mapOf(
                kSecAttrKeyType to kSecAttrKeyTypeEd25519,
                kSecAttrKeyClass to kSecAttrKeyClassPrivate,
                kSecAttrKeySizeInBits to 256,
            )
            val privateKey = SecKeyCreateWithData(
                rawPrivateKey.toNSData(),
                attrs.asCFDictionary(),
                error.ptr,
            ) ?: throw RuntimeException("Failed to reconstruct Ed25519 private key: ${error.value}")

            val signature = SecKeyCreateSignature(
                privateKey,
                kSecKeyAlgorithmEdDSASignatureMessageCurve25519SHA512,
                data.toNSData(),
                error.ptr,
            ) ?: throw RuntimeException("Ed25519 signing failed: ${error.value}")

            return signature.toByteArray()
        }
    }

    actual fun verifyEd25519(
        rawPublicKey: ByteArray,
        data: ByteArray,
        signature: ByteArray,
    ): Boolean {
        return try {
            memScoped {
                val error = alloc<CFErrorRefVar>()
                val attrs = mapOf(
                    kSecAttrKeyType to kSecAttrKeyTypeEd25519,
                    kSecAttrKeyClass to kSecAttrKeyClassPublic,
                    kSecAttrKeySizeInBits to 256,
                )
                val publicKey = SecKeyCreateWithData(
                    rawPublicKey.toNSData(),
                    attrs.asCFDictionary(),
                    error.ptr,
                ) ?: return@memScoped false

                SecKeyVerifySignature(
                    publicKey,
                    kSecKeyAlgorithmEdDSASignatureMessageCurve25519SHA512,
                    data.toNSData(),
                    signature.toNSData(),
                    error.ptr,
                )
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun sha256(input: ByteArray): ByteArray {
        val h = intArrayOf(
            0x6a09e667, 0xbb67ae85.toInt(), 0x3c6ef372, 0xa54ff53a.toInt(),
            0x510e527f, 0x9b05688c.toInt(), 0x1f83d9ab, 0x5be0cd19,
        )
        val k = intArrayOf(
            0x428a2f98, 0x71374491, 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(),
            0x3956c25b, 0x59f111f1, 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
            0xd807aa98.toInt(), 0x12835b01, 0x243185be, 0x550c7dc3,
            0x72be5d74, 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
            0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6, 0x240ca1cc,
            0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(),
            0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
            0x650a7354, 0x766a0abb, 0x81c2c92e.toInt(), 0x92722c85.toInt(),
            0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(),
            0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
            0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814.toInt(), 0x8cc70208.toInt(),
            0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt(),
        )
        val bitLen = input.size.toLong() * 8
        val paddedLen = ((input.size + 8) / 64 + 1) * 64
        val padded = ByteArray(paddedLen)
        System.arraycopy(input, 0, padded, 0, input.size)
        padded[input.size] = 0x80.toByte()
        for (i in 0 until 8) {
            padded[paddedLen - 1 - i] = (bitLen shr (i * 8)).toByte()
        }
        val w = IntArray(64)
        for (block in padded.indices step 64) {
            for (i in 0 until 16) {
                w[i] = ((padded[block + i * 4].toInt() and 0xFF) shl 24) or
                    ((padded[block + i * 4 + 1].toInt() and 0xFF) shl 16) or
                    ((padded[block + i * 4 + 2].toInt() and 0xFF) shl 8) or
                    (padded[block + i * 4 + 3].toInt() and 0xFF)
            }
            for (i in 16 until 64) {
                val s0 = rotateRight(w[i - 15], 7) xor rotateRight(w[i - 15], 18) xor (w[i - 15] ushr 3)
                val s1 = rotateRight(w[i - 2], 17) xor rotateRight(w[i - 2], 19) xor (w[i - 2] ushr 10)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }
            var a = h[0]; var b = h[1]; var c = h[2]; var d = h[3]
            var e = h[4]; var f = h[5]; var g = h[6]; var hh = h[7]
            for (i in 0 until 64) {
                val s1 = rotateRight(e, 6) xor rotateRight(e, 11) xor rotateRight(e, 25)
                val ch = (e and f) xor (e.inv() and g)
                val t1 = hh + s1 + ch + k[i] + w[i]
                val s0 = rotateRight(a, 2) xor rotateRight(a, 13) xor rotateRight(a, 22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val t2 = s0 + maj
                hh = g; g = f; f = e; e = d + t1
                d = c; c = b; b = a; a = t1 + t2
            }
            h[0] += a; h[1] += b; h[2] += c; h[3] += d
            h[4] += e; h[5] += f; h[6] += g; h[7] += hh
        }
        val result = ByteArray(32)
        for (i in 0 until 8) {
            result[i * 4] = (h[i] ushr 24).toByte()
            result[i * 4 + 1] = (h[i] ushr 16).toByte()
            result[i * 4 + 2] = (h[i] ushr 8).toByte()
            result[i * 4 + 3] = h[i].toByte()
        }
        return result
    }

    private fun rotateRight(x: Int, n: Int): Int = (x ushr n) or (x shl (32 - n))
}

private fun ByteArray.toNSData(): NSData = NSData.dataWithBytes(this, this.size.toULong())

private fun NSData.toByteArray(): ByteArray = ByteArray(this.length.toInt()).also { buf ->
    this.getBytes(buf)
}
