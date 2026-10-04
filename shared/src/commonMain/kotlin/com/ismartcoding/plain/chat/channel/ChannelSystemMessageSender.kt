package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.chat.peer.GraphQLError
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.chat.peer.transport.PeerTransportRouter
import com.ismartcoding.plain.chat.peer.transport.SignedRequest
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.enums.ChannelSystemMessageType
import com.ismartcoding.plain.helpers.Base64Lenient
import kotlinx.serialization.json.*

object ChannelSystemMessageSender {
    suspend fun sendInvite(channel: DChatChannel, peer: DPeer): GraphQLResponse = sendSingle(channel, ChannelSystemMessageType.INVITE, peer.id)
    suspend fun sendInviteAccept(channelId: String, ownerPeer: DPeer): GraphQLResponse = sendSingle(channel(channelId), ChannelSystemMessageType.INVITE_ACCEPT, ownerPeer.id)
    suspend fun sendInviteDecline(channelId: String, ownerPeer: DPeer): GraphQLResponse = sendSingle(channel(channelId), ChannelSystemMessageType.INVITE_DECLINE, ownerPeer.id)
    suspend fun sendKick(channel: DChatChannel, peer: DPeer): GraphQLResponse = sendSingle(channel, ChannelSystemMessageType.KICK, peer.id)
    suspend fun sendLeave(channelId: String, ownerPeer: DPeer): GraphQLResponse = sendSingle(channel(channelId), ChannelSystemMessageType.LEAVE, ownerPeer.id)
    suspend fun broadcastUpdate(channel: DChatChannel) { sendPrepared(RustChannelOutgoingStore.prepare(channel, ChannelSystemMessageType.UPDATE)) }
    suspend fun broadcastKick(channel: DChatChannel) { sendPrepared(RustChannelOutgoingStore.prepare(channel, ChannelSystemMessageType.KICK)) }

    private suspend fun channel(id: String) = requireNotNull(RustChannelStore.getById(id)) { "Channel not found" }
    private suspend fun sendSingle(channel: DChatChannel, type: ChannelSystemMessageType, peerId: String): GraphQLResponse {
        val prepared = RustChannelOutgoingStore.prepare(channel, type, peerId)
        return send(RustChannelOutgoingStore.wire(prepared), prepared.getValue("targets").jsonArray.single().jsonObject)
    }
    private suspend fun sendPrepared(prepared: JsonObject) {
        for (target in prepared.getValue("targets").jsonArray) send(RustChannelOutgoingStore.wire(prepared), target.jsonObject)
    }
    private suspend fun send(body: String, target: JsonObject): GraphQLResponse {
        val response = PeerTransportRouter.send(
            RustPeerStore.decode(target.getValue("peer")),
            SignedRequest(body, target.getValue("channelId").jsonPrimitive.content),
            Base64Lenient.decode(target.getValue("key").jsonPrimitive.content),
        )
        if (!response.isSuccess) return response
        val acknowledged = response.data?.let { Json.parseToJsonElement(it).jsonObject["channelSystemMessage"]?.jsonPrimitive?.booleanOrNull } == true
        return if (acknowledged) response else response.copy(errors = listOf(GraphQLError("Channel message rejected")))
    }
}
