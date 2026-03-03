package com.ismartcoding.plain.helpers

import android.util.Base64
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.preferences.SignatureKeyPreference

object SignatureHelper {

    /**
     * Sign data using the device's Ed25519 private key
     */
    suspend fun signDataAsync(data: ByteArray): ByteArray {
        val keyPair = SignatureKeyPreference.getKeyPairAsync(MainApp.instance)
        val rawPrivateKey = Base64.decode(keyPair.privateKey, Base64.NO_WRAP)

        return CryptoHelper.signDataWithRawEd25519PrivateKey(rawPrivateKey, data)
    }

    /**
     * Sign text using the device's Ed25519 private key
     */
    suspend fun signTextAsync(text: String): String {
        val signature = signDataAsync(text.toByteArray())
        return CryptoHelper.encodeToBase64(signature)
    }

    /**
     * Get the device's raw Ed25519 public key (32 bytes) as Base64 string for peer communication
     * Raw keys are now stored directly in preferences
     */
    suspend fun getRawPublicKeyBase64Async(): String {
        val keyPair = SignatureKeyPreference.getKeyPairAsync(MainApp.instance)
        return keyPair.publicKey
    }
}