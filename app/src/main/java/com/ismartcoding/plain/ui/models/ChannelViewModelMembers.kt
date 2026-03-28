package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.ChannelSystemMessageSender
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.events.ChannelUpdatedEvent
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal fun ChannelViewModel.addChannelMember(channelId: String, peerId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        val channel = AppDatabase.instance.chatChannelDao().getById(channelId) ?: return@launch
        if (channel.owner != "me") return@launch
        if (channel.hasMember(peerId)) return@launch

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
    }
}

internal fun ChannelViewModel.resendInvite(channelId: String, peerId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        val channel = AppDatabase.instance.chatChannelDao().getById(channelId) ?: return@launch
        if (channel.owner != "me") return@launch
        val member = channel.findMember(peerId) ?: return@launch
        if (!member.isPending()) return@launch
        val peer = AppDatabase.instance.peerDao().getById(peerId) ?: return@launch
        ChannelSystemMessageSender.sendInvite(channel, peer)
    }
}

internal fun ChannelViewModel.removeChannelMember(channelId: String, peerId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        val channel = AppDatabase.instance.chatChannelDao().getById(channelId) ?: return@launch
        if (channel.owner != "me") return@launch
        if (!channel.hasMember(peerId)) return@launch

        channel.members = channel.members.filter { it.id != peerId }
        channel.version++
        channel.updatedAt = TimeHelper.now()
        AppDatabase.instance.chatChannelDao().update(channel)

        val peer = AppDatabase.instance.peerDao().getById(peerId)
        if (peer != null) {
            ChannelSystemMessageSender.sendKick(channelId, peer, channel.key)
        }
        ChannelSystemMessageSender.broadcastUpdate(channel)
        sendEvent(ChannelUpdatedEvent())
    }
}

internal fun ChannelViewModel.leaveChannel(context: Context, channelId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        val channel = AppDatabase.instance.chatChannelDao().getById(channelId) ?: return@launch
        if (channel.owner == "me") return@launch

        val ownerPeer = AppDatabase.instance.peerDao().getById(channel.owner)
        if (ownerPeer != null) {
            ChannelSystemMessageSender.sendLeave(channelId, ownerPeer, channel.key)
        }
        channel.status = DChatChannel.STATUS_LEFT
        channel.members = channel.members.filter { it.id != TempData.clientId }
        AppDatabase.instance.chatChannelDao().update(channel)
        ChatCacheManager.loadKeyCacheAsync()
        sendEvent(ChannelUpdatedEvent())
    }
}

internal fun ChannelViewModel.acceptChannelInvite(channelId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        val channel = AppDatabase.instance.chatChannelDao().getById(channelId) ?: return@launch
        val ownerPeer = AppDatabase.instance.peerDao().getById(channel.owner) ?: return@launch
        ChannelSystemMessageSender.sendInviteAccept(channelId, ownerPeer)
    }
}

internal fun ChannelViewModel.declineChannelInvite(context: Context, channelId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        val channel = AppDatabase.instance.chatChannelDao().getById(channelId) ?: return@launch
        val ownerPeer = AppDatabase.instance.peerDao().getById(channel.owner)
        if (ownerPeer != null) {
            ChannelSystemMessageSender.sendInviteDecline(channelId, ownerPeer)
        }
        ChatDbHelper.deleteAllChatsAsync(context, channelId)
        AppDatabase.instance.chatChannelDao().delete(channelId)
        ChatCacheManager.loadKeyCacheAsync()
        sendEvent(ChannelUpdatedEvent())
    }
}
