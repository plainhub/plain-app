package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getOwner
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.helpers.coIO
import com.ismartcoding.plain.platform.generateChaCha20Key
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.web.models.dchatChannelToModel

object ChannelManager {

    init {
        startChannelBroadcaster()
    }

    private fun startChannelBroadcaster() {
        coIO {
            ChannelCacher.channels
                .collect { channels ->
                    if (channels.isEmpty()) return@collect
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

    suspend fun createChannel(name: String): DChatChannel {
        return withIO {
            val channel = DChatChannel()
            channel.name = name.trim()
            channel.owner = TempData.clientId
            channel.key = generateChaCha20Key()
            channel.version = 1
            channel.members = listOf(ChannelMember(id = TempData.clientId))

            AppDatabase.instance.chatChannelDao().insert(channel)
            ChannelCacher.load()
            channel
        }
    }

    suspend fun renameChannel(channelId: String, newName: String): DChatChannel {
        return withIO {
            val channel = ChannelCacher.mutateChannel(channelId) { ch ->
                ch.name = newName.trim()
                ch.version++
                ch.updatedAt = TimeHelper.now()
            } ?: throw Exception("Channel not found")
            if (channel.isOwnedByMe()) {
                ChannelSystemMessageSender.broadcastUpdate(channel)
            }
            channel
        }
    }

    suspend fun deleteChannel(channelId: String) {
        withIO {
            val channel = ensureChannel(channelId)
            if (channel.isOwnedByMe()) {
                ChannelSystemMessageSender.broadcastKick(channel)
            }
            ChatDbHelper.deleteAllChannelChatsAsync(channelId)
            AppDatabase.instance.chatChannelDao().delete(channelId)
            ChannelCacher.removeChannel(channelId)
        }
    }

    suspend fun leaveChannel(channelId: String) {
        withIO {
            val existing = ensureChannel(channelId)
            if (existing.isOwnedByMe()) throw Exception("Owner cannot leave; delete the channel instead")

            val ownerPeer = AppDatabase.instance.peerDao().getById(existing.owner)
            val channel = ChannelCacher.mutateChannel(channelId) { ch ->
                ch.status = DChatChannel.STATUS_LEFT
                ch.members = ch.members.filter { it.id != TempData.clientId }
            } ?: throw Exception("Channel not found")
            if (ownerPeer != null) {
                ChannelSystemMessageSender.sendLeave(channel.id, ownerPeer)
            }
        }
    }

    suspend fun inviteMember(channelId: String, peerId: String): DChatChannel {
        return withIO {
            val peer = AppDatabase.instance.peerDao().getById(peerId)
            val channel = ChannelCacher.mutateChannel(channelId) { ch ->
                if (!ch.isOwnedByMe()) throw Exception("Only owner can add members")
                if (ch.hasMember(peerId)) throw Exception("Already a member")
                ch.members += ChannelMember(
                    id = peerId,
                    status = ChannelMember.STATUS_PENDING,
                )
                ch.version++
                ch.updatedAt = TimeHelper.now()
            } ?: throw Exception("Channel not found")

            if (peer != null) {
                ChannelSystemMessageSender.sendInvite(channel, peer)
            }
            channel
        }
    }

    suspend fun resendInvite(channelId: String, peerId: String) {
        withIO {
            val channel = ensureChannel(channelId)
            if (!channel.isOwnedByMe()) throw Exception("Only owner can resend invites")
            val member = channel.findMember(peerId) ?: throw Exception("Not a member")
            if (!member.isPending()) throw Exception("Member is not pending")
            val peer = PeerCacher.getPeer(peerId)
                ?: throw Exception("Peer not found")
            ChannelSystemMessageSender.sendInvite(channel, peer)
        }
    }

    suspend fun kickMember(channelId: String, peerId: String): DChatChannel {
        return withIO {
            val peer = AppDatabase.instance.peerDao().getById(peerId)
            val channel = ChannelCacher.mutateChannel(channelId) { ch ->
                if (!ch.isOwnedByMe()) throw Exception("Only owner can remove members")
                if (!ch.hasMember(peerId)) throw Exception("Not a member")
                ch.members = ch.members.filter { it.id != peerId }
                ch.version++
                ch.updatedAt = TimeHelper.now()
            } ?: throw Exception("Channel not found")

            if (peer != null) {
                ChannelSystemMessageSender.sendKick(channel, peer)
            }
            ChannelSystemMessageSender.broadcastUpdate(channel)
            channel
        }
    }

    suspend fun acceptInvite(channelId: String): GraphQLResponse {
        return withIO {
            val channel = ensureChannel(channelId)
            val member = channel.findMember(TempData.clientId)
                ?: throw Exception("Invite no longer valid")
            if (!member.isPending()) throw Exception("Invite no longer valid")
            val ownerPeer = ensureOwner(channel)
            ChannelSystemMessageSender.sendInviteAccept(channel.id, ownerPeer)
        }
    }

    suspend fun declineInvite(channelId: String) {
        withIO {
            val channel = ensureChannel(channelId)
            val ownerPeer = ensureOwner(channel)
            ChannelSystemMessageSender.sendInviteDecline(channel.id, ownerPeer)
            ChatDbHelper.deleteAllChannelChatsAsync(channelId)
            AppDatabase.instance.chatChannelDao().delete(channelId)
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
    JsonHelper.jsonEncode(channels.map { dchatChannelToModel(it) })
