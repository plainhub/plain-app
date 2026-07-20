package com.ismartcoding.plain.chat

import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.chat.channel.ChannelChatSender
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.PeerChatSender
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.discover.LANDiscoverManager
import com.ismartcoding.plain.lib.logcat.LogCat

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
        LogCat.d("triggerPeerRediscovery: $peerId")
        val key = PeerCacher.getKeyBytes(peerId)
        if (key != null) {
            LANDiscoverManager.discoverSpecificDevice(peerId, key)
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
                channel.getRecipientIds().forEach { triggerPeerRediscovery(it) }
                ChatDbHelper.updateChannelChatItemStatus(item, null)
            }

            is ChannelChatSender.Result.LeaderPeerMissing -> {
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
