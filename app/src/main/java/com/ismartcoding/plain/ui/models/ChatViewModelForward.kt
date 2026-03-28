package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.chat.ChannelChatHelper
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.chat.PeerChatHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DMessageDeliveryResult
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.web.models.toModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray

fun ChatViewModel.retryMessage(messageId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        val state = chatState.value
        val item = ChatDbHelper.getAsync(messageId) ?: return@launch
        if (state.isRemote()) {
            ChatDbHelper.updateStatusAsync(messageId, "pending")
            item.status = "pending"
            update(item)
            val outcome = deliverToRemoteAsync(state, item.content)
            applyDeliveryOutcome(item, outcome)
            update(item)
        }
    }
}

fun ChatViewModel.resendToMembers(messageId: String, peerIds: List<String>) {
    viewModelScope.launch(Dispatchers.IO) {
        val state = chatState.value
        if (state.chatType != ChatType.CHANNEL) return@launch
        val channel = AppDatabase.instance.chatChannelDao().getById(state.toId) ?: return@launch
        val item = ChatDbHelper.getAsync(messageId) ?: return@launch

        ChatDbHelper.updateStatusAsync(messageId, "pending")
        item.status = "pending"
        update(item)

        val peerDao = AppDatabase.instance.peerDao()
        val newResults = mutableListOf<DMessageDeliveryResult>()
        for (peerId in peerIds) {
            val peer = peerDao.getById(peerId)
            if (peer == null) {
                newResults.add(DMessageDeliveryResult(peerId, peerId, "Peer not found in database"))
                continue
            }
            newResults.add(ChannelChatHelper.sendToMemberAsync(channel, peer, item.content))
        }

        val existing = item.parseStatusData()?.results ?: emptyList()
        val retriedIds = peerIds.toSet()
        val merged = existing.filter { it.peerId !in retriedIds } + newResults
        val mergedStatusData = DMessageStatusData(merged)

        ChatDbHelper.updateStatusAndDataAsync(item.id, mergedStatusData)
        item.status = when {
            mergedStatusData.total == 0 -> "sent"
            mergedStatusData.allDelivered -> "sent"
            mergedStatusData.allFailed -> "failed"
            else -> "partial"
        }
        item.statusData = if (mergedStatusData.total > 0) JsonHelper.jsonEncode(mergedStatusData) else ""
        update(item)
    }
}

fun ChatViewModel.forwardMessage(messageId: String, targetPeer: DPeer, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch(Dispatchers.IO) {
        val item = ChatDbHelper.getAsync(messageId) ?: return@launch
        val newItem = ChatDbHelper.sendAsync(message = item.content, fromId = "me", toId = targetPeer.id, peer = targetPeer)
        val model = newItem.toModel().apply { data = getContentData() }
        sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(model))))
        if (newItem.content.type == DMessageType.TEXT.value) sendEvent(FetchLinkPreviewsEvent(newItem))

        val error = PeerChatHelper.sendToPeerAsync(targetPeer, newItem.content)
        val outcome = if (error != null) {
            triggerPeerRediscovery(targetPeer.id)
            DeliveryOutcome(false, DMessageStatusData(listOf(DMessageDeliveryResult(targetPeer.id, targetPeer.name, error))))
        } else {
            DeliveryOutcome(true)
        }
        applyDeliveryOutcome(newItem, outcome)
        update(newItem)
        onResult(error == null)
    }
}

fun ChatViewModel.forwardMessageToLocal(messageId: String, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch(Dispatchers.IO) {
        val item = ChatDbHelper.getAsync(messageId) ?: return@launch
        val newItem = ChatDbHelper.sendAsync(message = item.content, fromId = "me", toId = "local", peer = null)
        val model = newItem.toModel().apply { data = getContentData() }
        sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(model))))
        if (newItem.content.type == DMessageType.TEXT.value) sendEvent(FetchLinkPreviewsEvent(newItem))
        onResult(true)
    }
}

fun ChatViewModel.delete(context: Context, ids: Set<String>) {
    viewModelScope.launch(Dispatchers.IO) {
        val json = JSONArray()
        val items = itemsFlow.value.filter { ids.contains(it.id) }
        for (m in items) {
            ChatDbHelper.deleteAsync(context, m.id, m.value)
            json.put(m.id)
        }
        _itemsFlow.update {
            val mutableList = it.toMutableStateList()
            mutableList.removeIf { m -> ids.contains(m.id) }
            mutableList
        }
        sendEvent(WebSocketEvent(EventType.MESSAGE_DELETED, json.toString()))
    }
}

fun ChatViewModel.clearAllMessages(context: Context) {
    viewModelScope.launch(Dispatchers.IO) {
        val state = chatState.value
        if (state.chatType == ChatType.CHANNEL) {
            ChatDbHelper.deleteAllChannelChatsAsync(context, state.toId)
        } else {
            ChatDbHelper.deleteAllChatsAsync(context, state.toId)
        }
        _itemsFlow.value = mutableStateListOf()
        sendEvent(WebSocketEvent(EventType.MESSAGE_CLEARED, JsonHelper.jsonEncode(state.toId)))
    }
}
