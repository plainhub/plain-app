package com.ismartcoding.plain.features.chat.models

import kotlinx.serialization.Serializable

@Serializable
enum class ChatTextFormat { PLAIN, MARKDOWN }

@Serializable
enum class ChatAttachmentType { IMAGE, FILE }

@Serializable
data class ChatMessageAttachment(val type: ChatAttachmentType, val attachment: MessageFile)

@Serializable
data class ChatMessage(
    val text: String? = null,
    val format: ChatTextFormat = ChatTextFormat.PLAIN,
    val items: List<ChatMessageAttachment> = emptyList(),
    val linkPreviews: List<LinkPreview> = emptyList(),
) {
    val images: List<MessageFile> get() = items.filter { it.type == ChatAttachmentType.IMAGE }.map { it.attachment }
    val files: List<MessageFile> get() = items.filter { it.type == ChatAttachmentType.FILE }.map { it.attachment }
}
