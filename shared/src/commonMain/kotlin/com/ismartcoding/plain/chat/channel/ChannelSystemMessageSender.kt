package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.ChannelSystemMessageType
import kotlinx.serialization.json.*

object ChannelSystemMessageSender {
    suspend fun sendInvite(channel: DChatChannel, peer: DPeer): GraphQLResponse = sendSingle(channel, ChannelSystemMessageType.INVITE, peer.id)
    suspend fun sendInviteAccept(channelId: String, ownerPeer: DPeer): GraphQLResponse = send(channelId, ChannelSystemMessageType.INVITE_ACCEPT, ownerPeer.id)
    suspend fun sendInviteDecline(channelId: String, ownerPeer: DPeer): GraphQLResponse = send(channelId, ChannelSystemMessageType.INVITE_DECLINE, ownerPeer.id)
    suspend fun sendKick(channel: DChatChannel, peer: DPeer): GraphQLResponse = sendSingle(channel, ChannelSystemMessageType.KICK, peer.id)
    suspend fun sendLeave(channelId: String, ownerPeer: DPeer): GraphQLResponse = send(channelId, ChannelSystemMessageType.LEAVE, ownerPeer.id)
    suspend fun broadcastUpdate(channel: DChatChannel) { send(channel.id, ChannelSystemMessageType.UPDATE, "") }
    suspend fun broadcastKick(channel: DChatChannel) { send(channel.id, ChannelSystemMessageType.KICK, "") }

    private suspend fun sendSingle(channel: DChatChannel, type: ChannelSystemMessageType, peerId: String): GraphQLResponse = send(channel.id, type, peerId)
    private suspend fun send(id: String, type: ChannelSystemMessageType, target: String): GraphQLResponse =
        RustChannelRuntime.response(RustChannelRuntime.call("send", id) {
            put("message_type", type.name)
            put("target", target)
        })
}
