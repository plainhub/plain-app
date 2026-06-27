package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.verifyEd25519Signature
import com.ismartcoding.plain.events.ChannelInviteCanceledEvent
import com.ismartcoding.plain.events.ChannelInviteReceivedEvent
import com.ismartcoding.plain.helpers.JsonHelper.jsonDecode
import com.ismartcoding.plain.helpers.TimeHelper

object ChannelSystemMessageReceiver {

    suspend fun handle(fromId: String, type: String, payload: String) {
        try {
            when (type) {
                ChannelSystemMessages.TYPE_INVITE -> handleInvite(fromId, jsonDecode(payload))
                ChannelSystemMessages.TYPE_INVITE_ACCEPT -> handleInviteAccept(fromId, jsonDecode(payload))
                ChannelSystemMessages.TYPE_INVITE_DECLINE -> handleInviteDecline(fromId, jsonDecode(payload))
                ChannelSystemMessages.TYPE_UPDATE -> handleUpdate(fromId, jsonDecode(payload))
                ChannelSystemMessages.TYPE_KICK -> handleKick(fromId, jsonDecode(payload))
                ChannelSystemMessages.TYPE_LEAVE -> handleLeave(fromId, jsonDecode(payload))
                else -> LogCat.e("Unknown channel system message type: $type")
            }
        } catch (e: Exception) {
            LogCat.e("Error handling channel system message [$type] from $fromId: ${e.message}")
        }
    }

    // ── Individual handlers ────────────────────────────────────────

    /**
     * Insert a `status = "channel"` peer record for a member we don't already
     * know. Returns true when a new row was inserted.
     */
    private suspend fun ensureChannelPeer(
        id: String,
        name: String,
        publicKey: String,
        deviceType: String,
        ip: String = "",
        port: Int = 0,
        logTag: String = "member",
    ): Boolean {
        if (PeerCacher.getPeer(id) != null) return false
        AppDatabase.instance.peerDao().insert(
            DPeer(
                id = id,
                name = name,
                publicKey = publicKey,
                status = "channel",
                deviceType = deviceType,
                ip = ip,
                port = port,
            ),
        )
        LogCat.d("Created channel peer record for $logTag $id")
        return true
    }

    private suspend fun handleInvite(fromId: String, msg: ChannelSystemMessages.ChannelInvite) {
        val existingChannel = ChannelCacher.getChannel(msg.channelId)
        val isReinvite = existingChannel != null &&
                (existingChannel.status == DChatChannel.STATUS_LEFT || existingChannel.status == DChatChannel.STATUS_KICKED)

        // Sanity check: the signed `fromId` (Ed25519 verified upstream) must equal the
        // owner the inviter is claiming in the payload. Otherwise an attacker who
        // forwarded us an invite payload could try to seed us with a channel whose
        // owner is a peer they control.
        if (msg.owner != fromId) {
            LogCat.e("Invite from $fromId but payload claims owner=${msg.owner} — rejected")
            return
        }

        // Cryptographic ownership check: the per-message [signature] must verify
        // against the owner's public key (sourced from memberPeers) over the
        // canonical payload `"$channelId|$version|invite|<our peer id>"`. The
        // target binding prevents an invite meant for peer A from being replayed
        // to peer B.
        val ownerMemberInfo = msg.memberPeers.find { it.id == msg.owner }
        if (ownerMemberInfo == null) {
            LogCat.e("Invite for channel ${msg.channelId} has no owner memberPeerInfo — rejected")
            return
        }
        val invitePayload = channelMessagePayload(
            channelId = msg.channelId,
            version = msg.version,
            action = ChannelSystemMessages.ACTION_INVITE,
            target = TempData.clientId,
        )
        if (!verifyEd25519Signature(ownerMemberInfo.publicKey, invitePayload, msg.signature)) {
            LogCat.e("Invite signature failed for channel ${msg.channelId} from $fromId — rejected")
            return
        }

        // Avoid duplicate processing for channels that are already active
        if (existingChannel != null && !isReinvite) {
            LogCat.d("Channel ${msg.channelId} already exists locally, ignoring invite")
            return
        }

        // Verify the sender is a known paired peer
        val peer = PeerCacher.getPeer(fromId) ?: run {
            LogCat.e("Invite from unknown peer $fromId — ignored")
            return
        }

        // Create peer records for channel members we don't already know.
        // These are created with status="channel" and key="" (no shared encryption key).
        for (memberInfo in msg.memberPeers) {
            ensureChannelPeer(
                id = memberInfo.id,
                name = memberInfo.name,
                publicKey = memberInfo.publicKey,
                deviceType = memberInfo.deviceType,
                ip = memberInfo.ip,
                port = memberInfo.port,
            )
        }

        if (isReinvite) {
            // Re-invite after leaving or being kicked: update metadata and restore joined status
            val channel = existingChannel
            channel.name = msg.channelName
            channel.key = msg.key
            channel.owner = fromId
            channel.members = msg.members
            channel.version = msg.version
            channel.status = DChatChannel.STATUS_JOINED
            AppDatabase.instance.chatChannelDao().update(channel)
            ChannelCacher.updateChannel(channel)
            LogCat.d("Re-invite for channel ${msg.channelId} (was ${existingChannel.status}), restored to joined")
        } else {
            // Store channel locally with members carrying only id + status
            val channel = DChatChannel()
            channel.id = msg.channelId
            channel.name = msg.channelName
            channel.key = msg.key
            channel.owner = fromId
            channel.members = msg.members
            channel.version = msg.version
            AppDatabase.instance.chatChannelDao().insert(channel)
        }

        PeerCacher.load()
        ChannelCacher.load()

        // Notify UI to show the invite dialog
        val peerName = peer.name.ifEmpty { fromId }
        sendEvent(
            ChannelInviteReceivedEvent(
                channelId = msg.channelId,
                channelName = msg.channelName,
                ownerPeerId = fromId,
                ownerPeerName = peerName,
            )
        )

        LogCat.d("Channel invite received: ${msg.channelName} from $fromId")
    }

    private suspend fun handleInviteAccept(fromId: String, msg: ChannelSystemMessages.ChannelInviteAccept) {
        val channel = ChannelCacher.getChannel(msg.channelId) ?: run {
            LogCat.e("InviteAccept for unknown channel ${msg.channelId}")
            return
        }

        if (!channel.isOwnedByMe()) {
            LogCat.e("InviteAccept received but we are not the owner of ${msg.channelId}")
            return
        }

        // Ensure we have a peer record for the accepting member.
        // If the peer doesn't exist, create a channel peer using the info from the accept message.
        val existingPeer = PeerCacher.getPeer(fromId)
        if (existingPeer == null) {
            ensureChannelPeer(
                id = fromId,
                name = msg.name,
                publicKey = msg.publicKey,
                deviceType = msg.deviceType,
                logTag = "accepting member",
            )
        } else if (existingPeer.publicKey.isEmpty() && msg.publicKey.isNotEmpty()) {
            // Update public key if we didn't have it
            existingPeer.publicKey = msg.publicKey
            if (msg.name.isNotEmpty() && existingPeer.name.isEmpty()) {
                existingPeer.name = msg.name
            }
            AppDatabase.instance.peerDao().update(existingPeer)
            PeerCacher.updatePeer(existingPeer)
        }

        // Only accept invites from peers that were already on the pending list.
        // Any peer not previously invited is rejected — otherwise anyone who knew the
        // channel id could self-add and start receiving future broadcast updates
        // (which include the channelKey via ChannelSystemMessageSender.broadcastUpdate).
        val member = channel.findMember(fromId)
        if (member == null) {
            LogCat.e("InviteAccept from $fromId for channel ${msg.channelId} but peer is not in members list — rejected")
            return
        }
        if (!member.isPending()) {
            LogCat.d("InviteAccept from $fromId but member is not pending (status=${member.status}), ignoring")
            return
        }

        // Move from pending → joined
        channel.members = channel.members.map {
            if (it.id == fromId) it.copy(status = ChannelMember.STATUS_JOINED) else it
        }

        channel.version++
        channel.updatedAt = TimeHelper.now()
        AppDatabase.instance.chatChannelDao().update(channel)
        ChannelCacher.updateChannel(channel)

        ChannelSystemMessageSender.broadcastUpdate(channel)

        LogCat.d("Peer $fromId accepted invite for channel ${msg.channelId}")
    }

    private suspend fun handleInviteDecline(fromId: String, msg: ChannelSystemMessages.ChannelInviteDecline) {
        val channel = ChannelCacher.getChannel(msg.channelId) ?: return

        if (!channel.isOwnedByMe()) return

        // Remove the declining peer from members
        if (channel.hasMember(fromId)) {
            channel.members = channel.members.filter { it.id != fromId }
            channel.version++
            channel.updatedAt = TimeHelper.now()
            AppDatabase.instance.chatChannelDao().update(channel)
            ChannelCacher.updateChannel(channel)
        }

        LogCat.d("Peer $fromId declined invite for channel ${msg.channelId}")
    }

    private suspend fun handleUpdate(fromId: String, msg: ChannelSystemMessages.ChannelUpdate) {
        val channel = ChannelCacher.getChannel(msg.channelId)

        if (channel == null) {
            LogCat.e("ChannelUpdate for unknown channel ${msg.channelId}")
            return
        }

        // Only the channel owner may broadcast updates
        if (channel.owner != fromId) {
            LogCat.e("ChannelUpdate from non-owner $fromId (owner=${channel.owner}) — rejected")
            return
        }

        // Cryptographic ownership check: the per-message [signature] must verify
        // against the owner's public key (from local DPeer row) over the
        // canonical payload `"$channelId|$version|update|"`. Transport-layer
        // Ed25519 already proves fromId is who they say they are; this check
        // proves they actually control the channel's owner private key.
        val ownerPeer = PeerCacher.getPeer(channel.owner)
        if (ownerPeer == null) {
            LogCat.e("ChannelUpdate: owner peer ${channel.owner} not found locally — rejected")
            return
        }
        val updatePayload = channelMessagePayload(
            channelId = msg.channelId,
            version = msg.version,
            action = ChannelSystemMessages.ACTION_UPDATE,
            target = "",
        )
        if (!verifyEd25519Signature(ownerPeer.publicKey, updatePayload, msg.signature)) {
            LogCat.e("ChannelUpdate signature failed for channel ${msg.channelId} from $fromId — rejected")
            return
        }

        // Only accept updates with a higher version (optimistic concurrency)
        if (msg.version <= channel.version) {
            LogCat.d("Ignoring stale ChannelUpdate (local=${channel.version}, remote=${msg.version})")
            return
        }

        // Create peer records for any new members we don't already know
        for (memberInfo in msg.memberPeers) {
            ensureChannelPeer(
                id = memberInfo.id,
                name = memberInfo.name,
                publicKey = memberInfo.publicKey,
                deviceType = memberInfo.deviceType,
                ip = memberInfo.ip,
                port = memberInfo.port,
                logTag = "member via update",
            )
        }

        channel.name = msg.channelName
        channel.members = msg.members
        channel.version = msg.version
        channel.updatedAt = TimeHelper.now()
        AppDatabase.instance.chatChannelDao().update(channel)
        ChannelCacher.updateChannel(channel)

        PeerCacher.load()

        LogCat.d("Channel ${msg.channelId} updated to version ${msg.version}")
    }

    private suspend fun handleKick(fromId: String, msg: ChannelSystemMessages.ChannelKick) {
        val channel = ChannelCacher.getChannel(msg.channelId) ?: return

        // Only the channel owner may kick members
        if (channel.owner != fromId) {
            LogCat.e("ChannelKick from non-owner $fromId (owner=${channel.owner}) — rejected")
            return
        }

        // Cryptographic ownership check: per-message [signature] must verify
        // against owner's public key over `"$channelId|$version|kick|<our peer id>"`.
        // The target binding ensures this kick was addressed to us specifically,
        // not replayed to another member.
        val ownerPeer = PeerCacher.getPeer(channel.owner)
        if (ownerPeer == null) {
            LogCat.e("ChannelKick: owner peer ${channel.owner} not found locally — rejected")
            return
        }
        val kickPayload = channelMessagePayload(
            channelId = msg.channelId,
            version = msg.version,
            action = ChannelSystemMessages.ACTION_KICK,
            target = TempData.clientId,
        )
        if (!verifyEd25519Signature(ownerPeer.publicKey, kickPayload, msg.signature)) {
            LogCat.e("ChannelKick signature failed for channel ${msg.channelId} from $fromId — rejected")
            return
        }

        // Update channel status to kicked; keep channel and chat history intact
        val wasPending = channel.findMember(TempData.clientId)?.isPending() == true
        channel.status = DChatChannel.STATUS_KICKED
        // Remove self from the members list so we no longer appear in the members grid
        channel.members = channel.members.filter { it.id != TempData.clientId }
        AppDatabase.instance.chatChannelDao().update(channel)
        ChannelCacher.updateChannel(channel)

        if (wasPending) {
            // Owner cancelled a pending invite — dismiss the auto-opened accept page on our side.
            sendEvent(ChannelInviteCanceledEvent(channelId = msg.channelId, ownerPeerId = fromId))
        }
        LogCat.d("Kicked from channel ${msg.channelId} by $fromId")
    }

    private suspend fun handleLeave(fromId: String, msg: ChannelSystemMessages.ChannelLeave) {
        val channel = ChannelCacher.getChannel(msg.channelId) ?: return

        if (!channel.isOwnedByMe()) {
            LogCat.e("ChannelLeave received but we are not the owner of ${msg.channelId}")
            return
        }

        // Remove the leaving peer from members
        channel.members = channel.members.filter { it.id != fromId }
        channel.version++
        channel.updatedAt = TimeHelper.now()
        AppDatabase.instance.chatChannelDao().update(channel)
        ChannelCacher.updateChannel(channel)

        ChannelSystemMessageSender.broadcastUpdate(channel)

        LogCat.d("Peer $fromId left channel ${msg.channelId}")
    }
}