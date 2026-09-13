package com.ismartcoding.plain.chat

import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.enums.ChatStatus
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.httpserver.models.toModel
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.buildPickedFilePlaceholders
import com.ismartcoding.plain.platform.importPickedFiles
import com.ismartcoding.plain.platform.textMessageContent

/**
 * Deliver share-intent payloads to chat targets without going through
 * ChatPage. Shared files are imported into the content-addressable store
 * once, then a message is created and sent per target.
 */
object ShareSendHelper {

    /**
     * Send files ([uris]) or [text] to every [target]. [caption] (text shared
     * alongside files) is delivered as its own message after the files.
     * Returns false when there is nothing sendable.
     */
    suspend fun sendAsync(targets: List<ChatTarget>, uris: List<String>, text: String?, caption: String?): Boolean = withIO {
        if (uris.isEmpty()) {
            if (text.isNullOrBlank()) return@withIO false
            targets.forEach { sendText(it, text) }
            return@withIO true
        }
        val placeholders = buildPickedFilePlaceholders(uris.toSet(), normalizeExtension = false)
        if (placeholders.isEmpty()) return@withIO false
        val isImageVideo = placeholders.all { it.first.fileName.isImageFast() || it.first.fileName.isVideoFast() }

        // Placeholder message per target so remote UIs see the files immediately.
        val messageIds = targets.map { target ->
            val item = ChatManager.insertFilesImmediate(target, placeholders.map { it.first }, isImageVideo)
            sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(item.toModel()))))
            ChatViewModel.onMessagesCreated(target, listOf(item), scroll = true)
            item.id
        }
        val finalItems = importPickedFiles(placeholders)
        targets.forEachIndexed { index, target ->
            val updated = ChatManager.updateFilesMessage(messageIds[index], finalItems, target, PeerCacher.getOnlinePeerIds())
            if (updated != null) ChatViewModel.onMessageUpdated(updated.id)
            if (!caption.isNullOrBlank()) sendText(target, caption)
        }
        true
    }

    /**
     * Send an already-built message content (e.g. a file already in the app
     * store) to every target. Used by in-app forwarding.
     */
    suspend fun sendContentAsync(targets: List<ChatTarget>, content: DMessageContent) {
        targets.forEach { sendContent(it, content) }
    }

    private suspend fun sendText(target: ChatTarget, text: String) {
        sendContent(target, textMessageContent(text))
    }

    private suspend fun sendContent(target: ChatTarget, content: DMessageContent) = withIO {
        val item = ChatManager.createChatItem(target, content)
        if (!target.isLocal()) {
            ChatManager.sendMessage(item, target, PeerCacher.getOnlinePeerIds())
        }
        sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(item.toModel()))))
        ChatViewModel.onMessagesCreated(target, listOf(item))
        item.status == ChatStatus.SENT
    }
}
