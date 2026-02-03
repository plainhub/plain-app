package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.chat.PeerChatHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.db.DChatGroup
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.features.ChatHelper
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.web.models.toModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray

data class ChatState(
    val toId: String = "local",
    val toName: String = "",
    val peer: DPeer? = null,
    val group: DChatGroup? = null
)

class ChatViewModel : ISelectableViewModel<VChat>, ViewModel() {
    private val _itemsFlow = MutableStateFlow(mutableStateListOf<VChat>())
    override val itemsFlow: StateFlow<List<VChat>> get() = _itemsFlow
    val selectedItem = mutableStateOf<VChat?>(null)
    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> get() = _chatState

    suspend fun initializeChatStateAsync(chatId: String) {
        var toId = ""
        var peer: DPeer? = null
        var group: DChatGroup? = null
        var toName = ""

        when {
            chatId.startsWith("peer:") -> {
                toId = chatId.removePrefix("peer:")
                peer = AppDatabase.instance.peerDao().getById(toId)
                toName = peer?.name ?: ""
            }

            chatId.startsWith("group:") -> {
                toId = chatId.removePrefix("group:")
                group = AppDatabase.instance.chatGroupDao().getById(toId)
                group?.name ?: ""
            }

            else -> {
                toId = "local"
                toName = getString(R.string.local_chat)
            }
        }

        _chatState.value = _chatState.value.copy(
            toId = toId,
            toName = toName,
            peer = peer,
            group = group
        )
    }

    suspend fun fetchAsync(toId: String) {
        val dao = AppDatabase.instance.chatDao()
        val list = dao.getByChatId(toId)
        _itemsFlow.value = list.sortedByDescending { it.createdAt }.map {
            VChat.from(it)
        }.toMutableStateList()
    }

    fun sendMessage(content: DMessageContent, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _chatState.value

            // Insert message with appropriate initial status
            val item = ChatHelper.sendAsync(
                message = content,
                fromId = "me",
                toId = state.toId,
                peer = state.peer
            )

            addAll(listOf(item))

            val model = item.toModel().apply { data = getContentData() }
            sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(model))))

            // Handle link previews for text messages
            if (item.content.type == DMessageType.TEXT.value) {
                sendEvent(FetchLinkPreviewsEvent(item))
            }

            // Send to peer if needed and update status
            if (state.peer != null) {
                val success = PeerChatHelper.sendToPeerAsync(state.peer, content)
                updateMessageStatus(item, success)
                onResult(success)
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

    fun sendFiles(files: List<DMessageFile>, isImageVideo: Boolean, onResult: (Boolean) -> Unit = {}) {
        val content = if (isImageVideo) {
            DMessageContent(DMessageType.IMAGES.value, DMessageImages(files))
        } else {
            DMessageContent(DMessageType.FILES.value, DMessageFiles(files))
        }
        sendMessage(content, onResult)
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

    private suspend fun updateMessageStatus(item: DChat, success: Boolean) {
        val newStatus = if (success) "sent" else "failed"
        ChatHelper.updateStatusAsync(item.id, newStatus)
        item.status = newStatus
        update(item)
    }

    fun retryMessage(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _chatState.value
            val item = ChatHelper.getAsync(messageId) ?: return@launch

            if (state.peer != null) {
                ChatHelper.updateStatusAsync(messageId, "pending")
                item.status = "pending"
                update(item)

                val success = PeerChatHelper.sendToPeerAsync(state.peer, item.content)
                updateMessageStatus(item, success)
            }
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
                ChatHelper.deleteAsync(context, m.id, m.value)
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

    fun forwardMessage(messageId: String, targetPeer: DPeer, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = ChatHelper.getAsync(messageId) ?: return@launch
            val newItem = ChatHelper.sendAsync(
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

            val success = PeerChatHelper.sendToPeerAsync(targetPeer, newItem.content)
            updateMessageStatus(newItem, success)
            onResult(success)
        }
    }

    fun forwardMessageToLocal(messageId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = ChatHelper.getAsync(messageId) ?: return@launch
            val newItem = ChatHelper.sendAsync(
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
