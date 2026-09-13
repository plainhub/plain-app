package com.ismartcoding.plain.chat

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.textMessageContent
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.platform.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.enums.ChatStatus
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.sent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.httpserver.models.toModel
import com.ismartcoding.plain.ui.models.ISelectableViewModel
import com.ismartcoding.plain.ui.models.VChat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Process-wide chat state holder. UI (ChatPage), non-UI writers
 * (ShareSendHelper, GraphQL mutations, ChatMessageReceiver, download
 * completion) all talk to this single instance directly instead of
 * synchronizing through events. Persistence stays behind [ChatManager] so
 * the data source can later be swapped from Room to HTTP without touching
 * callers.
 *
 * Writers may run on arbitrary dispatchers; [mutex] serializes the
 * persist-then-publish sequence so per-message updates (placeholder insert
 * followed by a final-state update) land on the in-memory list in order.
 */
object ChatViewModel : ISelectableViewModel<VChat> {
    internal val _itemsFlow = MutableStateFlow<List<VChat>>(emptyList())
    override val itemsFlow: StateFlow<List<VChat>> = _itemsFlow
    val selectedItem = mutableStateOf<VChat?>(null)
    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    private val _target = MutableStateFlow(ChatTarget("local", ChatTargetType.PEER))
    val target = _target.asStateFlow()

    private val _scrollToLatest = Channel<String?>(Channel.BUFFERED)
    val scrollToLatest: Flow<String?> = _scrollToLatest.receiveAsFlow()

    private val mutex = Mutex()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private fun launchSafe(block: suspend CoroutineScope.() -> Unit) {
        scope.launch {
            runCatching { block() }
        }
    }

    suspend fun initializeTargetAsync(chatId: String) = withIO {
        _target.value = ChatTarget.parseId(chatId)
    }

    suspend fun fetchAsync(toId: String) = withIO {
        val current = _target.value
        val dao = AppDatabase.instance.chatDao()
        val isChannel = current.type == ChatTargetType.CHANNEL
        val list = if (isChannel) dao.getByChannelId(current.toId) else dao.getByPeerId(toId)
        _itemsFlow.value = list.sortedByDescending { it.createdAt }.map { chat ->
            val fromName = if (isChannel && chat.fromId != "me") {
                AppDatabase.instance.peerDao().getById(chat.fromId)?.name ?: ""
            } else ""
            VChat.from(chat, fromName)
        }
    }

    fun addAll(items: List<DChat>) {
        _itemsFlow.update { items.map { VChat.from(it) } + it }
    }

    fun addAllAndScroll(items: List<DChat>) {
        val previousTopId = _itemsFlow.value.firstOrNull()?.id
        addAll(items)
        _scrollToLatest.trySend(previousTopId)
    }

    fun update(item: DChat) {
        _itemsFlow.update { currentList ->
            val index = currentList.indexOfFirst { it.id == item.id }
            if (index >= 0) currentList.toMutableList().also { it[index] = VChat.from(item) }
            else currentList
        }
    }

    fun remove(id: String) {
        _itemsFlow.update { it.filterNot { chat -> chat.id == id } }
    }

    fun removeIds(ids: Set<String>) {
        if (ids.isEmpty()) return
        _itemsFlow.update { it.filterNot { chat -> ids.contains(chat.id) } }
    }

    fun clearAllMessages() {
        launchSafe {
            val target = target.value
            ChatManager.clearAllMessages(target)
            _itemsFlow.value = emptyList()
            sendEvent(WebSocketEvent(EventType.MESSAGE_DELETED, JsonHelper.jsonEncode(target.encodedToId)))
        }
    }

    fun resendMessage(messageId: String) {
        launchSafe {
            val item = ChatManager.getChatItem(messageId) ?: return@launchSafe
            ChatManager.updateStatus(item, ChatStatus.PENDING)
            update(item)
            ChatManager.resendMessage(item)
        }
    }

    fun resendToMembers(messageId: String, peerIds: List<String>) {
        launchSafe {
            val target = target.value
            val channel = AppDatabase.instance.chatChannelDao().getById(target.toId) ?: return@launchSafe
            val item = ChatManager.getChatItem(messageId) ?: return@launchSafe
            ChatManager.updateStatus(item, ChatStatus.PENDING)
            update(item)
            ChatManager.sendToChannelMembers(item, channel, peerIds)
            update(item)
        }
    }

    fun forwardMessage(messageId: String, target: ChatTarget, onlinePeerIds: Set<String>) {
        launchSafe {
            val item = ChatManager.getChatItem(messageId) ?: return@launchSafe
            val item2 = ChatManager.createChatItem(target, item.content)
            if (!target.isLocal()) {
                ChatManager.sendMessage(item2, target, onlinePeerIds)
            }
            sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(item2.toModel()))))
            publishCreated(target, listOf(item2))
            if (item2.status == ChatStatus.SENT) {
                DialogHelper.showSuccess(Res.string.sent)
            }
        }
    }

    fun delete(ids: Set<String>) {
        launchSafe {
            ChatManager.deleteByIds(ids)
            _itemsFlow.update { it.filterNot { m -> ids.contains(m.id) } }
            sendEvent(WebSocketEvent(EventType.MESSAGE_DELETED, JsonHelper.jsonEncode("ids=${ids.joinToString(",")}")))
        }
    }

    private suspend fun doSendMessage(target: ChatTarget, content: DMessageContent, onlinePeerIds: Set<String>): Boolean = withIO {
        val item = ChatManager.createChatItem(target, content)
        publishCreated(target, listOf(item), scroll = true)

        if (!target.isLocal()) {
            ChatManager.sendMessage(item, target, onlinePeerIds)
            publishUpdated(item)
        }
        sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(item.toModel()))))
        item.status == ChatStatus.SENT
    }

    fun sendContent(content: DMessageContent, onResult: (Boolean) -> Unit = {}) {
        launchSafe {
            onResult(doSendMessage(target.value, content, PeerCacher.getOnlinePeerIds()))
        }
    }

    fun sendTextMessage(text: String, onlinePeerIds: Set<String>, onResult: (Boolean) -> Unit = {}) {
        launchSafe {
            onResult(doSendMessage(target.value, textMessageContent(text), onlinePeerIds))
        }
    }

    suspend fun sendFilesImmediate(files: List<DMessageFile>, isImageVideo: Boolean): String = withIO {
        val item = ChatManager.insertFilesImmediate(target.value, files, isImageVideo)
        publishCreated(target.value, listOf(item), scroll = true)
        sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(item.toModel()))))
        item.id
    }

    fun updateFilesMessage(messageId: String, files: List<DMessageFile>, isImageVideo: Boolean, onlinePeerIds: Set<String>) {
        launchSafe {
            val target = target.value
            val item = ChatManager.updateFilesMessage(messageId, files, target, onlinePeerIds) ?: return@launchSafe
            publishUpdated(item)
        }
    }

    /**
     * Entry point for non-UI writers delivering already-persisted messages
     * (share-to-chat, incoming WebSocket, GraphQL-created messages). Updates
     * the open conversation when the target matches; WebSocket broadcast to
     * desktop clients stays the caller's responsibility.
     */
    suspend fun onMessagesCreated(target: ChatTarget, items: List<DChat>, scroll: Boolean = false) = mutex.withLock {
        if (_target.value == target) {
            if (scroll) {
                val previousTopId = _itemsFlow.value.firstOrNull()?.id
                _itemsFlow.update { items.map { VChat.from(it) } + it }
                _scrollToLatest.trySend(previousTopId)
            } else {
                _itemsFlow.update { items.map { VChat.from(it) } + it }
            }
        }
    }

    suspend fun onMessageUpdated(id: String) {
        val chat = ChatManager.getChatItem(id) ?: return
        mutex.withLock {
            update(chat)
        }
    }

    suspend fun onMessagesDeleted(ids: Set<String>) = mutex.withLock {
        removeIds(ids)
    }

    private suspend fun publishCreated(target: ChatTarget, items: List<DChat>, scroll: Boolean = false) =
        onMessagesCreated(target, items, scroll)

    private suspend fun publishUpdated(item: DChat) {
        mutex.withLock {
            update(item)
        }
    }
}
