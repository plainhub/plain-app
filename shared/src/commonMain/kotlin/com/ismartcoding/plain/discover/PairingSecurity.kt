package com.ismartcoding.plain.discover

import com.ismartcoding.plain.crypto.PairingCrypto
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.abs

@OptIn(ExperimentalEncodingApi::class)
object PairingSecurity {
    private const val MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000L

    fun verify(request: DPairingRequest): Boolean {
        return try {
            val signatureData = request.toSignatureData()
            val signatureBytes = Base64.decode(request.signature)
            val rawPublicKey = Base64.decode(request.signaturePublicKey)
            PairingCrypto.verifyEd25519(rawPublicKey, signatureData.toByteArray(), signatureBytes)
        } catch (e: Exception) {
            LogCat.e("Failed to verify pairing request signature: ${e.message}")
            false
        }
    }

    fun verify(response: DPairingResponse): Boolean {
        return try {
            val signatureData = response.toSignatureData()
            val signatureBytes = Base64.decode(response.signature)
            val rawPublicKey = Base64.decode(response.signaturePublicKey)
            PairingCrypto.verifyEd25519(rawPublicKey, signatureData.toByteArray(), signatureBytes)
        } catch (e: Exception) {
            LogCat.e("Failed to verify pairing response signature: ${e.message}")
            false
        }
    }

    fun validateTimestamp(timestamp: Long): Boolean {
        val currentTime = TimeHelper.nowMillis()
        return abs(currentTime - timestamp) <= MAX_TIMESTAMP_DIFF_MS
    }
}
