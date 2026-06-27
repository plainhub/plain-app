package com.ismartcoding.plain.chat

import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.chat.peer.PeerStatusManager
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.HMessageUpdatedEvent

/**
 * Sole write-side entry point for chat rows. Every public mutator persists to
 * the database via [ChatDbHelper], triggers transport via [ChatSender] when
 * relevant, and mirrors the change into [ChatCacher] so the per-target latest
 * chat stays consistent. Reads happen directly off the cacher or via
 * [getChatItem]. UI / Web / notification paths compose these calls instead
 * of touching `ChatDbHelper` / `ChatSender` directly.
 */
object ChatManager {

    /** Recompute the latest-chat cache after any chat row mutation. */
    suspend fun refreshLatestChats() = withIO {
        ChatCacher.load()
    }

    suspend fun getChatItem(id: String): DChat? = ChatDbHelper.getChatItem(id)

    /**
     * Resolve a search-style query (e.g. `ids:1,2,3`, `channel:xxx`,
     * `peer:xxx`) into the set of chat item ids it matches.
     */
    suspend fun getIdsAsync(query: String): Set<String> = ChatDbHelper.getIdsAsync(query)

    /** Flip a chat item's send status (e.g. `pending` before a resend). */
    suspend fun updateStatus(item: DChat, status: String) {
        ChatDbHelper.updateChatItemStatus(item, status)
    }

    /**
     * Persist a new chat row from `me` to [target], trigger link-preview
     * fetch for text content, and refresh the latest-chat cache. The caller
     * owns transport — call [sendMessage] afterwards when [target] is remote.
     */
    suspend fun createChatItem(target: ChatTarget, content: DMessageContent): DChat = withIO {
        val item = ChatDbHelper.insertChatItem(
            message = content,
            fromId = "me",
            toId = if (target.type == ChatTargetType.PEER) target.toId else "",
            channelId = if (target.type == ChatTargetType.CHANNEL) target.toId else "",
            isRemote = !target.isLocal(),
        )
        if (item.content.type == DMessageType.TEXT.value) {
            sendEvent(FetchLinkPreviewsEvent(item))
        }
        refreshLatestChats()
        item
    }

    /** Dispatch [item] to its peer / channel via the transport layer. No-op for local. */
    suspend fun sendMessage(item: DChat, target: ChatTarget, onlinePeerIds: Set<String>) = withIO {
        ChatSender.send(item, target, onlinePeerIds)
    }

    /**
     * Re-dispatch an existing [item] (typically after the user retried a
     * failed send) and emit a `HMessageUpdatedEvent` so the chat page can
     * refresh the row.
     */
    suspend fun resendMessage(item: DChat) = withIO {
        ChatSender.send(item, item.target(), PeerStatusManager.onlinePeers())
        sendEvent(HMessageUpdatedEvent(item.id))
    }

    /** Retry delivery to a specific subset of channel members. */
    suspend fun sendToChannelMembers(item: DChat, channel: DChatChannel, peerIds: List<String>) = withIO {
        ChatSender.sendToChannelMembers(item, channel, peerIds)
    }

    /** Persist a fresh FILES / IMAGES row without sending. Caller drives transport. */
    suspend fun insertFilesImmediate(target: ChatTarget, files: List<DMessageFile>, isImageVideo: Boolean): DChat = withIO {
        val content = if (isImageVideo) {
            DMessageContent(DMessageType.IMAGES.value, DMessageImages(files))
        } else {
            DMessageContent(DMessageType.FILES.value, DMessageFiles(files))
        }
        val item = ChatDbHelper.insertChatItem(
            message = content,
            fromId = "me",
            toId = if (target.type == ChatTargetType.PEER) target.toId else "",
            channelId = if (target.type == ChatTargetType.CHANNEL) target.toId else "",
            isRemote = !target.isLocal(),
        )
        refreshLatestChats()
        item
    }

    /**
     * Replace the file list of an existing FILES / IMAGES message (e.g. after
     * downloads completed and the local `fid:` URIs are now available).
     * Local targets mark the row `sent`; remote targets mark it `pending`
     * and re-dispatch via [ChatSender]. Returns the updated row, or null when
     * the message no longer exists.
     */
    suspend fun updateFilesMessage(
        messageId: String,
        files: List<DMessageFile>,
        isImageVideo: Boolean,
        target: ChatTarget,
        onlinePeerIds: Set<String>,
    ): DChat? = withIO {
        val item = ChatDbHelper.getChatItem(messageId) ?: return@withIO null
        ChatDbHelper.updateChatItemFilesContent(item, files)
        if (target.isLocal()) {
            ChatDbHelper.updateChatItemStatus(item, "sent")
        } else {
            ChatDbHelper.updateChatItemStatus(item, "pending")
            ChatSender.send(item, target, onlinePeerIds)
        }
        item
    }

    /** Remove a single chat item. */
    suspend fun deleteOne(id: String) {
        ChatDbHelper.deleteAsync(id)
    }

    /** Remove a set of chat items in one DAO round-trip. */
    suspend fun deleteByIds(ids: Set<String>) {
        ChatDbHelper.deleteByIdsAsync(ids)
    }

    /** Remove every chat row tied to [target] (peer or channel). */
    suspend fun clearAllMessages(target: ChatTarget) = withIO {
        if (target.type == ChatTargetType.CHANNEL) {
            ChatDbHelper.deleteAllChannelChatsAsync(target.toId)
        } else {
            ChatDbHelper.deleteAllChatsAsync(target.toId)
        }
    }

    private fun DChat.target(): ChatTarget = when {
        channelId.isNotEmpty() -> ChatTarget(channelId, ChatTargetType.CHANNEL)
        toId.isEmpty() || toId == "local" -> ChatTarget("local", ChatTargetType.PEER)
        else -> ChatTarget(toId, ChatTargetType.PEER)
    }
}