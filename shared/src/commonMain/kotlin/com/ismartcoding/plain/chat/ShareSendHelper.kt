package com.ismartcoding.plain.chat

import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageShare
import com.ismartcoding.plain.db.DSharePeerInfo
import com.ismartcoding.plain.db.DShare
import com.ismartcoding.plain.db.MessageType
import com.ismartcoding.plain.enums.ChatStatus
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.share.ShareCrypto
import com.ismartcoding.plain.features.share.ShareManager
import com.ismartcoding.plain.httpserver.models.toModel
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.platform.getDeviceIP4
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.buildPickedFilePlaceholders
import com.ismartcoding.plain.platform.importPickedFiles
import com.ismartcoding.plain.platform.listFilesInDir
import com.ismartcoding.plain.platform.statFile
import com.ismartcoding.plain.platform.textMessageContent
import kotlin.time.Instant

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

    /**
     * Create a read-only share for [dirPath] and send a folder card message
     * (MessageType.SHARE) to every target. The card links to the same
     * `/s/<id>#<token>` URL the web share page uses.
     */
    suspend fun sendFolderShareAsync(
        targets: List<ChatTarget>,
        dirPath: String,
        name: String,
        expiresAt: Instant?,
    ): Boolean = withIO {
        if (dirPath.isBlank()) return@withIO false
        val share = ShareManager.createShare(
            name = name,
            realPaths = listOf(dirPath),
            urlToken = ShareCrypto.newUrlToken(),
            readOnly = true,
            expiresAt = expiresAt,
        )
        sendContentAsync(targets, buildShareContent(share, listOf(dirPath)))
        true
    }

    /**
     * Build the SHARE message content for an existing [share]. The card
     * carries the guest secret plus the sender endpoint (id/ip/port) instead
     * of a baked URL so forwarded copies stay openable.
     */
    suspend fun buildShareContent(share: DShare, paths: List<String>): DMessageContent = withIO {
        // Single directory root: count its shallow entries; file roots: one
        // item per path. Directory sizes are not walked recursively.
        val isSingleDir = paths.size == 1 && (statFile(paths[0])?.isDir == true)
        val itemCount: Int
        val totalSize: Long
        if (isSingleDir) {
            val entries = listFilesInDir(paths[0], showHidden = false, sortBy = FileSortBy.NAME_ASC)
            itemCount = entries.size
            totalSize = entries.sumOf { if (it.isDir) 0L else it.size }
        } else {
            itemCount = paths.size
            totalSize = paths.sumOf { statFile(it)?.size ?: 0L }
        }
        DMessageContent(
            MessageType.SHARE,
            DMessageShare(
                shareId = share.id,
                urlToken = ShareCrypto.deriveSharedTokenEncoded(share.id),
                peerInfo = DSharePeerInfo(
                    id = TempData.clientId,
                    ip = getDeviceIP4(),
                    port = TempData.httpsPort.value,
                ),
                name = share.name,
                itemCount = itemCount,
                totalSize = totalSize,
                expiresAt = share.expiresAt,
            ),
        )
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
