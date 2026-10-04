package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.callChatStore
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.enums.ChannelSystemMessageType
import com.ismartcoding.plain.events.ChannelInviteReceivedEvent
import com.ismartcoding.plain.events.ChannelInviteCanceledEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent
import kotlinx.serialization.json.*

object ChannelSystemMessageReceiver {
    suspend fun handle(fromId: String, type: ChannelSystemMessageType, payload: String): Boolean = try {
        val result = callChatStore("receiveChannel") {
            put("actor", TempData.clientId)
            put("from_id", fromId)
            put("message_type", type.name)
            put("payload", payload)
        }.jsonObject
        if (result.getValue("changed").jsonPrimitive.boolean) {
            PeerCacher.load()
            ChannelCacher.load()
        }
        result["invite"]?.takeUnless { it is JsonNull }?.jsonObject?.let { invite ->
            sendEvent(ChannelInviteReceivedEvent(
                invite.getValue("channelId").jsonPrimitive.content,
                invite.getValue("channelName").jsonPrimitive.content,
                invite.getValue("ownerPeerId").jsonPrimitive.content,
                invite.getValue("ownerPeerName").jsonPrimitive.content,
            ))
        }
        result["cancel"]?.takeUnless { it is JsonNull }?.jsonObject?.let { cancel ->
            sendEvent(ChannelInviteCanceledEvent(
                cancel.getValue("channelId").jsonPrimitive.content,
                cancel.getValue("ownerPeerId").jsonPrimitive.content,
            ))
        }
        if (result.getValue("broadcast").jsonPrimitive.boolean) {
            ChannelSystemMessageSender.broadcastUpdate(RustChannelStore.decode(result.getValue("channel")))
        }
        result.getValue("accepted").jsonPrimitive.boolean
    } catch (error: Exception) {
        LogCat.e("Channel message rejected [${type.name}] from $fromId: ${error.message}")
        false
    }
}
