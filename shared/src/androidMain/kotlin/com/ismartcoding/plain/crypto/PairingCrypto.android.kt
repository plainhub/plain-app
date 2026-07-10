package com.ismartcoding.plain.crypto

import com.ismartcoding.plain.lib.helpers.CryptoHelper
import org.json.JSONObject
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec

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

            CryptoHelper.computeECDHSharedKey(privateKey, peerPublicKey.encoded)
        } catch (e: Exception) {
            null
        }
    }

    actual fun generateEd25519KeyPair(): Pair<ByteArray, ByteArray> {
        val tinkKeyPair = CryptoHelper.generateEd25519KeyPair()
        val rawPublicKey = CryptoHelper.extractRawEd25519PublicKey(tinkKeyPair.publicKey)
            ?: throw RuntimeException("Failed to extract raw Ed25519 public key")
        val rawPrivateKey = extractRawEd25519PrivateKey(tinkKeyPair.privateKeyBytes)
        return Pair(rawPrivateKey, rawPublicKey)
    }

    actual fun signEd25519(rawPrivateKey: ByteArray, data: ByteArray): ByteArray {
        return CryptoHelper.signDataWithRawEd25519PrivateKey(rawPrivateKey, data)
    }

    actual fun verifyEd25519(
        rawPublicKey: ByteArray,
        data: ByteArray,
        signature: ByteArray,
    ): Boolean {
        return CryptoHelper.verifySignatureWithRawEd25519PublicKey(rawPublicKey, data, signature)
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

    private fun extractRawEd25519PrivateKey(privateKeysetJsonBytes: ByteArray): ByteArray {
        val jsonString = String(privateKeysetJsonBytes, Charsets.UTF_8)
        val jsonObject = JSONObject(jsonString)
        val keyArray = jsonObject.getJSONArray("key")
        val firstKey = keyArray.getJSONObject(0)
        val keyData = firstKey.getJSONObject("keyData")
        val keyValueBase64 = keyData.getString("value")
        val keyValueBytes = android.util.Base64.decode(keyValueBase64, android.util.Base64.NO_WRAP)

        for (i in 0 until keyValueBytes.size - 33) {
            if (keyValueBytes[i].toInt() == 0x1a && keyValueBytes[i + 1].toInt() == 0x20) {
                return keyValueBytes.copyOfRange(i + 2, i + 34)
            }
        }
        throw RuntimeException("Failed to extract raw Ed25519 private key from keyset")
    }
}
