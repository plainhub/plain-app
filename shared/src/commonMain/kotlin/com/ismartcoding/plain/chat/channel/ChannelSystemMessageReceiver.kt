package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.events.HChannelInviteReceivedEvent
import com.ismartcoding.plain.events.HChannelInviteCanceledEvent
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
                sendEvent(HChannelInviteReceivedEvent(channel.id, channel.name, invite.ownerPeerId, invite.ownerPeerName))
            }
        }
        event.cancel?.let { cancel ->
            val state = RustChannelRuntime.call(ChannelCommand.Invitation(cancel.channelId, null))
            if (state.getValue("channel") is JsonNull) {
                sendEvent(HChannelInviteCanceledEvent(cancel.channelId, cancel.ownerPeerId))
            }
        }
    }
}
