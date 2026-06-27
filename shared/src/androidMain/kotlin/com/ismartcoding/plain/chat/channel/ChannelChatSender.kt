package com.ismartcoding.plain.chat.channel

import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerGraphQLClient
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageDeliveryResult
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.toPeerMessageContent

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
 * - If this device **is not** the leader: send only to the leader;
 *   the leader is responsible for relaying.
 *
 * Encryption:
 * - If the sender and the target peer are paired (peer.key is non-empty),
 *   the message is encrypted with the peer's shared key.
 * - Otherwise, the channel key is used with the `c-cid` header so the
 *   receiver selects the correct decryption path.
 */
object ChannelChatSender {
    sealed class Result {
        data class Status(val data: DMessageStatusData) : Result()
        data object NoLeader : Result()
        data class LeaderPeerMissing(val leaderId: String) : Result()
    }

    suspend fun send(
        channel: DChatChannel,
        content: DMessageContent,
        onlinePeerIds: Set<String> = emptySet(),
    ): Result {
        val leaderId = channel.electLeader(onlinePeerIds, TempData.clientId)
        if (leaderId == null) {
            LogCat.e("Channel ${channel.id}: no online leader available")
            return Result.NoLeader
        }

        return if (leaderId == TempData.clientId) {
            // We are the leader — broadcast to everyone else
            Result.Status(broadcastAsLeader(channel, content))
        } else {
            // Send to leader only
            sendToLeader(channel, leaderId, content)
        }
    }

    private suspend fun broadcastAsLeader(channel: DChatChannel, content: DMessageContent): DMessageStatusData {
        return sendToRecipients(channel, channel.getRecipientIds(), content)
    }

    suspend fun sendToRecipients(
        channel: DChatChannel,
        recipientIds: List<String>,
        content: DMessageContent,
    ): DMessageStatusData = withIO {
        if (recipientIds.isEmpty()) {
            LogCat.d("Channel ${channel.id}: no recipients to send to")
            DMessageStatusData()
        } else {
            val peerDao = AppDatabase.instance.peerDao()
            val results = mutableListOf<DMessageDeliveryResult>()
            for (memberId in recipientIds) {
                val memberPeer = peerDao.getById(memberId)
                if (memberPeer == null) {
                    LogCat.e("Channel ${channel.id}: peer $memberId not found in DB, skipping")
                    results.add(DMessageDeliveryResult(memberId, memberId, "Peer not found in database"))
                    continue
                }
                results.add(sendToMember(channel, memberPeer, content))
            }
            DMessageStatusData(results)
        }
    }

    private suspend fun sendToLeader(
        channel: DChatChannel,
        leaderId: String,
        content: DMessageContent,
    ): Result {
        val peerDao = AppDatabase.instance.peerDao()
        val leaderPeer = peerDao.getById(leaderId)
        if (leaderPeer == null) {
            LogCat.e("Channel ${channel.id}: leader peer $leaderId not found in DB")
            return Result.LeaderPeerMissing(leaderId)
        }
        val result = sendToMember(channel, leaderPeer, content)
        return Result.Status(DMessageStatusData(listOf(result)))
    }

    suspend fun sendToMember(
        channel: DChatChannel,
        peer: DPeer,
        content: DMessageContent,
    ): DMessageDeliveryResult = withIO {
        try {
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
}
