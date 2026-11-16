package com.ismartcoding.plain.features.chat.models

import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

object MessageContentSerializer : KSerializer<MessageContent> {
    override val descriptor: SerialDescriptor = MessageEnvelope.serializer().descriptor

    override fun serialize(encoder: Encoder, value: MessageContent) {
        val payload = value.value?.let {
            when (value.type) {
                MessageType.MESSAGE -> JsonHelper.jsonEncodeToElement(it as ChatMessage)
                MessageType.SHARE -> JsonHelper.jsonEncodeToElement(it as MessageShare)
            }
        } ?: JsonObject(emptyMap())
        encoder.encodeSerializableValue(MessageEnvelope.serializer(), MessageEnvelope(value.type, payload))
    }

    override fun deserialize(decoder: Decoder): MessageContent {
        val envelope = decoder.decodeSerializableValue(MessageEnvelope.serializer())
        val value = when (envelope.type) {
            MessageType.MESSAGE -> JsonHelper.jsonDecodeFromElement<ChatMessage>(envelope.value)
            MessageType.SHARE -> JsonHelper.jsonDecodeFromElement<MessageShare>(envelope.value)
        }
        return MessageContent(envelope.type, value)
    }
}
