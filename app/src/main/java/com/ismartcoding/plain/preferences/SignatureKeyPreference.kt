package com.ismartcoding.plain.preferences

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.data.DSignatureKeyPair

object SignatureKeyPreference : BasePreference<String>() {
    override val default = ""
    override val key = stringPreferencesKey("signature_key_pair")

    suspend fun ensureKeyPairAsync(
        context: Context,
        preferences: Preferences,
    ) {
        val keyPairJson = get(preferences)
        if (keyPairJson.isEmpty()) {
            val tinkKeyPair = CryptoHelper.generateEd25519KeyPair()

            val rawPublicKey = CryptoHelper.extractRawEd25519PublicKey(tinkKeyPair.publicKey)
                ?: throw Exception("Failed to extract raw Ed25519 public key")

            val rawPrivateKey = extractRawEd25519PrivateKey(tinkKeyPair.privateKeyBytes)
                ?: throw Exception("Failed to extract raw Ed25519 private key")

            // Store raw keys as Base64
            val privateKeyBase64 = CryptoHelper.encodeToBase64(rawPrivateKey)
            val publicKeyBase64 = CryptoHelper.encodeToBase64(rawPublicKey)

            val signatureKeyPair = DSignatureKeyPair(
                privateKey = privateKeyBase64,
                publicKey = publicKeyBase64
            )

            val json = JsonHelper.jsonEncode(signatureKeyPair)
            putAsync(context, json)
        }
    }

    /**
     * Extract raw Ed25519 private key (32 bytes) from Tink KeysetHandle
     */
    private fun extractRawEd25519PrivateKey(privateKeyBytes: ByteArray): ByteArray? {
        return try {
            val jsonString = String(privateKeyBytes, Charsets.UTF_8)
            val jsonObject = org.json.JSONObject(jsonString)

            val keyArray = jsonObject.getJSONArray("key")
            val firstKey = keyArray.getJSONObject(0)
            val keyData = firstKey.getJSONObject("keyData")
            val keyValueBase64 = keyData.getString("value")

            val keyValueBytes = Base64.decode(keyValueBase64, Base64.NO_WRAP)

            // Ed25519PrivateKey protobuf format: 0x12 0x20 + 32_bytes_key
            if (keyValueBytes.size >= 34 && keyValueBytes[0].toInt() == 0x12 && keyValueBytes[1].toInt() == 0x20) {
                return keyValueBytes.copyOfRange(2, 34) // Extract 32 bytes
            }

            null
        } catch (ex: Exception) {
            LogCat.e("Failed to extract raw Ed25519 private key: ${ex.message}")
            null
        }
    }

    suspend fun getKeyPairAsync(context: Context): DSignatureKeyPair? {
        val keyPairJson = getAsync(context)
        if (keyPairJson.isEmpty()) {
            return null
        }

        return JsonHelper.jsonDecode<DSignatureKeyPair>(keyPairJson)
    }

    suspend fun getPublicKeyBytesAsync(context: Context): ByteArray? {
        val keyPair = getKeyPairAsync(context) ?: return null
        return Base64.decode(keyPair.publicKey, Base64.NO_WRAP)
    }
}