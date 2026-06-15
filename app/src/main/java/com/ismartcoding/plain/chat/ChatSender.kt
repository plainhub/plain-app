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
            toId = if (target.type == ChatTargetType.PEER) target.toId else "",
            channelId = if (target.type == ChatTargetType.CHANNEL) target.toId else "",
            isRemote = !target.isLocal(),
        )
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
        if (target.isLocal()) {
            return
        }

        when (target.type) {
            ChatTargetType.PEER -> {
                val peer = AppDatabase.instance.peerDao().getById(target.toId) ?: return
                sendToPeer(item, peer)
            }

            ChatTargetType.CHANNEL -> {
                val channel = AppDatabase.instance.chatChannelDao().getById(target.toId) ?: return
                sendToChannel(item, channel, onlinePeerIds)
            }
        }
    }

    suspend fun resend(item: DChat) {
        send(item, item.target(), currentOnlinePeers())
        sendEvent(HMessageUpdatedEvent(item.id))
    }

    private fun currentOnlinePeers(): Set<String> {
        return com.ismartcoding.plain.chat.peer.PeerStatusManager.onlinePeers()
    }

    private fun DChat.target(): ChatTarget = when {
        channelId.isNotEmpty() -> ChatTarget(channelId, ChatTargetType.CHANNEL)
        toId.isEmpty() || toId == "local" -> ChatTarget("local", ChatTargetType.PEER)
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
        when (val result = ChannelChatSender.send(channel, item.content)) {
            is ChannelChatSender.Result.Status -> {
                ChatDbHelper.updateChannelChatItemStatus(item, result.data)
            }

            ChannelChatSender.Result.NoLeader -> {
                // No joined member is reachable right now — trigger rediscovery for everyone
                // so the next send / heartbeat can re-elect a leader.
                channel.getRecipientIds().forEach { triggerPeerRediscovery(it) }
                ChatDbHelper.updateChannelChatItemStatus(item, null)
            }

            is ChannelChatSender.Result.LeaderPeerMissing -> {
                // We have a leader id from election but their peer record is gone locally
                // (DB row was deleted / not yet cached). Refresh that peer first, then try
                // the rest of the members.
                triggerPeerRediscovery(result.leaderId)
                channel.getRecipientIds()
                    .filter { it != result.leaderId }
                    .forEach { triggerPeerRediscovery(it) }
                ChatDbHelper.updateChannelChatItemStatus(item, null)
            }
        }
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