package com.ismartcoding.plain.chat

import com.ismartcoding.lib.helpers.JsonHelper.jsonEncode
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.helpers.PhoneHelper
import com.ismartcoding.plain.helpers.SignatureHelper
import com.ismartcoding.plain.preferences.DeviceNamePreference

/**
 * Encapsulates the logic for sending channel system messages to peers.
 *
 * All public methods are `suspend` and should be called from a coroutine on [Dispatchers.IO].
 */
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
     * Includes [MemberPeerInfo] for all current members so the invitee can
     * create peer records for members it doesn't already have locally.
     */
    suspend fun sendInvite(channel: DChatChannel, peer: DPeer): Boolean {
        val payload = jsonEncode(
            ChannelSystemMessages.ChannelInvite(
                channelId = channel.id,
                channelName = channel.name,
                owner = TempData.clientId,
                key = channel.key,
                members = channel.members,
                memberPeers = buildMemberPeers(channel),
                version = channel.version,
            )
        )
        return sendToPeer(peer, ChannelSystemMessages.TYPE_INVITE, payload)
    }

    /** Send accept response to the channel owner.
     *  Includes the accepter's publicKey, name, and deviceType so the owner
     *  can create/update a peer record. */
    suspend fun sendInviteAccept(channelId: String, ownerPeer: DPeer): Boolean {
        val context = MainApp.instance
        val publicKey = SignatureHelper.getRawPublicKeyBase64Async()
        val deviceName = TempData.deviceName
        val deviceType = PhoneHelper.getDeviceType(context).value
        val payload = jsonEncode(
            ChannelSystemMessages.ChannelInviteAccept(
                channelId = channelId,
                publicKey = publicKey,
                name = deviceName,
                deviceType = deviceType,
            )
        )
        return sendToPeer(ownerPeer, ChannelSystemMessages.TYPE_INVITE_ACCEPT, payload)
    }

    /** Send decline response to the channel owner. */
    suspend fun sendInviteDecline(channelId: String, ownerPeer: DPeer): Boolean {
        val payload = jsonEncode(ChannelSystemMessages.ChannelInviteDecline(channelId))
        return sendToPeer(ownerPeer, ChannelSystemMessages.TYPE_INVITE_DECLINE, payload)
    }

    /** Broadcast a [ChannelSystemMessages.ChannelUpdate] to all members (joined + pending).
     *  Includes [MemberPeerInfo] so receivers can create peer records for new members. */
    suspend fun broadcastUpdate(channel: DChatChannel) {
        val payload = jsonEncode(
            ChannelSystemMessages.ChannelUpdate(
                channelId = channel.id,
                channelName = channel.name,
                members = channel.members,
                memberPeers = buildMemberPeers(channel),
                version = channel.version,
            )
        )
        sendToMultiplePeers(channel.memberIds(), ChannelSystemMessages.TYPE_UPDATE, payload, channel.id, channel.key)
    }

    /** Send kick notification to a single peer. */
    suspend fun sendKick(channelId: String, peer: DPeer, channelKey: String = ""): Boolean {
        val payload = jsonEncode(ChannelSystemMessages.ChannelKick(channelId))
        return sendToPeer(peer, ChannelSystemMessages.TYPE_KICK, payload, channelId, channelKey)
    }

    /** Broadcast kick to all members (used when owner deletes the channel). */
    suspend fun broadcastKick(channel: DChatChannel) {
        val payload = jsonEncode(ChannelSystemMessages.ChannelKick(channel.id))
        sendToMultiplePeers(channel.memberIds(), ChannelSystemMessages.TYPE_KICK, payload, channel.id, channel.key)
    }

    /** Send leave notification to the channel owner. */
    suspend fun sendLeave(channelId: String, ownerPeer: DPeer, channelKey: String = ""): Boolean {
        val payload = jsonEncode(ChannelSystemMessages.ChannelLeave(channelId))
        return sendToPeer(ownerPeer, ChannelSystemMessages.TYPE_LEAVE, payload, channelId, channelKey)
    }

    // ── Private helpers ────────────────────────────────────────────

    private suspend fun sendToPeer(peer: DPeer, type: String, payload: String, channelId: String = "", channelKey: String = ""): Boolean {
        return try {
            val response = PeerGraphQLClient.sendChannelSystemMessage(
                peer = peer,
                clientId = TempData.clientId,
                type = type,
                payload = payload,
                channelId = channelId,
                channelKey = channelKey,
            )
            if (response != null && response.isSuccess) {
                LogCat.d("Channel system message [$type] sent to ${peer.id}")
                true
            } else {
                val errors = response?.errors?.joinToString { it.message } ?: "no response"
                LogCat.e("Failed to send [$type] to ${peer.id}: $errors")
                false
            }
        } catch (e: Exception) {
            LogCat.e("Error sending [$type] to ${peer.id}: ${e.message}")
            false
        }
    }

    private suspend fun sendToMultiplePeers(peerIds: List<String>, type: String, payload: String, channelId: String = "", channelKey: String = "") {
        val peerDao = AppDatabase.instance.peerDao()
        for (peerId in peerIds) {
            val peer = peerDao.getById(peerId) ?: continue
            sendToPeer(peer, type, payload, channelId, channelKey)
        }
    }
}
