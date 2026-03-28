package com.ismartcoding.plain.ui.models

import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.ChannelChatHelper
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.chat.PeerChatHelper
import com.ismartcoding.plain.chat.discover.NearbyDiscoverManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageDeliveryResult
import com.ismartcoding.plain.db.DMessageStatusData

internal data class DeliveryOutcome(
    val success: Boolean,
    val statusData: DMessageStatusData? = null,
)

internal fun ChatState.isRemote(): Boolean = chatType != ChatType.LOCAL

internal suspend fun deliverToRemoteAsync(state: ChatState, content: DMessageContent): DeliveryOutcome {
    return when (state.chatType) {
        ChatType.PEER -> {
            val peer = AppDatabase.instance.peerDao().getById(state.toId)
                ?: return DeliveryOutcome(
                    false,
                    DMessageStatusData(listOf(DMessageDeliveryResult(state.toId, state.toId, "Peer not found")))
                )
            val error = PeerChatHelper.sendToPeerAsync(peer, content)
            if (error != null) {
                triggerPeerRediscovery(state.toId)
                return DeliveryOutcome(
                    false,
                    DMessageStatusData(listOf(DMessageDeliveryResult(peer.id, peer.name, error)))
                )
            }
            DeliveryOutcome(true)
        }

        ChatType.CHANNEL -> {
            val channel = AppDatabase.instance.chatChannelDao().getById(state.toId)
                ?: return DeliveryOutcome(false)
            val statusData = ChannelChatHelper.sendAsync(
                channel = channel, content = content, onlinePeerIds = state.onlinePeerIds,
            )
            if (statusData == null) {
                val leaderId = channel.electLeader(state.onlinePeerIds)
                if (leaderId != null && leaderId != TempData.clientId) {
                    triggerPeerRediscovery(leaderId)
                } else {
                    ChannelChatHelper.getRecipientIds(channel).forEach { triggerPeerRediscovery(it) }
                }
                DeliveryOutcome(false)
            } else {
                val success = statusData.total == 0 || statusData.allDelivered
                DeliveryOutcome(success, statusData)
            }
        }

        else -> DeliveryOutcome(true)
    }
}

internal fun triggerPeerRediscovery(peerId: String) {
    val key = ChatCacheManager.peerKeyCache[peerId]
    if (key != null) {
        NearbyDiscoverManager.discoverSpecificDevice(peerId, key)
    }
}

internal suspend fun applyDeliveryOutcome(item: DChat, outcome: DeliveryOutcome) {
    if (outcome.statusData != null) {
        ChatDbHelper.updateStatusAndDataAsync(item.id, outcome.statusData)
        val newStatus = when {
            outcome.statusData.total == 0 -> "sent"
            outcome.statusData.allDelivered -> "sent"
            outcome.statusData.allFailed -> "failed"
            else -> "partial"
        }
        item.status = newStatus
        item.statusData = if (outcome.statusData.total > 0) JsonHelper.jsonEncode(outcome.statusData) else ""
    } else {
        if (outcome.success) {
            ChatDbHelper.updateStatusAndDataAsync(item.id, DMessageStatusData())
            item.statusData = ""
            item.status = "sent"
        } else {
            ChatDbHelper.updateStatusAsync(item.id, "failed")
            item.status = "failed"
        }
    }
}
