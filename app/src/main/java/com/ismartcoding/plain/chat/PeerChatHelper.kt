package com.ismartcoding.plain.chat

import android.util.Base64
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.toPeerMessageContent
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.RoutingCall
import kotlin.math.abs

data class PeerChatParseResult(val code: HttpStatusCode, val content: String? = null)

object PeerChatHelper {
    // Maximum allowed time difference for timestamp validation (5 minutes)
    const val MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000L

    suspend fun sendToPeerAsync(peer: DPeer, content: DMessageContent): String? {
        try {
            val response = PeerGraphQLClient.createChatItem(
                peer = peer,
                clientId = TempData.clientId,
                content = content.toPeerMessageContent()
            )

            if (response != null && response.errors.isNullOrEmpty()) {
                LogCat.d("Message sent successfully to peer ${peer.id}: ${response.data}")
                return null
            } else {
                val errorMessage = if (response == null) {
                    "No response received (host unreachable or connection refused)"
                } else {
                    response.errors?.joinToString(", ") { it.message } ?: "Empty error list in response"
                }
                LogCat.e("Failed to send message to peer ${peer.id}: $errorMessage")
                return errorMessage
            }

        } catch (e: Exception) {
            val errorMessage = e.message ?: "Unknown error"
            LogCat.e("Error sending message to peer ${peer.id}: $e")
            return errorMessage
        }
    }


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

        return PeerChatParseResult(HttpStatusCode.OK, requestStr)
    }
}