package com.ismartcoding.plain.ui.models

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.plain.R
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.events.ChannelUpdatedEvent
import com.ismartcoding.plain.events.PeerUpdatedEvent
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ChatType { LOCAL, PEER, CHANNEL }

data class ChatState(
    val toId: String = "",
    val toName: String = "",
    val chatType: ChatType = ChatType.LOCAL,
    val onlinePeerIds: Set<String> = emptySet(),
)

class ChatViewModel : ISelectableViewModel<VChat>, ViewModel() {
    internal val _itemsFlow = MutableStateFlow(mutableStateListOf<VChat>())
    override val itemsFlow: StateFlow<List<VChat>> get() = _itemsFlow
    val selectedItem = mutableStateOf<VChat?>(null)
    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> get() = _chatState

    init {
        viewModelScope.launch {
            Channel.sharedFlow.collect { event ->
                when (event) {
                    is PeerUpdatedEvent -> {
                        ChatCacheManager.peerMap[event.peer.id] = event.peer
                        if (_chatState.value.chatType == ChatType.PEER && _chatState.value.toId == event.peer.id) {
                            _chatState.value = _chatState.value.copy(toName = event.peer.name)
                        }
                    }
                    is ChannelUpdatedEvent -> {
                        if (_chatState.value.chatType != ChatType.CHANNEL) return@collect
                        val channelId = _chatState.value.toId
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
        when {
            chatId.startsWith("peer:") -> {
                val toId = chatId.removePrefix("peer:")
                val peer = AppDatabase.instance.peerDao().getById(toId)
                _chatState.value = _chatState.value.copy(toId = toId, toName = peer?.name ?: "", chatType = ChatType.PEER)
            }
            chatId.startsWith("channel:") -> {
                val toId = chatId.removePrefix("channel:")
                val channel = AppDatabase.instance.chatChannelDao().getById(toId)
                _chatState.value = _chatState.value.copy(toId = toId, toName = channel?.name ?: "", chatType = ChatType.CHANNEL)
            }
            else -> {
                _chatState.value = _chatState.value.copy(toId = "local", toName = getString(R.string.local_chat), chatType = ChatType.LOCAL)
            }
        }
    }

    suspend fun fetchAsync(toId: String) {
        val state = _chatState.value
        val dao = AppDatabase.instance.chatDao()
        val isChannel = state.chatType == ChatType.CHANNEL
        val list = if (isChannel) dao.getByChannelId(state.toId) else dao.getByChatId(toId)
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
}
