package com.ismartcoding.plain.chat

import android.content.Context
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.channel.ChannelSystemMessageSender
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.events.ChannelUpdatedEvent
import com.ismartcoding.plain.helpers.TimeHelper

/**
 * Centralizes chat-channel mutations. Shared by ChannelViewModel and
 * ChatChannelGraphQL so both call sites exercise the same code path.
 *
 * All functions are `suspend` and do not switch threads — callers (ViewModel
 * coroutines or GraphQL resolvers) are responsible for dispatch. Lookups
 * and preconditions throw [Exception] with a specific message; let the
 * caller (UI / GraphQL) decide how to surface it.
 */
object ChannelManager {

    suspend fun createChannel(name: String): DChatChannel {
        val channel = DChatChannel()
        channel.name = name.trim()
        channel.owner = "me"
        channel.key = CryptoHelper.generateChaCha20Key()
        channel.version = 1
        channel.members = listOf(ChannelMember(id = TempData.clientId))

        AppDatabase.instance.chatChannelDao().insert(channel)
        ChatCacheManager.loadKeyCacheAsync()
        sendEvent(ChannelUpdatedEvent())
        return channel
    }

    /** Throws if the channel is missing. */
    suspend fun renameChannel(channelId: String, newName: String): DChatChannel {
        val channel = AppDatabase.instance.chatChannelDao().getById(channelId)
            ?: throw Exception("Channel not found")
        channel.name = newName.trim()
        channel.version++
        channel.updatedAt = TimeHelper.now()
        AppDatabase.instance.chatChannelDao().update(channel)
        if (channel.owner == "me") {
            ChannelSystemMessageSender.broadcastUpdate(channel)
        }
        sendEvent(ChannelUpdatedEvent())
        return channel
    }

    suspend fun deleteChannel(context: Context, channelId: String) {
        val channel = AppDatabase.instance.chatChannelDao().getById(channelId)
            ?: throw Exception("Channel not found")
        if (channel.owner == "me") {
            ChannelSystemMessageSender.broadcastKick(channel)
        }
        ChatDbHelper.deleteAllChatsAsync(context, channelId)
        AppDatabase.instance.chatChannelDao().delete(channelId)
        ChatCacheManager.loadKeyCacheAsync()
        sendEvent(ChannelUpdatedEvent())
    }

    /** Owners must delete instead. */
    suspend fun leaveChannel(channelId: String) {
        val channel = AppDatabase.instance.chatChannelDao().getById(channelId)
            ?: throw Exception("Channel not found")
        if (channel.owner == "me") throw Exception("Owner cannot leave; delete the channel instead")

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

    suspend fun addMember(channelId: String, peerId: String): DChatChannel {
        val channel = AppDatabase.instance.chatChannelDao().getById(channelId)
            ?: throw Exception("Channel not found")
        if (channel.owner != "me") throw Exception("Only owner can add members")
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
        return channel
    }

    suspend fun resendInvite(channelId: String, peerId: String) {
        val channel = AppDatabase.instance.chatChannelDao().getById(channelId)
            ?: throw Exception("Channel not found")
        if (channel.owner != "me") throw Exception("Only owner can resend invites")
        val member = channel.findMember(peerId) ?: throw Exception("Not a member")
        if (!member.isPending()) throw Exception("Member is not pending")
        val peer = AppDatabase.instance.peerDao().getById(peerId)
            ?: throw Exception("Peer not found")
        ChannelSystemMessageSender.sendInvite(channel, peer)
    }

    suspend fun removeMember(channelId: String, peerId: String): DChatChannel {
        val channel = AppDatabase.instance.chatChannelDao().getById(channelId)
            ?: throw Exception("Channel not found")
        if (channel.owner != "me") throw Exception("Only owner can remove members")
        if (!channel.hasMember(peerId)) throw Exception("Not a member")

        channel.members = channel.members.filter { it.id != peerId }
        channel.version++
        channel.updatedAt = TimeHelper.now()
        AppDatabase.instance.chatChannelDao().update(channel)

        val peer = AppDatabase.instance.peerDao().getById(peerId)
        if (peer != null) {
            ChannelSystemMessageSender.sendKick(channel.id, peer, channel.key)
        }
        ChannelSystemMessageSender.broadcastUpdate(channel)
        sendEvent(ChannelUpdatedEvent())
        return channel
    }

    suspend fun acceptInvite(channelId: String) {
        val channel = AppDatabase.instance.chatChannelDao().getById(channelId)
            ?: throw Exception("Channel not found")
        val ownerPeer = AppDatabase.instance.peerDao().getById(channel.owner)
            ?: throw Exception("Owner peer not found")
        ChannelSystemMessageSender.sendInviteAccept(channel.id, ownerPeer)
    }

    suspend fun declineInvite(context: Context, channelId: String) {
        val channel = AppDatabase.instance.chatChannelDao().getById(channelId)
            ?: throw Exception("Channel not found")
        val ownerPeer = AppDatabase.instance.peerDao().getById(channel.owner)
        if (ownerPeer != null) {
            ChannelSystemMessageSender.sendInviteDecline(channel.id, ownerPeer)
        }
        ChatDbHelper.deleteAllChatsAsync(context, channelId)
        AppDatabase.instance.chatChannelDao().delete(channelId)
        ChatCacheManager.loadKeyCacheAsync()
        sendEvent(ChannelUpdatedEvent())
    }
}
