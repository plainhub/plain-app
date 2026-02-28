package com.ismartcoding.plain.chat

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.JsonHelper.jsonDecode
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.ChannelMember
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.ChannelInviteReceivedEvent
import com.ismartcoding.plain.events.ChannelUpdatedEvent
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Handles incoming channel system messages received via [PeerGraphQL].
 *
 * Each handler updates the local DB and fires UI events as needed.
 * Methods run on the caller's thread (the Ktor request thread); DB calls are lightweight.
 */
object ChannelSystemMessageHandler {

    fun handle(fromId: String, type: String, payload: String) {
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

    private fun handleInvite(fromId: String, msg: ChannelSystemMessages.ChannelInvite) {
        val dao = AppDatabase.instance.chatChannelDao()
        val peerDao = AppDatabase.instance.peerDao()

        // Avoid duplicate processing
        if (dao.getById(msg.channelId) != null) {
            LogCat.d("Channel ${msg.channelId} already exists locally, ignoring invite")
            return
        }

        // Verify the sender is a known paired peer
        val peer = peerDao.getById(fromId) ?: run {
            LogCat.e("Invite from unknown peer $fromId — ignored")
            return
        }

        // Create peer records for channel members we don't already know.
        // These are created with status="channel" and key="" (no shared encryption key).
        for (memberInfo in msg.memberPeers) {
            if (peerDao.getById(memberInfo.id) == null) {
                val newPeer = DPeer(
                    id = memberInfo.id,
                    name = memberInfo.name,
                    publicKey = memberInfo.publicKey,
                    status = "channel",
                    deviceType = memberInfo.deviceType,
                    ip = memberInfo.ip,
                    port = memberInfo.port,
                )
                peerDao.insert(newPeer)
                LogCat.d("Created channel peer record for ${memberInfo.id}")
            }
        }

        // Store channel locally with members carrying only id + status
        val channel = DChatChannel()
        channel.id = msg.channelId
        channel.name = msg.channelName
        channel.key = msg.key
        channel.owner = fromId
        channel.members = msg.members
        channel.version = msg.version
        dao.insert(channel)

        // Refresh key cache so we can decrypt channel messages
        runBlocking(Dispatchers.IO) {
            ChatCacheManager.loadKeyCacheAsync()
        }

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

        sendEvent(ChannelUpdatedEvent())
        LogCat.d("Channel invite received: ${msg.channelName} from $fromId")
    }

    private fun handleInviteAccept(fromId: String, msg: ChannelSystemMessages.ChannelInviteAccept) {
        val dao = AppDatabase.instance.chatChannelDao()
        val peerDao = AppDatabase.instance.peerDao()
        val channel = dao.getById(msg.channelId) ?: run {
            LogCat.e("InviteAccept for unknown channel ${msg.channelId}")
            return
        }

        if (channel.owner != "me") {
            LogCat.e("InviteAccept received but we are not the owner of ${msg.channelId}")
            return
        }

        // Ensure we have a peer record for the accepting member.
        // If the peer doesn't exist, create a channel peer using the info from the accept message.
        val existingPeer = peerDao.getById(fromId)
        if (existingPeer == null) {
            val newPeer = DPeer(
                id = fromId,
                name = msg.name,
                publicKey = msg.publicKey,
                status = "channel",
                deviceType = msg.deviceType,
            )
            peerDao.insert(newPeer)
            LogCat.d("Created channel peer record for accepting member $fromId")
        } else if (existingPeer.publicKey.isEmpty() && msg.publicKey.isNotEmpty()) {
            // Update public key if we didn't have it
            existingPeer.publicKey = msg.publicKey
            if (msg.name.isNotEmpty() && existingPeer.name.isEmpty()) {
                existingPeer.name = msg.name
            }
            peerDao.update(existingPeer)
        }

        // Move from pending → joined
        val member = channel.findMember(fromId)
        if (member != null && member.isPending()) {
            channel.members = channel.members.map {
                if (it.id == fromId) it.copy(status = ChannelMember.STATUS_JOINED) else it
            }
        } else if (member == null) {
            // Peer was not in the members list at all — add as joined
            channel.members += ChannelMember(id = fromId, status = ChannelMember.STATUS_JOINED)
        }

        channel.version++
        channel.updatedAt = TimeHelper.now()
        dao.update(channel)

        // Broadcast updated membership to all
        runBlocking(Dispatchers.IO) {
            ChannelSystemMessageSender.broadcastUpdate(channel)
            ChatCacheManager.loadKeyCacheAsync()
        }

        sendEvent(ChannelUpdatedEvent())
        LogCat.d("Peer $fromId accepted invite for channel ${msg.channelId}")
    }

    private fun handleInviteDecline(fromId: String, msg: ChannelSystemMessages.ChannelInviteDecline) {
        val dao = AppDatabase.instance.chatChannelDao()
        val channel = dao.getById(msg.channelId) ?: return

        if (channel.owner != "me") return

        // Remove the declining peer from members
        if (channel.hasMember(fromId)) {
            channel.members = channel.members.filter { it.id != fromId }
            channel.version++
            channel.updatedAt = TimeHelper.now()
            dao.update(channel)
        }

        sendEvent(ChannelUpdatedEvent())
        LogCat.d("Peer $fromId declined invite for channel ${msg.channelId}")
    }

    private fun handleUpdate(fromId: String, msg: ChannelSystemMessages.ChannelUpdate) {
        val dao = AppDatabase.instance.chatChannelDao()
        val peerDao = AppDatabase.instance.peerDao()
        val channel = dao.getById(msg.channelId)

        if (channel == null) {
            LogCat.e("ChannelUpdate for unknown channel ${msg.channelId}")
            return
        }

        // Only the channel owner may broadcast updates
        if (channel.owner != fromId) {
            LogCat.e("ChannelUpdate from non-owner $fromId (owner=${channel.owner}) — rejected")
            return
        }

        // Only accept updates with a higher version (optimistic concurrency)
        if (msg.version <= channel.version) {
            LogCat.d("Ignoring stale ChannelUpdate (local=${channel.version}, remote=${msg.version})")
            return
        }

        // Create peer records for any new members we don't already know
        for (memberInfo in msg.memberPeers) {
            if (peerDao.getById(memberInfo.id) == null) {
                val newPeer = DPeer(
                    id = memberInfo.id,
                    name = memberInfo.name,
                    publicKey = memberInfo.publicKey,
                    status = "channel",
                    deviceType = memberInfo.deviceType,
                    ip = memberInfo.ip,
                    port = memberInfo.port,
                )
                peerDao.insert(newPeer)
                LogCat.d("Created channel peer record for ${memberInfo.id} via update")
            }
        }

        channel.name = msg.channelName
        channel.members = msg.members
        channel.version = msg.version
        channel.updatedAt = TimeHelper.now()
        dao.update(channel)

        // Refresh cached keys
        runBlocking(Dispatchers.IO) {
            ChatCacheManager.loadKeyCacheAsync()
        }

        sendEvent(ChannelUpdatedEvent())
        LogCat.d("Channel ${msg.channelId} updated to version ${msg.version}")
    }

    private fun handleKick(fromId: String, msg: ChannelSystemMessages.ChannelKick) {
        val dao = AppDatabase.instance.chatChannelDao()
        val channel = dao.getById(msg.channelId) ?: return

        // Only the channel owner may kick members
        if (channel.owner != fromId) {
            LogCat.e("ChannelKick from non-owner $fromId (owner=${channel.owner}) — rejected")
            return
        }

        // Delete channel and its chats locally
        dao.delete(msg.channelId)
        AppDatabase.instance.chatDao().deleteByChannelId(msg.channelId)

        runBlocking(Dispatchers.IO) {
            ChatCacheManager.loadKeyCacheAsync()
        }

        sendEvent(ChannelUpdatedEvent())
        LogCat.d("Kicked from channel ${msg.channelId} by $fromId")
    }

    private fun handleLeave(fromId: String, msg: ChannelSystemMessages.ChannelLeave) {
        val dao = AppDatabase.instance.chatChannelDao()
        val channel = dao.getById(msg.channelId) ?: return

        if (channel.owner != "me") {
            LogCat.e("ChannelLeave received but we are not the owner of ${msg.channelId}")
            return
        }

        // Remove the leaving peer from members
        channel.members = channel.members.filter { it.id != fromId }
        channel.version++
        channel.updatedAt = TimeHelper.now()
        dao.update(channel)

        // Broadcast updated membership to remaining members
        runBlocking(Dispatchers.IO) {
            ChannelSystemMessageSender.broadcastUpdate(channel)
        }

        sendEvent(ChannelUpdatedEvent())
        LogCat.d("Peer $fromId left channel ${msg.channelId}")
    }
}
