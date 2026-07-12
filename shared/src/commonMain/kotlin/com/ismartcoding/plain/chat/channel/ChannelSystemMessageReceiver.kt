package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.platform.AppDatabase
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

        if (msg.owner != fromId) {
            LogCat.e("Invite from $fromId but payload claims owner=${msg.owner} — rejected")
            return
        }

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

        if (existingChannel != null && !isReinvite) {
            LogCat.d("Channel ${msg.channelId} already exists locally, ignoring invite")
            return
        }

        val peer = PeerCacher.getPeer(fromId) ?: run {
            LogCat.e("Invite from unknown peer $fromId — ignored")
            return
        }

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
            existingPeer.publicKey = msg.publicKey
            if (msg.name.isNotEmpty() && existingPeer.name.isEmpty()) {
                existingPeer.name = msg.name
            }
            AppDatabase.instance.peerDao().update(existingPeer)
            PeerCacher.updatePeer(existingPeer)
        }

        val member = channel.findMember(fromId)
        if (member == null) {
            LogCat.e("InviteAccept from $fromId for channel ${msg.channelId} but peer is not in members list — rejected")
            return
        }
        if (!member.isPending()) {
            LogCat.d("InviteAccept from $fromId but member is not pending (status=${member.status}), ignoring")
            return
        }

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

        if (channel.owner != fromId) {
            LogCat.e("ChannelUpdate from non-owner $fromId (owner=${channel.owner}) — rejected")
            return
        }

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

        if (msg.version <= channel.version) {
            LogCat.d("Ignoring stale ChannelUpdate (local=${channel.version}, remote=${msg.version})")
            return
        }

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

        if (channel.owner != fromId) {
            LogCat.e("ChannelKick from non-owner $fromId (owner=${channel.owner}) — rejected")
            return
        }

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

        val wasPending = channel.findMember(TempData.clientId)?.isPending() == true
        channel.status = DChatChannel.STATUS_KICKED
        channel.members = channel.members.filter { it.id != TempData.clientId }
        AppDatabase.instance.chatChannelDao().update(channel)
        ChannelCacher.updateChannel(channel)

        if (wasPending) {
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

        channel.members = channel.members.filter { it.id != fromId }
        channel.version++
        channel.updatedAt = TimeHelper.now()
        AppDatabase.instance.chatChannelDao().update(channel)
        ChannelCacher.updateChannel(channel)

        ChannelSystemMessageSender.broadcastUpdate(channel)

        LogCat.d("Peer $fromId left channel ${msg.channelId}")
    }
}
