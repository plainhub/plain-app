package com.ismartcoding.plain.chat

import android.content.Context
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.channel.ChannelSystemMessageSender
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getOwner
import com.ismartcoding.plain.events.ChannelUpdatedEvent
import com.ismartcoding.plain.helpers.TimeHelper

object ChannelManager {

    suspend fun createChannel(name: String): DChatChannel {
        return withIO {
            val channel = DChatChannel()
            channel.name = name.trim()
            channel.owner = TempData.clientId
            channel.key = CryptoHelper.generateChaCha20Key()
            channel.version = 1
            channel.members = listOf(ChannelMember(id = TempData.clientId))

            AppDatabase.instance.chatChannelDao().insert(channel)
            ChatCacheManager.loadKeyCacheAsync()
            sendEvent(ChannelUpdatedEvent())
            channel
        }
    }

    suspend fun renameChannel(channelId: String, newName: String): DChatChannel {
        return withIO {
            val channel = ensureChannel(channelId)
            channel.name = newName.trim()
            channel.version++
            channel.updatedAt = TimeHelper.now()
            AppDatabase.instance.chatChannelDao().update(channel)
            if (channel.isOwnedByMe()) {
                ChannelSystemMessageSender.broadcastUpdate(channel)
            }
            sendEvent(ChannelUpdatedEvent())
            channel
        }
    }

    suspend fun deleteChannel(context: Context, channelId: String) {
        withIO {
            val channel = ensureChannel(channelId)
            if (channel.isOwnedByMe()) {
                ChannelSystemMessageSender.broadcastKick(channel)
            }
            ChatDbHelper.deleteAllChannelChatsAsync(context, channelId)
            AppDatabase.instance.chatChannelDao().delete(channelId)
            ChatCacheManager.loadKeyCacheAsync()
            sendEvent(ChannelUpdatedEvent())
        }
    }

    suspend fun leaveChannel(channelId: String) {
        withIO {
            val channel = ensureChannel(channelId)
            if (!channel.isOwnedByMe()) throw Exception("Owner cannot leave; delete the channel instead")

            val ownerPeer = AppDatabase.instance.peerDao().getById(channel.owner)
            if (ownerPeer != null) {
                ChannelSystemMessageSender.sendLeave(channel.id, ownerPeer, channel.key)
            }
            channel.status = DChatChannel.STATUS_LEFT
            channel.members = channel.members.filter { it.id != TempData.clientId }
            AppDatabase.instance.chatChannelDao().update(channel)
            ChatCacheManager.loadKeyCacheAsync()
            sendEvent(ChannelUpdatedEvent())
        }
    }

    suspend fun inviteMember(channelId: String, peerId: String): DChatChannel {
        return withIO {
            val channel = ensureChannel(channelId)
            if (!channel.isOwnedByMe()) throw Exception("Only owner can add members")
            if (channel.hasMember(peerId)) throw Exception("Already a member")

            val peer = AppDatabase.instance.peerDao().getById(peerId)
            channel.members += ChannelMember(
                id = peerId,
                status = ChannelMember.STATUS_PENDING,
            )
            channel.version++
            channel.updatedAt = TimeHelper.now()
            AppDatabase.instance.chatChannelDao().update(channel)

            if (peer != null) {
                ChannelSystemMessageSender.sendInvite(channel, peer)
            }
            sendEvent(ChannelUpdatedEvent())
            channel
        }
    }

    suspend fun resendInvite(channelId: String, peerId: String) {
        withIO {
            val channel = ensureChannel(channelId)
            if (!channel.isOwnedByMe()) throw Exception("Only owner can resend invites")
            val member = channel.findMember(peerId) ?: throw Exception("Not a member")
            if (!member.isPending()) throw Exception("Member is not pending")
            val peer = AppDatabase.instance.peerDao().getById(peerId)
                ?: throw Exception("Peer not found")
            ChannelSystemMessageSender.sendInvite(channel, peer)
        }
    }

    suspend fun kickMember(channelId: String, peerId: String): DChatChannel {
        return withIO {
            val channel = ensureChannel(channelId)
            if (!channel.isOwnedByMe()) throw Exception("Only owner can remove members")
            if (!channel.hasMember(peerId)) throw Exception("Not a member")

            channel.members = channel.members.filter { it.id != peerId }
            channel.version++
            channel.updatedAt = TimeHelper.now()
            AppDatabase.instance.chatChannelDao().update(channel)

            val peer = AppDatabase.instance.peerDao().getById(peerId)
            if (peer != null) {
                ChannelSystemMessageSender.sendKick(channel, peer)
            }
            ChannelSystemMessageSender.broadcastUpdate(channel)
            sendEvent(ChannelUpdatedEvent())
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

    suspend fun declineInvite(context: Context, channelId: String) {
        withIO {
            val channel = ensureChannel(channelId)
            val ownerPeer = ensureOwner(channel)
            ChannelSystemMessageSender.sendInviteDecline(channel.id, ownerPeer)
            ChatDbHelper.deleteAllChannelChatsAsync(context, channelId)
            AppDatabase.instance.chatChannelDao().delete(channelId)
            ChatCacheManager.loadKeyCacheAsync()
            sendEvent(ChannelUpdatedEvent())
        }
    }

    private fun ensureOwner(channel: DChatChannel): DPeer {
        return channel.getOwner()
            ?: throw Exception("Owner peer not found")
    }

    private suspend fun ensureChannel(channelId: String): DChatChannel {
        return AppDatabase.instance.chatChannelDao().getById(channelId)
            ?: throw Exception("Channel not found")
    }
}
