package com.ismartcoding.plain.web.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
class ID(val value:String) {
    override fun toString():String {
        return value
    }

    @Serializer(forClass = ID::class)
    companion object {
        override fun serialize(encoder: Encoder, value: ID) {
            encoder.encodeString(value.value)
        }

        override fun deserialize(decoder: Decoder): ID {
            return ID(decoder.decodeString())
        }
    }
}