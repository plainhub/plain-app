package com.ismartcoding.plain.ui.models

import android.content.Context

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.chat.ChatManager
import com.ismartcoding.plain.chat.data.ChatTarget
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.HMessageUpdatedEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.sent
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.web.models.toModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class ChatViewModel : ISelectableViewModel<VChat>, ViewModel() {
    internal val _itemsFlow = MutableStateFlow<List<VChat>>(emptyList())
    override val itemsFlow: StateFlow<List<VChat>> = _itemsFlow
    val selectedItem = mutableStateOf<VChat?>(null)
    override var selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()

    private val _target = MutableStateFlow(ChatTarget("local", ChatTargetType.PEER))
    val target = _target.asStateFlow()

    private val _scrollToLatest = Channel<String?>(Channel.BUFFERED)
    val scrollToLatest: Flow<String?> = _scrollToLatest.receiveAsFlow()

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

    fun clearAllMessages(context: Context) {
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
            ChatManager.updateStatus(item, "pending")
            update(item)
            ChatManager.resendMessage(item)
        }
    }

    fun resendToMembers(messageId: String, peerIds: List<String>) {
        launchSafe {
            val target = target.value
            val channel = AppDatabase.instance.chatChannelDao().getById(target.toId) ?: return@launchSafe
            val item = ChatManager.getChatItem(messageId) ?: return@launchSafe
            ChatManager.updateStatus(item, "pending")
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
            if (_target.value == target) {
                addAllAndScroll(listOf(item2))
            }
            if (item2.status == "sent") {
                DialogHelper.showSuccess(Res.string.sent)
            }
        }
    }

    fun delete(context: Context, ids: Set<String>) {
        launchSafe {
            ChatManager.deleteByIds(ids)
            _itemsFlow.update { it.filterNot { m -> ids.contains(m.id) } }
            sendEvent(WebSocketEvent(EventType.MESSAGE_DELETED, JsonHelper.jsonEncode("ids=${ids.joinToString(",")}")))
        }
    }

    private suspend fun doSendMessage(target: ChatTarget, content: DMessageContent, onlinePeerIds: Set<String>): Boolean = withIO {
        val item = ChatManager.createChatItem(target, content)
        addAllAndScroll(listOf(item))

        if (!target.isLocal()) {
            ChatManager.sendMessage(item, target, onlinePeerIds)
            update(item)
        }
        sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(item.toModel()))))
        item.status == "sent"
    }

    fun sendTextMessage(text: String, context: Context, onlinePeerIds: Set<String>, onResult: (Boolean) -> Unit = {}) {
        launchSafe {
            val content = if (text.length > Constants.MAX_MESSAGE_LENGTH) {
                createLongTextFile(text, context)
            } else {
                DMessageContent(DMessageType.TEXT.value, DMessageText(text))
            }
            onResult(doSendMessage(target.value, content, onlinePeerIds))
        }
    }

    suspend fun sendFilesImmediate(files: List<DMessageFile>, isImageVideo: Boolean): String = withIO {
        val item = ChatManager.insertFilesImmediate(target.value, files, isImageVideo)
        addAllAndScroll(listOf(item))
        item.id
    }

    fun updateFilesMessage(messageId: String, files: List<DMessageFile>, isImageVideo: Boolean, onlinePeerIds: Set<String>) {
        launchSafe {
            val target = target.value
            val item = ChatManager.updateFilesMessage(messageId, files, isImageVideo, target, onlinePeerIds) ?: return@launchSafe
            sendEvent(HMessageUpdatedEvent(item.id))
            update(item)
        }
    }

    fun createLongTextFile(text: String, context: Context): DMessageContent {
        val timestamp = TimeHelper.now().toEpochMilliseconds()
        val fileName = "message-$timestamp.txt"
        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
        if (!dir!!.exists()) dir.mkdirs()
        val file = java.io.File(dir, fileName)
        file.writeText(text)
        val summary = text.substring(0, minOf(text.length, Constants.TEXT_FILE_SUMMARY_LENGTH))
        val messageFile = DMessageFile(uri = file.absolutePath, size = file.length(), summary = summary, fileName = fileName)
        return DMessageContent(DMessageType.FILES.value, DMessageFiles(listOf(messageFile)))
    }
}
