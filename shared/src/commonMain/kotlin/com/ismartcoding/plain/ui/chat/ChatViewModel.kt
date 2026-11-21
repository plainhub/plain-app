package com.ismartcoding.plain.ui.chat

import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.features.chat.readChatSnapshot

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.features.chat.data.ChatTargetType
import com.ismartcoding.plain.features.chat.models.MessageContent
import com.ismartcoding.plain.features.chat.peer.transport.PeerTransportType
import com.ismartcoding.plain.features.chat.ChatStatus
import com.ismartcoding.plain.models.ChatItem
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.sent
import com.ismartcoding.plain.i18n.send_failed
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.state.ISelectableViewModel
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

class ChatViewModel internal constructor(
    val source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.rust.graphql.ContentSource.Local,
    initialTargetId: String = "local",
) : androidx.lifecycle.ViewModel(), ISelectableViewModel<VChat> {
    companion object { val local: ChatViewModel by lazy { ChatViewModel() } }
    val client = source.chats
    val downloads by lazy {
        if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Local) com.ismartcoding.plain.features.downloads.DownloadCenter
        else com.ismartcoding.plain.features.downloads.DownloadsClient(source.api, scope)
    }
    internal val sourceChanges = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    internal suspend fun observeSource() {
        val remote = source as? com.ismartcoding.plain.rust.graphql.ContentSource.Remote ?: return
        remote.client.refreshAccess()
        downloads.refresh()
        val fields = listOf("accessUpdated", "contentChanged", "messagesCreated { id }", "messagesUpdated { id }", "messagesDeleted", "peersChanged", "channelsChanged { changed }", "downloadsChanged")
        remote.client.subscriptionUpdates(fields.map { com.ismartcoding.plain.rust.graphql.GraphqlOperation("subscription { $it }") }).collect { update ->
            if (update is com.ismartcoding.plain.rust.graphql.SubscriptionUpdate.Failed) throw update.error
            if (update is com.ismartcoding.plain.rust.graphql.SubscriptionUpdate.Attached ||
                update is com.ismartcoding.plain.rust.graphql.SubscriptionUpdate.Data && update.value.data?.containsKey("accessUpdated") == true) remote.client.refreshAccess()
            fetchAsync(target.value.toId)
            downloads.refresh()
            sourceChanges.emit(Unit)
        }
    }
    val visibleConversationId = MutableStateFlow<String?>(null)
    internal val _itemsFlow = MutableStateFlow<List<VChat>>(emptyList())
    override val itemsFlow: StateFlow<List<VChat>> = _itemsFlow
    val selectedItem = mutableStateOf<VChat?>(null)
    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    private val _target = MutableStateFlow(ChatTarget.parseId(initialTargetId))
    val target = _target.asStateFlow()

    // Transport has an event snapshot, not a query; retain it before the page opens.
    val currentTransportMap = MutableStateFlow<Map<String, PeerTransportType>>(emptyMap())

    private val _scrollToLatest = Channel<String?>(Channel.BUFFERED)
    val scrollToLatest: Flow<String?> = _scrollToLatest.receiveAsFlow()

    private val mutex = Mutex()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) launchSafe { observeSource() }
    }

    override fun onCleared() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        super.onCleared()
    }

    private fun launchSafe(block: suspend CoroutineScope.() -> Unit) {
        scope.launchSafe(block = block)
    }

    suspend fun initializeTargetAsync(chatId: String) = withIO {
        mutex.withLock {
            val next = ChatTarget.parseId(chatId)
            if (_target.value != next) {
                _itemsFlow.value = emptyList()
                _target.value = next
                selectedIds.clear()
                selectMode.value = false
                selectedItem.value = null
            }
        }
    }

    suspend fun fetchAsync(toId: String) = withIO {
        val current = _target.value
        val dao = client.records
        val isChannel = current.type == ChatTargetType.CHANNEL
        val list = if (isChannel) dao.getByChannelId(current.toId) else dao.getByPeerId(toId)
        mutex.withLock {
            if (_target.value == current) _itemsFlow.value = list.sortedByDescending { it.createdAt }.map { VChat.from(it) }
        }
    }

    private fun insertMessages(items: List<ChatItem>, scroll: Boolean) {
        val previous = _itemsFlow.value
        val newItems = items.filterNot { incoming -> previous.any { it.id == incoming.id } }
        if (newItems.isEmpty()) return
        val rows = newItems.map(VChat::from)
        _itemsFlow.update { rows + it }
        if (scroll) _scrollToLatest.trySend(previous.firstOrNull()?.id)
    }

    fun update(item: ChatItem) {
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
            client.clearAllMessages(target)
            _itemsFlow.value = emptyList()
        }
    }

    fun resendMessage(messageId: String) {
        launchSafe {
            update(client.retry(messageId))
        }
    }

    fun resendToMembers(messageId: String, peerIds: List<String>) {
        launchSafe {
            update(client.retry(messageId, peerIds))
        }
    }

    suspend fun forwardMessage(messageId: String, targets: List<ChatTarget>, note: String?) {
        val items = client.forward(messageId, targets, note)
        check(items.isNotEmpty()) { "Empty forwarding response" }
        if (items.all { it.status == ChatStatus.SENT }) {
            DialogHelper.showSuccess(Res.string.sent)
        } else if (items.any { it.status == ChatStatus.FAILED || it.status == ChatStatus.PARTIAL }) {
            DialogHelper.showMessage(Res.string.send_failed)
        }
    }

    fun delete(ids: Set<String>) {
        launchSafe {
            client.deleteByIds(ids)
            _itemsFlow.update { it.filterNot { m -> ids.contains(m.id) } }
        }
    }

    internal suspend fun sendMessage(
        target: ChatTarget,
        content: MessageContent,
        send: suspend (ChatTarget, MessageContent) -> ChatItem = client::sendContent,
    ): Boolean {
        val item = send(target, content)
        mutex.withLock {
            if (_target.value == target) {
                if (_itemsFlow.value.any { it.id == item.id }) update(item)
                else insertMessages(listOf(item), scroll = true)
            }
        }
        return item.status == ChatStatus.SENT
    }

    suspend fun onMessagesCreated(target: ChatTarget, items: List<ChatItem>, scroll: Boolean = false) = mutex.withLock {
        if (_target.value == target) {
            insertMessages(items, scroll)
        }
    }

    internal suspend fun refreshLoaded(
        read: suspend (List<String>) -> Map<String, ChatItem?> = { readChatSnapshot(it, source.api) },
    ) {
        val previous = _itemsFlow.value
        if (previous.isEmpty()) return
        val target = _target.value
        val updates = read(previous.map { it.id })
        val before = previous.associateBy { it.id }
        mutex.withLock {
            if (_target.value != target) return
            _itemsFlow.update { current ->
                current.mapNotNull { item ->
                    if (before[item.id] !== item || !updates.containsKey(item.id)) item
                    else updates[item.id]?.let { VChat.from(it) }
                }
            }
        }
    }

    suspend fun onMessageUpdated(id: String) {
        val chat = client.getChatItem(id) ?: return
        mutex.withLock {
            update(chat)
        }
    }

    suspend fun onConversationCleared(encodedTarget: String) = mutex.withLock {
        if (_target.value.encodedToId == encodedTarget) _itemsFlow.value = emptyList()
    }

    suspend fun onMessagesDeleted(ids: Set<String>) = mutex.withLock {
        removeIds(ids)
    }

}
