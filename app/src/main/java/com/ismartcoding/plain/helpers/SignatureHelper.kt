package com.ismartcoding.plain.helpers

import android.util.Base64
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.preferences.SignatureKeyPreference

object SignatureHelper {
    
    /**
     * Sign data using the device's Ed25519 private key
     */
    suspend fun signDataAsync(data: ByteArray): ByteArray? {
        val keyPair = SignatureKeyPreference.getKeyPairAsync(MainApp.instance) ?: return null
        val rawPrivateKey = Base64.decode(keyPair.privateKey, Base64.NO_WRAP)
        
        return CryptoHelper.signDataWithRawEd25519PrivateKey(rawPrivateKey, data)
    }
    
    /**
     * Sign text using the device's Ed25519 private key
     */
    suspend fun signTextAsync(text: String): String? {
        val signature = signDataAsync(text.toByteArray())
        return signature?.let { CryptoHelper.encodeToBase64(it) }
    }
    
    /**
     * Verify signature using the device's Ed25519 public key
     */
    suspend fun verifySignatureAsync(data: ByteArray, signature: ByteArray): Boolean {
        val keyPair = SignatureKeyPreference.getKeyPairAsync(MainApp.instance) ?: return false
        val rawPublicKey = Base64.decode(keyPair.publicKey, Base64.NO_WRAP)
        
        return CryptoHelper.verifySignatureWithRawEd25519PublicKey(rawPublicKey, data, signature)
    }
    
    /**
     * Get the device's public key bytes for sharing with other devices
     */
    suspend fun getPublicKeyBytesAsync(): ByteArray? {
        return SignatureKeyPreference.getPublicKeyBytesAsync(MainApp.instance)
    }
    
    /**
     * Get the device's public key as Base64 string for transmission
     */
    suspend fun getPublicKeyBase64Async(): String? {
        val publicKeyBytes = getPublicKeyBytesAsync()
        return publicKeyBytes?.let { CryptoHelper.encodeToBase64(it) }
    }
    
    /**
     * Get the device's raw Ed25519 public key (32 bytes) as Base64 string for peer communication
     * Raw keys are now stored directly in preferences
     */
    suspend fun getRawPublicKeyBase64Async(): String? {
        val keyPair = SignatureKeyPreference.getKeyPairAsync(MainApp.instance) ?: return null
        LogCat.d("getRawPublicKeyBase64Async: keyPair.publicKey = '${keyPair.publicKey}'")
        LogCat.d("getRawPublicKeyBase64Async: keyPair.publicKey length = ${keyPair.publicKey.length}")
        
        // 验证解码后的长度
        try {
            val decoded = Base64.decode(keyPair.publicKey, Base64.NO_WRAP)
            LogCat.d("getRawPublicKeyBase64Async: decoded length = ${decoded.size}")
        } catch (e: Exception) {
            LogCat.e("getRawPublicKeyBase64Async: Failed to decode public key: ${e.message}")
        }
        
        return keyPair.publicKey
    }
    
    /**
     * Verify Ed25519 signature from another device using raw public key
     */
    fun verifyExternalSignatureWithRawKey(
        data: ByteArray, 
        signature: ByteArray, 
        rawPublicKeyBase64: String
    ): Boolean {
        return try {
            val rawPublicKey = Base64.decode(rawPublicKeyBase64, Base64.NO_WRAP)
            CryptoHelper.verifySignatureWithRawEd25519PublicKey(rawPublicKey, data, signature)
        } catch (ex: Exception) {
            LogCat.e("Failed to verify external Ed25519 signature with raw key: ${ex.message}")
            false
        }
    }
    
} 