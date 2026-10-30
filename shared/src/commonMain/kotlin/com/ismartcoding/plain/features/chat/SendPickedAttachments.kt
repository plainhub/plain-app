package com.ismartcoding.plain.features.chat

import com.ismartcoding.plain.ui.platform.files.PickedAttachments

import com.ismartcoding.plain.features.chat.ChatClient
import com.ismartcoding.plain.ui.chat.ChatViewModel
import com.ismartcoding.plain.features.chat.data.ChatTarget
import com.ismartcoding.plain.features.chat.models.MessageContent
import com.ismartcoding.plain.features.chat.models.MessageFiles
import com.ismartcoding.plain.features.chat.models.MessageImages
import com.ismartcoding.plain.features.chat.models.MessageType
import com.ismartcoding.plain.features.chat.ChatStatus
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun sendPickedAttachments(
    targets: List<ChatTarget>,
    uris: List<String>,
    text: String? = null,
    caption: String? = null,
    images: Boolean? = null,
    normalize: Boolean = false,
    chatVM: ChatViewModel = ChatViewModel.local,
): Boolean {
    if (targets.isEmpty()) return false
    return PickedAttachments.withFiles(uris, normalize, chatVM.source) { attachments ->
        if (uris.isNotEmpty() && attachments.isEmpty()) false
        else coroutineScope {
            val content = if (attachments.isEmpty()) null else {
                val visual = images ?: attachments.all { it.fileName.isImageFast() || it.fileName.isVideoFast() }
                if (visual) MessageContent(MessageType.IMAGES, MessageImages(attachments))
                else MessageContent(MessageType.FILES, MessageFiles(attachments))
            }
            if (content == null && text.isNullOrBlank()) return@coroutineScope false
            targets.map { target -> async {
                val item = if (content == null) chatVM.client.sendText(target, text!!) else chatVM.client.sendContent(target, content)
                chatVM.onMessagesCreated(target, listOf(item), scroll = true)
                val captionItem = if (content != null && !caption.isNullOrBlank()) chatVM.client.sendText(target, caption) else null
                captionItem?.let { chatVM.onMessagesCreated(target, listOf(it), scroll = true) }
                item.status == ChatStatus.SENT && (captionItem == null || captionItem.status == ChatStatus.SENT)
            } }.awaitAll().all { it }
        }
    }
}
