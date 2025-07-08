package com.ismartcoding.plain.chat

import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.toJSONString
import com.ismartcoding.plain.helpers.FileHelper

object PeerChatHelper {
    // Maximum allowed time difference for timestamp validation (5 minutes)
    const val MAX_TIMESTAMP_DIFF_MS = 5 * 60 * 1000L

    suspend fun sendToPeerAsync(peer: DPeer, content: DMessageContent): Boolean {
        try {
            val modifiedContent = when (content.type) {
                DMessageType.FILES.value -> {
                    val files = content.value as DMessageFiles
                    val modifiedFiles = files.items.map { file ->
                        val fileId = FileHelper.getFileId(file.uri)
                        file.copy(uri = "fid:$fileId") // Update URI to use file ID
                    }
                    DMessageContent(content.type, DMessageFiles(modifiedFiles))
                }

                DMessageType.IMAGES.value -> {
                    val images = content.value as DMessageImages
                    val modifiedImages = images.items.map { image ->
                        val fileId = FileHelper.getFileId(image.uri)
                        image.copy(uri = "fid:$fileId") // Update URI to use file ID
                    }
                    DMessageContent(content.type, DMessageImages(modifiedImages))
                }

                else -> content
            }

            val response = PeerGraphQLClient.createChatItem(
                peer = peer,
                clientId = TempData.clientId,
                content = modifiedContent.toJSONString()
            )

            if (response != null && response.errors.isNullOrEmpty()) {
                LogCat.d("Message sent successfully to peer ${peer.id}: ${response.data}")
                return true
            } else {
                val errorMessages = response?.errors?.joinToString(", ") { it.message } ?: "Unknown error"
                LogCat.e("Failed to send message to peer ${peer.id}: $errorMessages")
                return false
            }

        } catch (e: Exception) {
            LogCat.e("Error sending message to peer ${peer.id}: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}