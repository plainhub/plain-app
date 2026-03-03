package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.ChannelChatHelper
import com.ismartcoding.plain.chat.PeerChatHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.db.DChatChannel
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageDeliveryResult
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.chat.discover.NearbyDiscoverManager
import com.ismartcoding.plain.events.ChannelUpdatedEvent
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.PeerUpdatedEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.chat.ChatCacheManager
import com.ismartcoding.plain.web.models.toModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

enum class ChatType { LOCAL, PEER, CHANNEL }

data class ChatState(
    val toId: String = "",
    val toName: String = "",
    val chatType: ChatType = ChatType.LOCAL,
    /** Set of peer ids known to be online; used for leader election. */
    val onlinePeerIds: Set<String> = emptySet(),
)

class ChatViewModel : ISelectableViewModel<VChat>, ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<VChat>())
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
                        // Keep cache entry up-to-date for this peer
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
                        _chatState.update { state ->
                            state.copy(toName = updated?.name ?: state.toName)
                        }
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
                _chatState.value = _chatState.value.copy(
                    toId = toId,
                    toName = peer?.name ?: "",
                    chatType = ChatType.PEER,
                )
            }

            chatId.startsWith("channel:") -> {
                val toId = chatId.removePrefix("channel:")
                val channel = AppDatabase.instance.chatChannelDao().getById(toId)
                _chatState.value = _chatState.value.copy(
                    toId = toId,
                    toName = channel?.name ?: "",
                    chatType = ChatType.CHANNEL,
                )
            }

            else -> {
                _chatState.value = _chatState.value.copy(
                    toId = "local",
                    toName = getString(R.string.local_chat),
                    chatType = ChatType.LOCAL,
                )
            }
        }
    }

    suspend fun fetchAsync(toId: String) {
        val state = _chatState.value
        val dao = AppDatabase.instance.chatDao()
        val isChannel = state.chatType == ChatType.CHANNEL
        val list = if (isChannel) {
            dao.getByChannelId(state.toId)
        } else {
            dao.getByChatId(toId)
        }
        _itemsFlow.value = list.sortedByDescending { it.createdAt }.map { chat ->
            val fromName = if (isChannel && chat.fromId != "me") {
                // Look up sender name from peers table (channel members no longer carry names)
                AppDatabase.instance.peerDao().getById(chat.fromId)?.name ?: ""
            } else ""
            VChat.from(chat, fromName)
        }.toMutableStateList()
    }

    fun sendMessage(content: DMessageContent, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _chatState.value

            // Block sending to unpaired peers
            if (state.chatType == ChatType.PEER) {
                val peer = AppDatabase.instance.peerDao().getById(state.toId)
                if (peer == null || peer.status != "paired") {
                    onResult(false)
                    return@launch
                }
            }

            // Insert message with appropriate initial status
            val item = ChatDbHelper.sendAsync(
                message = content,
                fromId = "me",
                toId = if (state.chatType == ChatType.CHANNEL) "" else state.toId,
                channelId = if (state.chatType == ChatType.CHANNEL) state.toId else "",
                peer = if (state.chatType == ChatType.PEER) AppDatabase.instance.peerDao().getById(state.toId) else null,
                isRemote = state.isRemote()
            )

            addAll(listOf(item))

            val model = item.toModel().apply { data = getContentData() }
            sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(model))))

            // Handle link previews for text messages
            if (item.content.type == DMessageType.TEXT.value) {
                sendEvent(FetchLinkPreviewsEvent(item))
            }

            // Send to remote target(s) if needed and update status
            if (state.isRemote()) {
                val outcome = deliverToRemoteAsync(state, content)
                updateMessageStatus(item, outcome)
                onResult(outcome.success)
            } else {
                onResult(true)
            }
        }
    }

    fun sendTextMessage(text: String, context: Context, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = if (text.length > Constants.MAX_MESSAGE_LENGTH) {
                createLongTextFile(text, context)
            } else {
                DMessageContent(DMessageType.TEXT.value, DMessageText(text))
            }
            sendMessage(content, onResult)
        }
    }

    /**
     * Insert a placeholder message immediately with [status] = "pending" so
     * the UI renders thumbnail previews right away. Returns the inserted [DChat]
     * id; call [updateFilesMessage] once real fid: URIs are ready.
     */
    suspend fun sendFilesImmediate(files: List<DMessageFile>, isImageVideo: Boolean): String {
        val state = _chatState.value
        val content = if (isImageVideo) {
            DMessageContent(DMessageType.IMAGES.value, DMessageImages(files))
        } else {
            DMessageContent(DMessageType.FILES.value, DMessageFiles(files))
        }
        val item = AppDatabase.instance.chatDao().let { dao ->
            val chat = DChat()
            chat.fromId = "me"
            chat.toId = if (state.chatType == ChatType.CHANNEL) "" else state.toId
            chat.channelId = if (state.chatType == ChatType.CHANNEL) state.toId else ""
            chat.content = content
            chat.status = "pending"
            dao.insert(chat)
            chat
        }
        addAll(listOf(item))
        return item.id
    }

    /**
     * Replace the placeholder message's content with fully-imported [DMessageFile]s
     * and transition the status to "sent" / "pending".
     */
    fun updateFilesMessage(messageId: String, files: List<DMessageFile>, isImageVideo: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _chatState.value
            val content = if (isImageVideo) {
                DMessageContent(DMessageType.IMAGES.value, DMessageImages(files))
            } else {
                DMessageContent(DMessageType.FILES.value, DMessageFiles(files))
            }
            val newStatus = if (state.isRemote()) "pending" else "sent"
            val dao = com.ismartcoding.plain.db.AppDatabase.instance.chatDao()
            dao.getById(messageId)?.let { item ->
                item.content = content
                item.status = newStatus
                dao.update(item)

                val model = item.toModel().apply { data = getContentData() }
                sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(model))))

                if (state.isRemote()) {
                    val outcome = deliverToRemoteAsync(state, content)
                    updateMessageStatus(item, outcome)
                }
                update(item)
            }
        }
    }

    private fun createLongTextFile(text: String, context: Context): DMessageContent {
        val timestamp = TimeHelper.now().toEpochMilliseconds()
        val fileName = "message-$timestamp.txt"
        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
        if (!dir!!.exists()) {
            dir.mkdirs()
        }
        val file = java.io.File(dir, fileName)
        file.writeText(text)

        val summary = text.substring(0, minOf(text.length, com.ismartcoding.plain.Constants.TEXT_FILE_SUMMARY_LENGTH))

        val messageFile = DMessageFile(
            uri = file.absolutePath,
            size = file.length(),
            summary = summary,
            fileName = fileName
        )

        return DMessageContent(DMessageType.FILES.value, DMessageFiles(listOf(messageFile)))
    }

    /** Whether the current chat targets a remote destination (peer or channel). */
    private fun ChatState.isRemote(): Boolean = chatType != ChatType.LOCAL

    /**
     * Wraps the outcome of a remote delivery attempt.
     * For peer chat: [success] reflects send result, [statusData] is null.
     * For channel: [statusData] carries per-member results; [success] is derived.
     */
    private data class DeliveryOutcome(
        val success: Boolean,
        val statusData: DMessageStatusData? = null,
    )

    /**
     * Deliver [content] to the appropriate remote target(s) based on [state].
     * For channels, uses star topology: send to leader or broadcast if we are the leader.
     * Returns [DeliveryOutcome] summarising success and per-member data.
     */
    private suspend fun deliverToRemoteAsync(state: ChatState, content: DMessageContent): DeliveryOutcome {
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
                    channel = channel,
                    content = content,
                    onlinePeerIds = state.onlinePeerIds,
                )
                if (statusData == null) {
                    // No leader available — trigger rediscovery
                    val leaderId = channel.electLeader(state.onlinePeerIds)
                    if (leaderId != null && leaderId != TempData.clientId) {
                        triggerPeerRediscovery(leaderId)
                    } else {
                        ChannelChatHelper.getRecipientIds(channel).forEach { memberId ->
                            triggerPeerRediscovery(memberId)
                        }
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

    private fun triggerPeerRediscovery(peerId: String) {
        val key = ChatCacheManager.peerKeyCache[peerId]
        if (key != null) {
            NearbyDiscoverManager.discoverSpecificDevice(peerId, key)
        }
    }

    private suspend fun updateMessageStatus(item: DChat, outcome: DeliveryOutcome) {
        if (outcome.statusData != null) {
            // Channel message with per-member data
            ChatDbHelper.updateStatusAndDataAsync(item.id, outcome.statusData)
            val newStatus = when {
                outcome.statusData.total == 0 -> "sent"
                outcome.statusData.allDelivered -> "sent"
                outcome.statusData.allFailed -> "failed"
                else -> "partial"
            }
            item.status = newStatus
            item.statusData = if (outcome.statusData.total > 0)
                JsonHelper.jsonEncode(outcome.statusData)
            else ""
        } else {
            if (outcome.success) {
                // Clear any stale failure statusData so the error badge disappears
                ChatDbHelper.updateStatusAndDataAsync(item.id, DMessageStatusData())
                item.statusData = ""
                item.status = "sent"
            } else {
                ChatDbHelper.updateStatusAsync(item.id, "failed")
                item.status = "failed"
            }
        }
        update(item)
    }

    fun retryMessage(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _chatState.value
            val item = ChatDbHelper.getAsync(messageId) ?: return@launch

            if (state.isRemote()) {
                ChatDbHelper.updateStatusAsync(messageId, "pending")
                item.status = "pending"
                update(item)
                val outcome = deliverToRemoteAsync(state, item.content)
                updateMessageStatus(item, outcome)
            }
        }
    }

    /**
     * Resend a channel message to specific [peerIds] that did not receive it.
     * Merges new delivery results with existing ones from [statusData].
     */
    fun resendToMembers(messageId: String, peerIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _chatState.value
            if (state.chatType != ChatType.CHANNEL) return@launch
            val channel = AppDatabase.instance.chatChannelDao().getById(state.toId) ?: return@launch
            val item = ChatDbHelper.getAsync(messageId) ?: return@launch

            // Mark as pending to show progress
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
                val result = ChannelChatHelper.sendToMemberAsync(channel, peer, item.content)
                newResults.add(result)
            }

            // Merge: keep previously successful results, replace retried ones
            val existing = item.parseStatusData()?.results ?: emptyList()
            val retriedIds = peerIds.toSet()
            val merged = existing.filter { it.peerId !in retriedIds } + newResults
            val mergedStatusData = DMessageStatusData(merged)

            ChatDbHelper.updateStatusAndDataAsync(item.id, mergedStatusData)
            val newStatus = when {
                mergedStatusData.total == 0 -> "sent"
                mergedStatusData.allDelivered -> "sent"
                mergedStatusData.allFailed -> "failed"
                else -> "partial"
            }
            item.status = newStatus
            item.statusData = if (mergedStatusData.total > 0)
                JsonHelper.jsonEncode(mergedStatusData)
            else ""
            update(item)
        }
    }

    fun remove(id: String) {
        _itemsFlow.value.removeIf { it.id == id }
    }

    fun addAll(items: List<DChat>) {
        _itemsFlow.value.addAll(0, items.map { VChat.from(it) })
    }

    fun update(item: DChat) {
        _itemsFlow.update { currentList ->
            val mutableList = currentList.toMutableStateList()
            val index = mutableList.indexOfFirst { it.id == item.id }
            if (index >= 0) {
                mutableList[index] = VChat.from(item)
            }
            mutableList
        }
    }

    fun delete(context: Context, ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = JSONArray()
            val items = _itemsFlow.value.filter { ids.contains(it.id) }
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

    fun clearAllMessages(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _chatState.value
            if (state.chatType == ChatType.CHANNEL) {
                ChatDbHelper.deleteAllChannelChatsAsync(context, state.toId)
            } else {
                ChatDbHelper.deleteAllChatsAsync(context, state.toId)
            }
            _itemsFlow.value = mutableStateListOf()
            sendEvent(WebSocketEvent(EventType.MESSAGE_CLEARED, JsonHelper.jsonEncode(state.toId)))
        }
    }

    fun forwardMessage(messageId: String, targetPeer: DPeer, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = ChatDbHelper.getAsync(messageId) ?: return@launch
            val newItem = ChatDbHelper.sendAsync(
                message = item.content,
                fromId = "me",
                toId = targetPeer.id,
                peer = targetPeer
            )

            val model = newItem.toModel().apply { data = getContentData() }
            sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(model))))

            // Handle link previews for text messages
            if (newItem.content.type == DMessageType.TEXT.value) {
                sendEvent(FetchLinkPreviewsEvent(newItem))
            }

            val error = PeerChatHelper.sendToPeerAsync(targetPeer, newItem.content)
            val outcome = if (error != null) {
                triggerPeerRediscovery(targetPeer.id)
                DeliveryOutcome(
                    false,
                    DMessageStatusData(listOf(DMessageDeliveryResult(targetPeer.id, targetPeer.name, error)))
                )
            } else {
                DeliveryOutcome(true)
            }
            updateMessageStatus(newItem, outcome)
            onResult(error == null)
        }
    }

    fun forwardMessageToLocal(messageId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = ChatDbHelper.getAsync(messageId) ?: return@launch
            val newItem = ChatDbHelper.sendAsync(
                message = item.content,
                fromId = "me",
                toId = "local",
                peer = null
            )

            val model = newItem.toModel().apply { data = getContentData() }
            sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(model))))

            // Handle link previews for text messages
            if (newItem.content.type == DMessageType.TEXT.value) {
                sendEvent(FetchLinkPreviewsEvent(newItem))
            }

            onResult(true)
        }
    }
}
