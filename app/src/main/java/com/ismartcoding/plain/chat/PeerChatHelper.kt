package com.ismartcoding.plain.chat

import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.toJSONString

object PeerChatHelper {
    // Maximum allowed time difference for timestamp validation (5 minutes)
    const val MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000L

    /**
     * Sends [content] to [peer].
     * Returns `null` on success, or a human-readable error string on failure.
     */
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
}