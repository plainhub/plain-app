package com.ismartcoding.plain.chat

import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.chat.channel.ChannelChatSender
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.PeerChatSender
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.discover.NearbyDiscoverManager

/**
 * Transport-only dispatcher for outbound chat messages. Higher-level business
 * flow (creating the chat row, refreshing the latest-chat cache, broadcasting
 * `HMessageUpdatedEvent`) lives in [ChatManager] — callers go there for
 * `createChatItem` / `resendMessage` / etc. and only land here for the raw
 * transport step.
 */
object ChatSender {
    suspend fun send(
        item: DChat,
        target: ChatTarget,
        onlinePeerIds: Set<String>,
    ) = withIO {
        if (target.isLocal()) {
            return@withIO
        }

        when (target.type) {
            ChatTargetType.PEER -> {
                val peer = AppDatabase.instance.peerDao().getById(target.toId) ?: return@withIO
                sendToPeer(item, peer)
            }

            ChatTargetType.CHANNEL -> {
                val channel = AppDatabase.instance.chatChannelDao().getById(target.toId) ?: return@withIO
                sendToChannel(item, channel, onlinePeerIds)
            }
        }
    }

    fun triggerPeerRediscovery(peerId: String) {
        val key = PeerCacher.getKeyBytes(peerId)
        if (key != null) {
            NearbyDiscoverManager.discoverSpecificDevice(peerId, key)
        }
    }

    suspend fun sendToPeer(item: DChat, peer: DPeer) = withIO {
        val error = PeerChatSender.send(peer, item.content)
        if (error != null) {
            triggerPeerRediscovery(peer.id)
        }
        ChatDbHelper.updateChatItemStatus(item, peer, error)
    }

    suspend fun sendToChannel(item: DChat, channel: DChatChannel, onlinePeerIds: Set<String> = emptySet()) = withIO {
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

    suspend fun sendToChannelMembers(item: DChat, channel: DChatChannel, peerIds: List<String>) = withIO {
        val newResults = ChannelChatSender.sendToRecipients(channel, peerIds, item.content)
        val existing = item.parseStatusData()?.results ?: emptyList()
        val retriedIds = peerIds.toSet()
        val merged = existing.filter { it.peerId !in retriedIds } + newResults.results
        val mergedStatusData = DMessageStatusData(merged)

        ChatDbHelper.updateChannelChatItemStatus(item, mergedStatusData)
    }
}