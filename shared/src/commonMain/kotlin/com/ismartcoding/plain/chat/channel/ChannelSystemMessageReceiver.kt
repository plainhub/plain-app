package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.events.ChannelInviteReceivedEvent
import com.ismartcoding.plain.events.ChannelInviteCanceledEvent
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.sendEvent
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

object ChannelSystemMessageReceiver {
    suspend fun applyCommitted(result: JsonObject) {
        val event = JsonHelper.jsonDecodeFromElement<ChannelRuntimeEvent>(result)
        if (event.changed) {
            PeerCacher.load()
            ChannelCacher.load()
            com.ismartcoding.plain.chat.ChatManager.refreshLatestChats()
        }
        event.invite?.let { invite ->
            val state = RustChannelRuntime.call(ChannelCommand.Invitation(invite.channelId, invite.ownerPeerId))
            state.getValue("channel").takeUnless { it is JsonNull }?.let { value ->
                val channel = RustChannelStore.decode(value)
                sendEvent(ChannelInviteReceivedEvent(channel.id, channel.name, invite.ownerPeerId, invite.ownerPeerName))
            }
        }
        event.cancel?.let { cancel ->
            val state = RustChannelRuntime.call(ChannelCommand.Invitation(cancel.channelId, null))
            if (state.getValue("channel") is JsonNull) {
                sendEvent(ChannelInviteCanceledEvent(cancel.channelId, cancel.ownerPeerId))
            }
        }
    }
}
