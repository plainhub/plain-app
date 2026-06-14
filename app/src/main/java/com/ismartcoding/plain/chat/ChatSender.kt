package com.ismartcoding.plain.chat

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.channel.ChannelChatSender
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.chat.peer.PeerChatSender
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.discover.NearbyDiscoverManager
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.HMessageUpdatedEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.web.models.toModel

object ChatSender {
    suspend fun createChatItem(target: ChatTarget, content: DMessageContent): DChat {
        val item = ChatDbHelper.insertChatItem(
            message = content,
            fromId = "me",
            toId = when (target.type) {
                ChatTargetType.CHANNEL -> ""
                ChatTargetType.PEER, ChatTargetType.LOCAL -> target.toId
            },
            channelId = if (target.type == ChatTargetType.CHANNEL) target.toId else "",
            isRemote = target.type != ChatTargetType.LOCAL,
        )

        val model = item.toModel().apply { data = getContentData() }
        sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(model))))
        if (item.content.type == DMessageType.TEXT.value) {
            sendEvent(FetchLinkPreviewsEvent(item))
        }
        return item
    }

    suspend fun send(
        item: DChat,
        target: ChatTarget,
        onlinePeerIds: Set<String>,
    ) {
        return when (target.type) {
            ChatTargetType.PEER -> {
                val peer = AppDatabase.instance.peerDao().getById(target.toId) ?: return
                sendToPeer(item, peer)
            }

            ChatTargetType.CHANNEL -> {
                val channel = AppDatabase.instance.chatChannelDao().getById(target.toId) ?: return
                sendToChannel(item, channel, onlinePeerIds)
            }

            ChatTargetType.LOCAL -> {}
        }
    }

    /**
     * Resend a previously-sent message. Caller must have already set the
     * item's status to "pending" and refreshed its own view-model state
     * so the UI shows a "sending" indicator before the network round-trip
     * completes. This function sends via the existing target route and
     * then broadcasts MESSAGE_UPDATED so the UI and WebSocket clients
     * refresh to the final delivery status.
     */
    suspend fun resend(item: DChat, onlinePeerIds: Set<String> = emptySet()) {
        send(item, item.target(), onlinePeerIds)
        sendEvent(HMessageUpdatedEvent(item.id))
    }

    private fun DChat.target(): ChatTarget = when {
        channelId.isNotEmpty() -> ChatTarget(channelId, ChatTargetType.CHANNEL)
        toId.isEmpty() || toId == "local" -> ChatTarget("local", ChatTargetType.LOCAL)
        else -> ChatTarget(toId, ChatTargetType.PEER)
    }

    fun triggerPeerRediscovery(peerId: String) {
        val key = ChatCacheManager.peerKeyCache[peerId]
        if (key != null) {
            NearbyDiscoverManager.discoverSpecificDevice(peerId, key)
        }
    }

    suspend fun sendToPeer(item: DChat, peer: DPeer) {
        val error = PeerChatSender.send(peer, item.content)
        if (error != null) {
            triggerPeerRediscovery(peer.id)
        }
        ChatDbHelper.updateChatItemStatus(item, peer, error)
    }

    suspend fun sendToChannel(item: DChat, channel: DChatChannel, onlinePeerIds: Set<String> = emptySet()) {
        val statusData = ChannelChatSender.send(channel, item.content)
        if (statusData == null) {
            val leaderId = channel.electLeader(onlinePeerIds, TempData.clientId)
            if (leaderId != null && leaderId != TempData.clientId) {
                triggerPeerRediscovery(leaderId)
            } else {
                channel.getRecipientIds().forEach { triggerPeerRediscovery(it) }
            }
        }
        ChatDbHelper.updateChannelChatItemStatus(item, statusData)
    }

    suspend fun sendToChannelMembers(item: DChat, channel: DChatChannel, peerIds: List<String>) {
        val newResults = ChannelChatSender.sendToRecipients(channel, peerIds, item.content)
        val existing = item.parseStatusData()?.results ?: emptyList()
        val retriedIds = peerIds.toSet()
        val merged = existing.filter { it.peerId !in retriedIds } + newResults.results
        val mergedStatusData = DMessageStatusData(merged)

        ChatDbHelper.updateChannelChatItemStatus(item, mergedStatusData)
    }
}