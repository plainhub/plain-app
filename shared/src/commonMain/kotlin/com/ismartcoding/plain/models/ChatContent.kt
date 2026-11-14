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
    data class Text(val text: String, val linkPreviews: List<LinkPreview>) : ChatContent() {
        @SerialName("__typename") val typename = "ChatTextContent"
        override fun message() = MessageContent(MessageType.TEXT, MessageText(text, linkPreviews))
    }
    @Serializable
    data class Images(val items: List<MessageFile>) : ChatContent() {
        @SerialName("__typename") val typename = "ChatImagesContent"
        override fun message() = MessageContent(MessageType.IMAGES, MessageImages(items))
    }
    @Serializable
    data class Files(val items: List<MessageFile>) : ChatContent() {
        @SerialName("__typename") val typename = "ChatFilesContent"
        override fun message() = MessageContent(MessageType.FILES, MessageFiles(items))
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
            "ChatTextContent" -> ChatContent.Text.serializer()
            "ChatImagesContent" -> ChatContent.Images.serializer()
            "ChatFilesContent" -> ChatContent.Files.serializer()
            "ChatShareContent" -> ChatContent.Share.serializer()
            else -> throw SerializationException("Unknown chat content type")
        }
}
