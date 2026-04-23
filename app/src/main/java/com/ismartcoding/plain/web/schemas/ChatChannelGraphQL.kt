package com.ismartcoding.plain.web.schemas

import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CryptoHelper
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.ChannelSystemMessageSender
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.events.ChannelUpdatedEvent
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addChatChannelSchema() {
    mutation("createChatChannel") {
        resolver { name: String ->
            val channel = DChatChannel()
            channel.name = name.trim()
            channel.owner = "me"
            channel.key = CryptoHelper.generateChaCha20Key()
            channel.version = 1
            channel.members = listOf(ChannelMember(id = TempData.clientId))
            AppDatabase.instance.chatChannelDao().insert(channel)
            ChatCacheManager.loadKeyCacheAsync()
            sendEvent(ChannelUpdatedEvent())
            channel.toModel()
        }
    }
    mutation("updateChatChannel") {
        resolver { id: ID, name: String ->
            val channel = AppDatabase.instance.chatChannelDao().getById(id.value)
                ?: throw Exception("Channel not found")
            channel.name = name.trim()
            channel.version++
            channel.updatedAt = TimeHelper.now()
            AppDatabase.instance.chatChannelDao().update(channel)
            if (channel.owner == "me") {
                ChannelSystemMessageSender.broadcastUpdate(channel)
            }
            sendEvent(ChannelUpdatedEvent())
            channel.toModel()
        }
    }
    mutation("deleteChatChannel") {
        resolver { id: ID ->
            val channel = AppDatabase.instance.chatChannelDao().getById(id.value)
            if (channel != null) {
                if (channel.owner == "me") {
                    ChannelSystemMessageSender.broadcastKick(channel)
                }
                ChatDbHelper.deleteAllChatsAsync(MainApp.instance, channel.id)
                AppDatabase.instance.chatChannelDao().delete(channel.id)
                ChatCacheManager.loadKeyCacheAsync()
                sendEvent(ChannelUpdatedEvent())
            }
            true
        }
    }
    mutation("leaveChatChannel") {
        resolver { id: ID ->
            val channel = AppDatabase.instance.chatChannelDao().getById(id.value)
            if (channel != null && channel.owner != "me") {
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
            true
        }
    }
    mutation("addChatChannelMember") {
        resolver { id: ID, peerId: String ->
            val channel = AppDatabase.instance.chatChannelDao().getById(id.value)
                ?: throw Exception("Channel not found")
            if (channel.owner != "me") throw Exception("Only owner can add members")
            if (channel.hasMember(peerId)) throw Exception("Already a member")
            val peer = AppDatabase.instance.peerDao().getById(peerId)
            channel.members = channel.members + ChannelMember(
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
            channel.toModel()
        }
    }
    mutation("removeChatChannelMember") {
        resolver { id: ID, peerId: String ->
            val channel = AppDatabase.instance.chatChannelDao().getById(id.value)
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
            channel.toModel()
        }
    }
    mutation("acceptChatChannelInvite") {
        resolver { id: ID ->
            val channel = AppDatabase.instance.chatChannelDao().getById(id.value)
                ?: throw Exception("Channel not found")
            val ownerPeer = AppDatabase.instance.peerDao().getById(channel.owner)
                ?: throw Exception("Owner peer not found")
            ChannelSystemMessageSender.sendInviteAccept(channel.id, ownerPeer)
            true
        }
    }
    mutation("declineChatChannelInvite") {
        resolver { id: ID ->
            val channel = AppDatabase.instance.chatChannelDao().getById(id.value)
            if (channel != null) {
                val ownerPeer = AppDatabase.instance.peerDao().getById(channel.owner)
                if (ownerPeer != null) {
                    ChannelSystemMessageSender.sendInviteDecline(channel.id, ownerPeer)
                }
                ChatDbHelper.deleteAllChatsAsync(MainApp.instance, channel.id)
                AppDatabase.instance.chatChannelDao().delete(channel.id)
                ChatCacheManager.loadKeyCacheAsync()
                sendEvent(ChannelUpdatedEvent())
            }
            true
        }
    }
}
