package com.ismartcoding.plain.discover

import android.util.Base64
import com.ismartcoding.plain.lib.helpers.CryptoHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.data.DPairingResponse
import kotlin.math.abs

object PairingSecurity {
    // Maximum allowed time difference for timestamp validation (5 minutes)
    private const val MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000L

    fun verify(request: DPairingRequest): Boolean {
        return try {
            val signatureData = request.toSignatureData()
            val signatureBytes = Base64.decode(request.signature, Base64.NO_WRAP)
            val rawPublicKey = Base64.decode(request.signaturePublicKey, Base64.NO_WRAP)
            CryptoHelper.verifySignatureWithRawEd25519PublicKey(rawPublicKey, signatureData.toByteArray(), signatureBytes)
        } catch (e: Exception) {
            LogCat.e("Failed to verify pairing request signature: ${e.message}")
            false
        }
    }

    fun verify(response: DPairingResponse): Boolean {
        return try {
            val signatureData = response.toSignatureData()
            val signatureBytes = Base64.decode(response.signature, Base64.NO_WRAP)
            val rawPublicKey = Base64.decode(response.signaturePublicKey, Base64.NO_WRAP)
            CryptoHelper.verifySignatureWithRawEd25519PublicKey(rawPublicKey, signatureData.toByteArray(), signatureBytes)
        } catch (e: Exception) {
            LogCat.e("Failed to verify pairing response signature: ${e.message}")
            false
        }
    }

    fun validateTimestamp(timestamp: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        return abs(currentTime - timestamp) <= MAX_TIMESTAMP_DIFF_MS
    }
}
