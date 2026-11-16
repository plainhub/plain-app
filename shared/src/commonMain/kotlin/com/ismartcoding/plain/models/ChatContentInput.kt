package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.chat.models.*
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessageInput(
    val text: String?,
    val format: ChatTextFormat,
    val items: List<ChatMessageAttachment>,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatContentInput(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val message: ChatMessageInput? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val share: MessageShare? = null,
) {
    init {
        require((message == null) != (share == null))
    }
    companion object {
        fun from(content: MessageContent): ChatContentInput = when (content.type) {
            MessageType.MESSAGE -> {
                val value = content.value as ChatMessage
                ChatContentInput(message = ChatMessageInput(value.text, value.format, value.items))
            }
            MessageType.SHARE -> ChatContentInput(share = content.value as MessageShare)
        }
    }
}
