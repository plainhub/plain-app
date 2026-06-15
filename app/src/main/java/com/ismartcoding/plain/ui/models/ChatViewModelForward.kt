package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.chat.channel.ChannelChatSender
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.chat.ChatSender
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DMessageDeliveryResult
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray

fun ChatViewModel.resendMessage(messageId: String) {
    viewModelScope.launch(Dispatchers.IO) {
        val item = ChatDbHelper.getChatItem(messageId) ?: return@launch
        val state = chatState.value
        ChatDbHelper.updateChatItemStatus(item, "pending")
        update(item)
        ChatSender.resend(item, onlinePeerIds.value)
    }
}

fun ChatViewModel.resendToMembers(messageId: String, peerIds: List<String>) {
    viewModelScope.launch(Dispatchers.IO) {
        val state = chatState.value
        val channel = AppDatabase.instance.chatChannelDao().getById(state.target.toId) ?: return@launch
        val item = ChatDbHelper.getChatItem(messageId) ?: return@launch
        ChatDbHelper.updateChatItemStatus(item, "pending")
        update(item)
        ChatSender.sendToChannelMembers(item, channel, peerIds)
        update(item)
    }
}

fun ChatViewModel.forwardMessage(messageId: String, target: ChatTarget, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch(Dispatchers.IO) {
        val item = ChatDbHelper.getChatItem(messageId) ?: return@launch
        val newItem = ChatSender.createChatItem(target, item.content)
        val state = chatState.value
        ChatSender.send(newItem, target, onlinePeerIds.value)
        update(newItem)
        onResult(newItem.status == "sent")
    }
}

fun ChatViewModel.delete(context: Context, ids: Set<String>) {
    viewModelScope.launch(Dispatchers.IO) {
        val json = JSONArray()
        val items = itemsFlow.value.filter { ids.contains(it.id) }
        for (m in items) {
            ChatDbHelper.deleteAsync(context, m.id)
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
        if (state.target.type == ChatTargetType.CHANNEL) {
            ChatDbHelper.deleteAllChannelChatsAsync(context, state.target.toId)
        } else {
            ChatDbHelper.deleteAllChatsAsync(context, state.target.toId)
        }
        _itemsFlow.value = mutableStateListOf()
        sendEvent(WebSocketEvent(EventType.MESSAGE_CLEARED, JsonHelper.jsonEncode(state.target.toId)))
    }
}
