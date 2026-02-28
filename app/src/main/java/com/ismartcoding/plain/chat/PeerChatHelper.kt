package com.ismartcoding.plain.chat

import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.toJSONString

object PeerChatHelper {
    // Maximum allowed time difference for timestamp validation (5 minutes)
    const val MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000L

    suspend fun sendToPeerAsync(peer: DPeer, content: DMessageContent): Boolean {
        try {
            val response = PeerGraphQLClient.createChatItem(
                peer = peer,
                clientId = TempData.clientId,
                content = content.toPeerMessageContent()
            )

            if (response != null && response.errors.isNullOrEmpty()) {
                LogCat.d("Message sent successfully to peer ${peer.id}: ${response.data}")
                return true
            } else {
                val errorMessages = if (response == null) {
                    "No response received (host unreachable or connection refused)"
                } else {
                    response.errors?.joinToString(", ") { it.message } ?: "Empty error list in response"
                }
                LogCat.e("Failed to send message to peer ${peer.id}: $errorMessages")
                return false
            }

        } catch (e: Exception) {
            LogCat.e("Error sending message to peer ${peer.id}: ${e}")
            return false
        }
    }
}