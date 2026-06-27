package com.ismartcoding.plain.chat.peer

import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.toPeerMessageContent


object PeerChatSender {
    suspend fun send(peer: DPeer, content: DMessageContent): String? = withIO {
        try {
            val response = PeerGraphQLClient.createChatItem(
                peer = peer,
                clientId = TempData.clientId,
                content = content.toPeerMessageContent()
            )

            if (response != null && response.errors.isNullOrEmpty()) {
                LogCat.d("Message sent successfully to peer ${peer.id}: ${response.data}")
                null
            } else {
                val errorMessage = if (response == null) {
                    "No response received (host unreachable or connection refused)"
                } else {
                    response.errors?.joinToString(", ") { it.message } ?: "Empty error list in response"
                }
                LogCat.e("Failed to send message to peer ${peer.id}: $errorMessage")
                errorMessage
            }

        } catch (e: Exception) {
            val errorMessage = e.message ?: "Unknown error"
            LogCat.e("Error sending message to peer ${peer.id}: $e")
            errorMessage
        }
    }
}