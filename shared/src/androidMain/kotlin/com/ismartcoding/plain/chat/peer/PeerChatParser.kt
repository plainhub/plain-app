package com.ismartcoding.plain.chat.peer

import android.util.Base64
import com.ismartcoding.plain.lib.helpers.CryptoHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import io.ktor.http.HttpStatusCode
import kotlin.math.abs

data class PeerChatParseResult(
    val code: HttpStatusCode,
    val content: String? = null,
    val signature: String = "",
    val timestamp: Long = 0L,
)

object PeerChatParser {
    // Maximum allowed time difference for timestamp validation (5 minutes)
    const val MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000L

    suspend fun decrypt(
        token: ByteArray, clientId: String,
        publicKey: ByteArray, raw: ByteArray
    ): PeerChatParseResult {
        var decryptedStr = ""
        val decryptedBytes = CryptoHelper.chaCha20Decrypt(token, raw)
        if (decryptedBytes != null) {
            decryptedStr = decryptedBytes.decodeToString()
        }
        if (decryptedStr.isEmpty()) {
            return PeerChatParseResult(HttpStatusCode.Unauthorized)
        }

        // Extract timestamp, signature and GraphQL JSON from decrypted string
        // Format: "signature|timestamp|GraphQL_JSON"
        var timestamp = 0L
        var signature = ""
        var requestStr = ""

        if (decryptedStr.contains("|")) {
            val parts = decryptedStr.split("|", limit = 3)
            signature = parts[0]
            timestamp = parts[1].toLongOrNull() ?: 0L
            requestStr = parts[2]
        }

        LogCat.d("[Request] GraphQL: $requestStr")

        val currentTime = System.currentTimeMillis()
        // Verify timestamp is within acceptable range, replay attacks are not allowed
        if (abs(currentTime - timestamp) > MAX_TIMESTAMP_DIFF_MS) {
            LogCat.e("Message timestamp is too old or in the future: $timestamp - rejected")
            return PeerChatParseResult(HttpStatusCode.BadRequest)
        }

        val signatureBytes = Base64.decode(signature, Base64.NO_WRAP)
        val messageBytes = "$timestamp$requestStr".toByteArray()
        val isValid = CryptoHelper.verifySignatureWithRawEd25519PublicKey(
            publicKey,
            messageBytes,
            signatureBytes
        )
        if (!isValid) {
            LogCat.e("Invalid signature from peer $clientId")
            return PeerChatParseResult(HttpStatusCode.Unauthorized)
        }

        return PeerChatParseResult(HttpStatusCode.OK, requestStr, signature, timestamp)
    }
}