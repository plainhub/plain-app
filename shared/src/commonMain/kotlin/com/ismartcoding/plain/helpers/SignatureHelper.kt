package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.crypto.PairingCrypto
import com.ismartcoding.plain.preferences.SignatureKeyPreference
import com.ismartcoding.plain.preferences.getKeyPairAsync
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
object SignatureHelper {

    suspend fun signDataAsync(data: ByteArray): ByteArray {
        val keyPair = SignatureKeyPreference.getKeyPairAsync()
        val rawPrivateKey = Base64.decode(keyPair.privateKey)
        return PairingCrypto.signEd25519(rawPrivateKey, data)
    }

    suspend fun signTextAsync(text: String): String {
        val signature = signDataAsync(text.toByteArray())
        return Base64.encode(signature)
    }

    suspend fun getRawPublicKeyBase64Async(): String {
        return SignatureKeyPreference.getKeyPairAsync().publicKey
    }
}
