package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.chat.models.*
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ChatContentInput(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val text: MessageText? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val images: MessageImages? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val files: MessageFiles? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val share: MessageShare? = null,
) {
    init {
        require(listOf(text, images, files, share).count { it != null } == 1)
    }
    companion object {
        fun from(content: MessageContent): ChatContentInput = when (content.type) {
            MessageType.TEXT -> ChatContentInput(text = content.value as MessageText)
            MessageType.IMAGES -> ChatContentInput(images = content.value as MessageImages)
            MessageType.FILES -> ChatContentInput(files = content.value as MessageFiles)
            MessageType.SHARE -> ChatContentInput(share = content.value as MessageShare)
        }
    }
}
