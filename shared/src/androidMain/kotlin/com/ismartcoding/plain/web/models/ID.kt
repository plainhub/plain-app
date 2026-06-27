package com.ismartcoding.plain.web.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(ID.Companion::class)
class ID(val value: String) {
    override fun toString(): String {
        return value
    }

    companion object : KSerializer<ID> {
        override fun serialize(
            encoder: Encoder,
            value: ID,
        ) {
            encoder.encodeString(value.value)
        }

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ID", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): ID {
            return ID(decoder.decodeString())
        }
    }
}
