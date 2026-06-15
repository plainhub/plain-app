package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.chat.ChatDbHelper
import com.ismartcoding.plain.chat.ChatSender
import com.ismartcoding.plain.chat.data.ChatTargetType
import com.ismartcoding.plain.db.AppDatabase
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageImages
import com.ismartcoding.plain.db.DMessageText
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.events.HMessageUpdatedEvent
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun ChatViewModel.sendMessage(content: DMessageContent, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch(Dispatchers.IO) {
        val state = chatState.value

        if (state.target.type == ChatTargetType.PEER) {
            val peer = AppDatabase.instance.peerDao().getById(state.target.toId)
            if (peer == null || peer.status != "paired") {
                onResult(false)
                return@launch
            }
        }

        val item = ChatSender.createChatItem(state.target, content)
        addAll(listOf(item))

        if (state.target.type != ChatTargetType.LOCAL) {
            ChatSender.send(item, state.target, onlinePeerIds.value)
            update(item)
            onResult(item.status == "sent")
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
    val item = ChatDbHelper.insertChatItem(
        message = content,
        fromId = "me",
        toId = if (state.target.type == ChatTargetType.CHANNEL) "" else state.target.toId,
        channelId = if (state.target.type == ChatTargetType.CHANNEL) state.target.toId else "",
        isRemote = state.target.type != ChatTargetType.LOCAL,
    )
    addAll(listOf(item))
    return item.id
}

fun ChatViewModel.updateFilesMessage(messageId: String, files: List<DMessageFile>, isImageVideo: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
        val state = chatState.value
        val item = ChatDbHelper.getChatItem(messageId) ?: return@launch
        ChatDbHelper.updateChatItemFilesContent(item, files)
        if (state.target.type == ChatTargetType.LOCAL) {
            ChatDbHelper.updateChatItemStatus(item, "sent")
        } else {
            ChatDbHelper.updateChatItemStatus(item, "pending")
            ChatSender.send(item, state.target, onlinePeerIds.value)
        }
        sendEvent(HMessageUpdatedEvent(item.id))
        update(item)
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
