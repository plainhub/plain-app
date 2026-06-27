package com.ismartcoding.plain.preferences

import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import com.ismartcoding.plain.lib.helpers.CryptoHelper
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.data.DSignatureKeyPair

// The SignatureKeyPreference object is defined in shared/commonMain.
// This file contains Android-specific operations that require CryptoHelper and Base64.

suspend fun SignatureKeyPreference.ensureKeyPairAsync(
    preferences: Preferences,
) {
    val keyPairJson = get(preferences)
    if (keyPairJson.isEmpty()) {
        val tinkKeyPair = CryptoHelper.generateEd25519KeyPair()

        val rawPublicKey = CryptoHelper.extractRawEd25519PublicKey(tinkKeyPair.publicKey)
            ?: throw Exception("Failed to extract raw Ed25519 public key")

        val rawPrivateKey = extractRawEd25519PrivateKey(tinkKeyPair.privateKeyBytes)
            ?: throw Exception("Failed to extract raw Ed25519 private key")

        val privateKeyBase64 = CryptoHelper.encodeToBase64(rawPrivateKey)
        val publicKeyBase64 = CryptoHelper.encodeToBase64(rawPublicKey)

        val signatureKeyPair = DSignatureKeyPair(
            privateKey = privateKeyBase64,
            publicKey = publicKeyBase64
        )

        putAsync(JsonHelper.jsonEncode(signatureKeyPair))
    }
}

private fun extractRawEd25519PrivateKey(privateKeyBytes: ByteArray): ByteArray? {
    return try {
        val jsonString = String(privateKeyBytes, Charsets.UTF_8)
        val jsonObject = org.json.JSONObject(jsonString)
        val keyArray = jsonObject.getJSONArray("key")
        val firstKey = keyArray.getJSONObject(0)
        val keyData = firstKey.getJSONObject("keyData")
        val keyValueBase64 = keyData.getString("value")
        val keyValueBytes = Base64.decode(keyValueBase64, Base64.NO_WRAP)
        if (keyValueBytes.size >= 34 && keyValueBytes[0].toInt() == 0x12 && keyValueBytes[1].toInt() == 0x20) {
            keyValueBytes.copyOfRange(2, 34)
        } else null
    } catch (ex: Exception) {
        LogCat.e("Failed to extract raw Ed25519 private key: ${ex.message}")
        null
    }
}

suspend fun SignatureKeyPreference.getKeyPairAsync(): DSignatureKeyPair {
    return JsonHelper.jsonDecode<DSignatureKeyPair>(getAsync())
}

suspend fun SignatureKeyPreference.getPublicKeyBytesAsync(): ByteArray {
    val keyPair = getKeyPairAsync()
    return Base64.decode(keyPair.publicKey, Base64.NO_WRAP)
}
