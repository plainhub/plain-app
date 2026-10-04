package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.chat.peer.RustPeerStore

import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getOwner
import com.ismartcoding.plain.db.isOwnedByMe
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.httpserver.models.toModel

object ChannelManager {

    init {
        startChannelBroadcaster()
    }

    private fun startChannelBroadcaster() {
        coIO {
            ChannelCacher.channels
                .collect { channels ->
                    sendEvent(
                        WebSocketEvent(
                            EventType.CHANNELS_UPDATED,
                            channelsToJsonModelString(channels),
                        ),
                    )
                    ChatManager.refreshLatestChats()
                }
        }
    }

    suspend fun createChannel(name: String): DChatChannel = withIO {
        val channel = RustChannelStore.create(name)
        ChannelCacher.load()
        channel
    }

    suspend fun renameChannel(channelId: String, newName: String): DChatChannel = withIO {
        val channel = RustChannelStore.action(channelId, "rename", name = newName)
        ChannelCacher.load()
        if (channel.isOwnedByMe()) ChannelSystemMessageSender.broadcastUpdate(channel)
        channel
    }

    suspend fun deleteChannel(channelId: String) {
        withIO {
            val channel = RustChannelStore.remove(channelId) ?: throw Exception("Channel not found")
            com.ismartcoding.plain.chat.ChatManager.refreshLatestChats()
            ChannelCacher.removeChannel(channelId)
            if (channel.isOwnedByMe()) {
                ChannelSystemMessageSender.broadcastKick(channel)
            }
        }
    }

    suspend fun leaveChannel(channelId: String): Unit = withIO {
        val channel = RustChannelStore.action(channelId, "leave")
        ChannelCacher.load()
        RustPeerStore.getById(channel.ownerId)?.let { ChannelSystemMessageSender.sendLeave(channel.id, it) }
        Unit
    }

    suspend fun inviteMember(channelId: String, peerId: String): DChatChannel = withIO {
        val channel = RustChannelStore.action(channelId, "invite", peer = peerId)
        ChannelCacher.load()
        RustPeerStore.getById(peerId)?.let { ChannelSystemMessageSender.sendInvite(channel, it) }
        channel
    }

    suspend fun resendInvite(channelId: String, peerId: String): Unit = withIO {
        val channel = RustChannelStore.action(channelId, "resend", peer = peerId)
        ChannelCacher.load()
        val peer = RustPeerStore.getById(peerId) ?: throw Exception("Peer not found")
        ChannelSystemMessageSender.sendInvite(channel, peer)
        Unit
    }

    suspend fun kickMember(channelId: String, peerId: String): DChatChannel = withIO {
        val channel = RustChannelStore.action(channelId, "kick", peer = peerId)
        ChannelCacher.load()
        RustPeerStore.getById(peerId)?.let { ChannelSystemMessageSender.sendKick(channel, it) }
        ChannelSystemMessageSender.broadcastUpdate(channel)
        channel
    }

    suspend fun acceptInvite(channelId: String): GraphQLResponse = withIO {
        val channel = RustChannelStore.action(channelId, "accept")
        ChannelCacher.load()
        ChannelSystemMessageSender.sendInviteAccept(channel.id, ensureOwner(channel))
    }

    suspend fun declineInvite(channelId: String) {
        withIO {
            val channel = ensureChannel(channelId)
            val ownerPeer = ensureOwner(channel)
            ChannelSystemMessageSender.sendInviteDecline(channel.id, ownerPeer)
            RustChannelStore.remove(channelId)
            com.ismartcoding.plain.chat.ChatManager.refreshLatestChats()
            ChannelCacher.removeChannel(channelId)
        }
    }

    private fun ensureOwner(channel: DChatChannel): DPeer {
        return channel.getOwner()
            ?: throw Exception("Owner peer not found")
    }

    private suspend fun ensureChannel(channelId: String): DChatChannel {
        return ChannelCacher.getChannel(channelId)
            ?: throw Exception("Channel not found")
    }
}

private fun channelsToJsonModelString(channels: List<DChatChannel>): String =
    JsonHelper.jsonEncode(channels.map { it.toModel() })
