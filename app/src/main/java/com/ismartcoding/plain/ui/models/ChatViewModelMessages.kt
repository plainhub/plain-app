package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.JsonHelper
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.FetchLinkPreviewsEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.TimeHelper
import com.ismartcoding.plain.web.models.toModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun ChatViewModel.sendMessage(content: DMessageContent, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch(Dispatchers.IO) {
        val state = chatState.value

        if (state.chatType == ChatType.PEER) {
            val peer = AppDatabase.instance.peerDao().getById(state.toId)
            if (peer == null || peer.status != "paired") {
                onResult(false)
                return@launch
            }
        }

        val item = ChatDbHelper.sendAsync(
            message = content, fromId = "me",
            toId = if (state.chatType == ChatType.CHANNEL) "" else state.toId,
            channelId = if (state.chatType == ChatType.CHANNEL) state.toId else "",
            peer = if (state.chatType == ChatType.PEER) AppDatabase.instance.peerDao().getById(state.toId) else null,
            isRemote = state.isRemote()
        )
        addAll(listOf(item))
        val model = item.toModel().apply { data = getContentData() }
        sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(model))))
        if (item.content.type == DMessageType.TEXT.value) sendEvent(FetchLinkPreviewsEvent(item))

        if (state.isRemote()) {
            val outcome = deliverToRemoteAsync(state, content)
            applyDeliveryOutcome(item, outcome)
            update(item)
            onResult(outcome.success)
        } else {
            onResult(true)
        }
    }
}

fun ChatViewModel.sendTextMessage(text: String, context: Context, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch(Dispatchers.IO) {
        val content = if (text.length > Constants.MAX_MESSAGE_LENGTH) {
            createLongTextFile(text, context)
        } else {
            DMessageContent(DMessageType.TEXT.value, DMessageText(text))
        }
        sendMessage(content, onResult)
    }
}

suspend fun ChatViewModel.sendFilesImmediate(files: List<DMessageFile>, isImageVideo: Boolean): String {
    val state = chatState.value
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

fun ChatViewModel.updateFilesMessage(messageId: String, files: List<DMessageFile>, isImageVideo: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
        val state = chatState.value
        val content = if (isImageVideo) {
            DMessageContent(DMessageType.IMAGES.value, DMessageImages(files))
        } else {
            DMessageContent(DMessageType.FILES.value, DMessageFiles(files))
        }
        val newStatus = if (state.isRemote()) "pending" else "sent"
        val dao = AppDatabase.instance.chatDao()
        dao.getById(messageId)?.let { item ->
            item.content = content
            item.status = newStatus
            dao.update(item)
            val model = item.toModel().apply { data = getContentData() }
            sendEvent(WebSocketEvent(EventType.MESSAGE_CREATED, JsonHelper.jsonEncode(listOf(model))))
            if (state.isRemote()) {
                val outcome = deliverToRemoteAsync(state, content)
                applyDeliveryOutcome(item, outcome)
            }
            update(item)
        }
    }
}

internal fun createLongTextFile(text: String, context: Context): DMessageContent {
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
