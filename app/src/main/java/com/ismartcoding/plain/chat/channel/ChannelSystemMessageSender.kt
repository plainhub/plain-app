package com.ismartcoding.plain.chat.channel

import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.api.ApiResult
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.chat.peer.PeerGraphQLClient
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getPeersAsync
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.helpers.SignatureHelper

object ChannelSystemMessageSender {

    /**
     * Build lightweight peer info for all channel members from the local peers table.
     * This is included in ChannelInvite/ChannelUpdate so receivers can create
     * peer records for members they don't already know.
     */
    private suspend fun buildMemberPeers(channel: DChatChannel): List<ChannelSystemMessages.MemberPeerInfo> {
        return channel.getPeersAsync().map { peer ->
            ChannelSystemMessages.MemberPeerInfo(
                id = peer.id,
                name = peer.name,
                publicKey = peer.publicKey,
                deviceType = peer.deviceType,
                ip = peer.ip,
                port = peer.port,
            )
        }
    }

    /**
     * Send a [ChannelSystemMessages.ChannelInvite] to a single peer.
     * The channel key is sent as-is because the PeerGraphQL transport layer
     * already encrypts the entire payload with the peer's shared key and
     * verifies the Ed25519 signature.
     *
     * Includes [ChannelSystemMessages.MemberPeerInfo] for all current members so
     * the invitee can create peer records for members it doesn't already have
     * locally, and dynamically signs `"$channelId|$version|invite|<peer id>"`
     * with the owner's private key.
     */
    suspend fun sendInvite(channel: DChatChannel, peer: DPeer): GraphQLResponse = withIO {
        val payload = jsonEncode(
            ChannelSystemMessages.ChannelInvite(
                channelId = channel.id,
                channelName = channel.name,
                owner = TempData.clientId,
                key = channel.key,
                members = channel.members,
                memberPeers = buildMemberPeers(channel),
                version = channel.version,
                signature = SignatureHelper.signTextAsync(
                    channelMessagePayload(channel.id, channel.version, ChannelSystemMessages.ACTION_INVITE, peer.id)
                ),
            )
        )
        sendToPeer(peer, ChannelSystemMessages.TYPE_INVITE, payload)
    }

    /** Send accept response to the channel owner.
     *  Includes the accepter's publicKey, name, and deviceType so the owner
     *  can create/update a peer record. */
    suspend fun sendInviteAccept(channelId: String, ownerPeer: DPeer): GraphQLResponse = withIO {
        val context = MainApp.instance
        val publicKey = SignatureHelper.getRawPublicKeyBase64Async()
        val deviceType = PhoneHelper.getDeviceType(context).value
        val payload = jsonEncode(
            ChannelSystemMessages.ChannelInviteAccept(
                channelId = channelId,
                publicKey = publicKey,
                name = TempData.deviceName.value,
                deviceType = deviceType,
            )
        )
        sendToPeer(ownerPeer, ChannelSystemMessages.TYPE_INVITE_ACCEPT, payload)
    }

    /** Send decline response to the channel owner. */
    suspend fun sendInviteDecline(channelId: String, ownerPeer: DPeer): GraphQLResponse = withIO {
        val payload = jsonEncode(ChannelSystemMessages.ChannelInviteDecline(channelId))
        sendToPeer(ownerPeer, ChannelSystemMessages.TYPE_INVITE_DECLINE, payload)
    }

    /** Broadcast a [ChannelSystemMessages.ChannelUpdate] to all members (joined + pending).
     *  Includes [ChannelSystemMessages.MemberPeerInfo] so receivers can create peer records
     *  for new members, and dynamically signs `"$channelId|$version|update|"`
     *  with the owner's private key. */
    suspend fun broadcastUpdate(channel: DChatChannel) = withIO {
        val payload = jsonEncode(
            ChannelSystemMessages.ChannelUpdate(
                channelId = channel.id,
                channelName = channel.name,
                members = channel.members,
                memberPeers = buildMemberPeers(channel),
                version = channel.version,
                signature = SignatureHelper.signTextAsync(
                    channelMessagePayload(channel.id, channel.version, ChannelSystemMessages.ACTION_UPDATE, "")
                ),
            )
        )
        sendToMultiplePeers(channel.memberIdsNotMe(TempData.clientId), ChannelSystemMessages.TYPE_UPDATE, payload, channel.id, channel.key)
    }

    /** Send kick notification to a single peer, signed with
     *  `"$channelId|$version|kick|<peer id>"` so the kicked peer can verify
     *  it came from the channel owner. */
    suspend fun sendKick(channel: DChatChannel, peer: DPeer): GraphQLResponse = withIO {
        val payload = jsonEncode(
            ChannelSystemMessages.ChannelKick(
                channelId = channel.id,
                version = channel.version,
                signature = SignatureHelper.signTextAsync(
                    channelMessagePayload(channel.id, channel.version, ChannelSystemMessages.ACTION_KICK, peer.id)
                ),
            )
        )
        sendToPeer(peer, ChannelSystemMessages.TYPE_KICK, payload, channel.id, channel.key)
    }

    /** Broadcast kick to all members (used when owner deletes the channel).
     *  Signed with `"$channelId|$version|kick|"` — target is empty for broadcast. */
    suspend fun broadcastKick(channel: DChatChannel) = withIO {
        val payload = jsonEncode(
            ChannelSystemMessages.ChannelKick(
                channelId = channel.id,
                version = channel.version,
                signature = SignatureHelper.signTextAsync(
                    channelMessagePayload(channel.id, channel.version, ChannelSystemMessages.ACTION_KICK, "")
                ),
            )
        )
        sendToMultiplePeers(channel.memberIdsNotMe(TempData.clientId), ChannelSystemMessages.TYPE_KICK, payload, channel.id, channel.key)
    }

    /** Send leave notification to the channel owner. */
    suspend fun sendLeave(channelId: String, ownerPeer: DPeer, channelKey: String = ""): GraphQLResponse = withIO {
        val payload = jsonEncode(ChannelSystemMessages.ChannelLeave(channelId))
        sendToPeer(ownerPeer, ChannelSystemMessages.TYPE_LEAVE, payload, channelId, channelKey)
    }

    private suspend fun sendToPeer(peer: DPeer, type: String, payload: String, channelId: String = "", channelKey: String = ""): GraphQLResponse = withIO {
        PeerGraphQLClient.sendChannelSystemMessage(
            peer = peer,
            clientId = TempData.clientId,
            type = type,
            payload = payload,
            channelId = channelId,
            channelKey = channelKey,
        )
    }

    private suspend fun sendToMultiplePeers(peerIds: List<String>, type: String, payload: String, channelId: String = "", channelKey: String = "") = withIO {
        val peerDao = AppDatabase.instance.peerDao()
        for (peerId in peerIds) {
            val peer = peerDao.getById(peerId) ?: continue
            sendToPeer(peer, type, payload, channelId, channelKey)
        }
    }
}