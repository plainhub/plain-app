package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.chat.peer.PeerGraphQLClient
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getPeersAsync
import com.ismartcoding.plain.enums.ChannelSystemMessageAction
import com.ismartcoding.plain.enums.ChannelSystemMessageType
import com.ismartcoding.plain.helpers.JsonHelper.jsonEncode
import com.ismartcoding.plain.helpers.SignatureHelper
import com.ismartcoding.plain.platform.getDeviceType
import com.ismartcoding.plain.chat.channel.ChannelSystemMessages.MemberPeerInfo
import com.ismartcoding.plain.chat.channel.ChannelSystemMessages.ChannelInvite
import com.ismartcoding.plain.chat.channel.ChannelSystemMessages.ChannelInviteAccept
import com.ismartcoding.plain.chat.channel.ChannelSystemMessages.ChannelInviteDecline
import com.ismartcoding.plain.chat.channel.ChannelSystemMessages.ChannelUpdate
import com.ismartcoding.plain.chat.channel.ChannelSystemMessages.ChannelKick
import com.ismartcoding.plain.chat.channel.ChannelSystemMessages.ChannelLeave

object ChannelSystemMessageSender {

    private suspend fun buildMemberPeers(channel: DChatChannel): List<MemberPeerInfo> {
        return channel.getPeersAsync().map { peer ->
            MemberPeerInfo(
                id = peer.id,
                name = peer.name,
                publicKey = peer.publicKey,
                deviceType = peer.deviceType,
                ip = peer.ip,
                port = peer.port,
            )
        }
    }

    suspend fun sendInvite(channel: DChatChannel, peer: DPeer): GraphQLResponse = withIO {
        val payload = jsonEncode(
            ChannelInvite(
                channelId = channel.id,
                channelName = channel.name,
                owner = TempData.clientId,
                key = channel.key,
                members = channel.members,
                memberPeers = buildMemberPeers(channel),
                version = channel.version,
                signature = SignatureHelper.signTextAsync(
                    channelMessagePayload(channel.id, channel.version, ChannelSystemMessageAction.INVITE, peer.id)
                ),
            )
        )
        sendToPeer(peer, ChannelSystemMessageType.INVITE, payload)
    }

    suspend fun sendInviteAccept(channelId: String, ownerPeer: DPeer): GraphQLResponse = withIO {
        val publicKey = SignatureHelper.getRawPublicKeyBase64Async()
        val deviceType = getDeviceType()
        val payload = jsonEncode(
            ChannelInviteAccept(
                channelId = channelId,
                publicKey = publicKey,
                name = TempData.deviceName.value,
                deviceType = deviceType,
            )
        )
        sendToPeer(ownerPeer, ChannelSystemMessageType.INVITE_ACCEPT, payload)
    }

    suspend fun sendInviteDecline(channelId: String, ownerPeer: DPeer): GraphQLResponse = withIO {
        val payload = jsonEncode(ChannelInviteDecline(channelId))
        sendToPeer(ownerPeer, ChannelSystemMessageType.INVITE_DECLINE, payload)
    }

    suspend fun broadcastUpdate(channel: DChatChannel) = withIO {
        val payload = jsonEncode(
            ChannelUpdate(
                channelId = channel.id,
                channelName = channel.name,
                members = channel.members,
                memberPeers = buildMemberPeers(channel),
                version = channel.version,
                signature = SignatureHelper.signTextAsync(
                    channelMessagePayload(channel.id, channel.version, ChannelSystemMessageAction.UPDATE, "")
                ),
            )
        )
        sendToMultiplePeers(channel.memberIdsNotMe(TempData.clientId), ChannelSystemMessageType.UPDATE, payload, channel.id)
    }

    suspend fun sendKick(channel: DChatChannel, peer: DPeer): GraphQLResponse = withIO {
        val payload = jsonEncode(
            ChannelKick(
                channelId = channel.id,
                version = channel.version,
                signature = SignatureHelper.signTextAsync(
                    channelMessagePayload(channel.id, channel.version, ChannelSystemMessageAction.KICK, peer.id)
                ),
            )
        )
        sendToPeer(peer, ChannelSystemMessageType.KICK, payload, channel.id)
    }

    suspend fun broadcastKick(channel: DChatChannel) = withIO {
        val payload = jsonEncode(
            ChannelKick(
                channelId = channel.id,
                version = channel.version,
                signature = SignatureHelper.signTextAsync(
                    channelMessagePayload(channel.id, channel.version, ChannelSystemMessageAction.KICK, "")
                ),
            )
        )
        sendToMultiplePeers(channel.memberIdsNotMe(TempData.clientId), ChannelSystemMessageType.KICK, payload, channel.id)
    }

    suspend fun sendLeave(channelId: String, ownerPeer: DPeer): GraphQLResponse = withIO {
        val payload = jsonEncode(ChannelLeave(channelId))
        sendToPeer(ownerPeer, ChannelSystemMessageType.LEAVE, payload, channelId)
    }

    private suspend fun sendToPeer(peer: DPeer, type: ChannelSystemMessageType, payload: String, channelId: String = ""): GraphQLResponse = withIO {
        PeerGraphQLClient.sendChannelSystemMessage(
            peer = peer,
            type = type,
            payload = payload,
            channelId = channelId,
        )
    }

    private suspend fun sendToMultiplePeers(peerIds: List<String>, type: ChannelSystemMessageType, payload: String, channelId: String = "") = withIO {
        for (peerId in peerIds) {
            val peer = PeerCacher.getPeer(peerId) ?: continue
            sendToPeer(peer, type, payload, channelId)
        }
    }
}