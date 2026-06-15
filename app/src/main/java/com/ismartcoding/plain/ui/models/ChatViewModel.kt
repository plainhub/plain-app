package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.features.locale.LocaleHelper

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.events.ChannelUpdatedEvent
import com.ismartcoding.plain.events.PeerOnlineStatusChangedEvent
import com.ismartcoding.plain.events.PeerUpdatedEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ChatState(
    val target: ChatTarget = ChatTarget("local", ChatTargetType.PEER),
    val toName: String = "",
)

class ChatViewModel : ISelectableViewModel<VChat>, ViewModel() {
    internal val _itemsFlow = MutableStateFlow(mutableStateListOf<VChat>())
    override val itemsFlow: StateFlow<List<VChat>> get() = _itemsFlow
    val selectedItem = mutableStateOf<VChat?>(null)
    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> get() = _chatState

    private val _onlinePeerIds = MutableStateFlow<Set<String>>(emptySet())
    val onlinePeerIds: StateFlow<Set<String>> get() = _onlinePeerIds

    init {
        viewModelScope.launch {
            Channel.sharedFlow.collect { event ->
                when (event) {
                    is PeerOnlineStatusChangedEvent -> {
                        _onlinePeerIds.update { current ->
                            if (event.online) current + event.peerId
                            else current - event.peerId
                        }
                    }

                    is PeerUpdatedEvent -> {
                        ChatCacheManager.updatePeer(event.peer)
                        if (_chatState.value.target.type == ChatTargetType.PEER && _chatState.value.target.toId == event.peer.id) {
                            _chatState.value = _chatState.value.copy(toName = event.peer.name)
                        }
                    }

                    is ChannelUpdatedEvent -> {
                        if (_chatState.value.target.type != ChatTargetType.CHANNEL) return@collect
                        val channelId = _chatState.value.target.toId
                        val updated = withContext(Dispatchers.IO) {
                            AppDatabase.instance.chatChannelDao().getById(channelId)
                        }
                        _chatState.update { state -> state.copy(toName = updated?.name ?: state.toName) }
                    }
                }
            }
        }
    }

    suspend fun initializeChatStateAsync(chatId: String) {
        val target = ChatTarget.parseId(chatId)
        if (target.isLocal()) {
            _chatState.value = _chatState.value.copy(target = target, toName = LocaleHelper.getStringAsync(Res.string.local_chat))
            return
        }
        when (target.type) {
            ChatTargetType.PEER -> {
                val peer = AppDatabase.instance.peerDao().getById(target.toId)
                _chatState.value = _chatState.value.copy(target = target, toName = peer?.name ?: "")
            }

            ChatTargetType.CHANNEL -> {
                val channel = AppDatabase.instance.chatChannelDao().getById(target.toId)
                _chatState.value = _chatState.value.copy(target = target, toName = channel?.name ?: "")
            }
        }
    }

    suspend fun fetchAsync(toId: String) {
        val state = _chatState.value
        val dao = AppDatabase.instance.chatDao()
        val isChannel = state.target.type == ChatTargetType.CHANNEL
        val list = if (isChannel) dao.getByChannelId(state.target.toId) else dao.getByPeerId(toId)
        _itemsFlow.value = list.sortedByDescending { it.createdAt }.map { chat ->
            val fromName = if (isChannel && chat.fromId != "me") {
                AppDatabase.instance.peerDao().getById(chat.fromId)?.name ?: ""
            } else ""
            VChat.from(chat, fromName)
        }.toMutableStateList()
    }

    fun addAll(items: List<DChat>) {
        _itemsFlow.value.addAll(0, items.map { VChat.from(it) })
    }

    fun update(item: DChat) {
        _itemsFlow.update { currentList ->
            val mutableList = currentList.toMutableStateList()
            val index = mutableList.indexOfFirst { it.id == item.id }
            if (index >= 0) mutableList[index] = VChat.from(item)
            mutableList
        }
    }

    fun remove(id: String) {
        _itemsFlow.value.removeIf { it.id == id }
    }

    fun removeIds(ids: Set<String>) {
        if (ids.isEmpty()) return
        _itemsFlow.value.removeIf { ids.contains(it.id) }
    }
}
