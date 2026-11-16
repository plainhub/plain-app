package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.chat.models.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

@Serializable(with = ChatContentSerializer::class)
sealed class ChatContent {
    abstract fun message(): MessageContent

    @Serializable
    data class Message(
        val text: String?,
        val format: ChatTextFormat,
        val items: List<ChatMessageAttachment>,
        val linkPreviews: List<LinkPreview>,
    ) : ChatContent() {
        @SerialName("__typename") val typename = "ChatMessageContent"
        override fun message() = MessageContent(MessageType.MESSAGE, ChatMessage(text, format, items, linkPreviews))
    }
    @Serializable
    data class Share(
        val shareId: String,
        val urlToken: String,
        val peerInfo: SharePeerInfo,
        val name: String,
        val itemCount: Long,
        val totalSize: Long,
        val expiresAt: kotlin.time.Instant?,
    ) : ChatContent() {
        @SerialName("__typename") val typename = "ChatShareContent"
        override fun message() = MessageContent(MessageType.SHARE, MessageShare(shareId, urlToken, peerInfo, name, itemCount, totalSize, expiresAt))
    }
}

object ChatContentSerializer : JsonContentPolymorphicSerializer<ChatContent>(ChatContent::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ChatContent> =
        when (element.jsonObject.getValue("__typename").jsonPrimitive.content) {
            "ChatMessageContent" -> ChatContent.Message.serializer()
            "ChatShareContent" -> ChatContent.Share.serializer()
            else -> throw SerializationException("Unknown chat content type")
        }
}
