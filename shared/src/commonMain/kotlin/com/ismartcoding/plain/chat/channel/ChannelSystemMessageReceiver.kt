package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.events.ChannelInviteReceivedEvent
import com.ismartcoding.plain.events.ChannelInviteCanceledEvent
import com.ismartcoding.plain.lib.sendEvent
import kotlinx.serialization.json.*

object ChannelSystemMessageReceiver {
    suspend fun applyCommitted(result: JsonObject) {
        if (result.getValue("changed").jsonPrimitive.boolean) {
            PeerCacher.load()
            ChannelCacher.load()
            com.ismartcoding.plain.chat.ChatManager.refreshLatestChats()
        }
        result["invite"]?.takeUnless { it is JsonNull }?.jsonObject?.let { invite ->
            val channelId = invite.getValue("channelId").jsonPrimitive.content
            val ownerId = invite.getValue("ownerPeerId").jsonPrimitive.content
            val current = RustChannelStore.getById(channelId)
            if (current != null && current.ownerId == ownerId && current.status == com.ismartcoding.plain.enums.ChatChannelStatus.JOINED &&
                current.members.any { it.peerId == TempData.clientId && it.status == com.ismartcoding.plain.enums.ChannelMemberStatus.PENDING }) {
                sendEvent(ChannelInviteReceivedEvent(channelId, current.name, ownerId,
                    invite.getValue("ownerPeerName").jsonPrimitive.content))
            }
        }
        result["cancel"]?.takeUnless { it is JsonNull }?.jsonObject?.let { cancel ->
            val channelId = cancel.getValue("channelId").jsonPrimitive.content
            val current = RustChannelStore.getById(channelId)
            val pending = current != null && current.status == com.ismartcoding.plain.enums.ChatChannelStatus.JOINED &&
                current.members.any { it.peerId == TempData.clientId && it.status == com.ismartcoding.plain.enums.ChannelMemberStatus.PENDING }
            if (!pending) sendEvent(ChannelInviteCanceledEvent(channelId,
                cancel.getValue("ownerPeerId").jsonPrimitive.content))
        }
    }
}
