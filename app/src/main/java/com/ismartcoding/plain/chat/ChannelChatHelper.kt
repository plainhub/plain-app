package com.ismartcoding.plain.chat

import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageDeliveryResult
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.toJSONString
import com.ismartcoding.plain.helpers.FileHelper

/**
 * Handles sending chat messages in a channel using a **star topology**.
 *
 * Topology:
 * ```
 *   B
 *   |
 * A--L--C
 *   |
 *   D
 * ```
 *
 * All messages flow:  client → leader → broadcast to all other members.
 *
 * - If this device **is** the leader: broadcast directly to every other member.
 * - If this device **is not** the leader: send to the leader only;
 *   the leader is responsible for relaying.
 *
 * Encryption:
 * - If the sender and the target peer are paired (peer.key is non-empty),
 *   the message is encrypted with the peer's shared key.
 * - Otherwise, the channel key is used with the `c-cid` header so the
 *   receiver selects the correct decryption path.
 */
object ChannelChatHelper {

    /**
     * Send [content] from this device into the channel.
     *
     * If this device is the leader, broadcast to all other members.
     * Otherwise, send only to the leader (who will relay).
     *
     * Returns [DMessageStatusData] with per-member delivery results, or null when
     * no leader is available.
     *
     * @param onlinePeerIds set of peer ids known to be online, used for leader election.
     */
    suspend fun sendAsync(
        channel: DChatChannel,
        content: DMessageContent,
        onlinePeerIds: Set<String> = emptySet(),
    ): DMessageStatusData? {
        val leaderId = channel.electLeader(onlinePeerIds)
        if (leaderId == null) {
            LogCat.e("Channel ${channel.id}: no online leader available")
            return null
        }

        return if (leaderId == TempData.clientId) {
            // We are the leader — broadcast to everyone else
            broadcastAsLeader(channel, content)
        } else {
            // Send to leader only
            sendToLeaderAsync(channel, leaderId, content)
        }
    }

    /**
     * Leader broadcasts [content] to all other joined members.
     * Returns [DMessageStatusData] with a [DMessageDeliveryResult] per recipient.
     */
    suspend fun broadcastAsLeader(channel: DChatChannel, content: DMessageContent): DMessageStatusData {
        val recipientIds = getRecipientIds(channel)
        if (recipientIds.isEmpty()) {
            LogCat.d("Channel ${channel.id}: no recipients to broadcast to")
            return DMessageStatusData()
        }

        val peerDao = AppDatabase.instance.peerDao()
        val results = mutableListOf<DMessageDeliveryResult>()
        for (memberId in recipientIds) {
            val memberPeer = peerDao.getById(memberId)
            if (memberPeer == null) {
                LogCat.e("Channel ${channel.id}: peer $memberId not found in DB, skipping")
                results.add(DMessageDeliveryResult(memberId, memberId, "Peer not found in database"))
                continue
            }
            val result = sendToMemberAsync(channel, memberPeer, content)
            results.add(result)
        }
        return DMessageStatusData(results)
    }

    /**
     * Send [content] to the channel leader for relaying.
     * Returns [DMessageStatusData] with a single entry for the leader, or null if leader not found.
     */
    private suspend fun sendToLeaderAsync(
        channel: DChatChannel,
        leaderId: String,
        content: DMessageContent,
    ): DMessageStatusData? {
        val peerDao = AppDatabase.instance.peerDao()
        val leaderPeer = peerDao.getById(leaderId)
        if (leaderPeer == null) {
            LogCat.e("Channel ${channel.id}: leader peer $leaderId not found in DB")
            return null
        }
        val result = sendToMemberAsync(channel, leaderPeer, content)
        return DMessageStatusData(listOf(result))
    }

    /**
     * Send [content] to a single channel member.
     * Returns [DMessageDeliveryResult] with error = null on success, or an error message.
     *
     * If the member is a paired peer (peer.key is non-empty), uses the peer's shared key.
     * Otherwise, uses the channel key with `c-cid` header.
     */
    suspend fun sendToMemberAsync(
        channel: DChatChannel,
        peer: DPeer,
        content: DMessageContent,
    ): DMessageDeliveryResult {
        return try {
            val modifiedContent = content.toPeerMessageContent()
            val response = PeerGraphQLClient.createChannelChatItem(
                peer = peer,
                channelId = channel.id,
                channelKey = channel.key,
                clientId = TempData.clientId,
                content = modifiedContent,
            )

            if (response != null && response.errors.isNullOrEmpty()) {
                LogCat.d("Channel message sent to ${peer.id} via channel ${channel.id}")
                DMessageDeliveryResult(peer.id, peer.name, null)
            } else {
                val errors = if (response == null) {
                    "No response received (host unreachable or connection refused)"
                } else {
                    response.errors?.joinToString("; ") { it.message } ?: "Empty error list in response"
                }
                LogCat.e("Failed to send channel message to ${peer.id}: $errors")
                DMessageDeliveryResult(peer.id, peer.name, errors)
            }
        } catch (e: Exception) {
            val msg = e.toString()
            LogCat.e("Error sending channel message to ${peer.id}: $msg")
            DMessageDeliveryResult(peer.id, peer.name, msg)
        }
    }

    /**
     * Compute the list of peer IDs that should receive a channel message (everyone except self).
     * Only includes joined members.
     */
    fun getRecipientIds(channel: DChatChannel): List<String> {
        return channel.joinedMembers()
            .map { it.id }
            .distinct()
            .filter { it != TempData.clientId }
    }
}
