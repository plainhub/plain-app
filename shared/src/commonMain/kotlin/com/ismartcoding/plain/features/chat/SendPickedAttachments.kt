package com.ismartcoding.plain.features.chat

import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.features.chat.models.ChatAttachmentType
import com.ismartcoding.plain.features.chat.models.ChatMessage
import com.ismartcoding.plain.features.chat.models.ChatMessageAttachment
import com.ismartcoding.plain.features.chat.models.ChatTextFormat
import com.ismartcoding.plain.features.chat.models.MessageContent
import com.ismartcoding.plain.features.chat.models.MessageFile
import com.ismartcoding.plain.features.chat.models.MessageType
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.ui.chat.ChatViewModel
import com.ismartcoding.plain.ui.platform.files.PickedAttachments
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun sendPickedAttachments(
    targets: List<ChatTarget>,
    uris: List<String>,
    text: String? = null,
    caption: String? = null,
    chatVM: ChatViewModel = ChatViewModel.local,
): Boolean {
    if (targets.isEmpty()) return false
    return PickedAttachments.withFiles(uris, { false }, chatVM.source) { attachments ->
        if (uris.isNotEmpty() && attachments.isEmpty()) return@withFiles false
        val items = attachments.map { (_, attachment) ->
            val type = if (attachment.fileName.isImageFast() || attachment.fileName.isVideoFast()) ChatAttachmentType.IMAGE else ChatAttachmentType.FILE
            ChatMessageAttachment(type, attachment)
        }
        val body = caption?.takeIf(String::isNotBlank) ?: text?.takeIf(String::isNotBlank)
        if (items.isEmpty() && body == null) return@withFiles false
        val content = MessageContent(MessageType.MESSAGE, ChatMessage(text = body, items = items))
        coroutineScope {
            targets.distinct().map { target -> async {
                val item = chatVM.client.sendContent(target, content)
                chatVM.onMessagesCreated(target, listOf(item), scroll = true)
                item.status == ChatStatus.SENT
            } }.awaitAll().all { it }
        }
    }
}

internal data class ChatDraftAttachment(val type: ChatAttachmentType, val name: String, val previewImage: Boolean = false)

internal suspend fun sendChatDraft(
    chatVM: ChatViewModel,
    target: ChatTarget,
    text: String,
    format: ChatTextFormat,
    picked: Map<String, ChatDraftAttachment>,
): Boolean {
    return PickedAttachments.withFiles(picked.keys.toList(), { picked.getValue(it).type == ChatAttachmentType.IMAGE }, chatVM.source) { files ->
        val content = chatDraftContent(text, format, picked, files) ?: return@withFiles false
        chatVM.sendMessage(target, content)
    }
}

internal fun chatDraftContent(
    text: String,
    format: ChatTextFormat,
    picked: Map<String, ChatDraftAttachment>,
    files: List<Pair<String, MessageFile>>,
): MessageContent? {
    val body = text.takeIf(String::isNotBlank)
    if (files.size != picked.size || body == null && files.isEmpty()) return null
    val items = files.map { (uri, file) -> ChatMessageAttachment(picked.getValue(uri).type, file) }
    return MessageContent(MessageType.MESSAGE, ChatMessage(body, format, items))
}
