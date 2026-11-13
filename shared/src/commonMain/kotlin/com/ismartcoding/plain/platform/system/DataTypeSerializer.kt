package com.ismartcoding.plain.platform.system

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object GraphqlDataTypeSerializer : KSerializer<DataType> {
    override val descriptor = PrimitiveSerialDescriptor("GraphqlDataType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DataType) = encoder.encodeString(value.name)

    override fun deserialize(decoder: Decoder): DataType {
        val value = decoder.decodeString()
        return DataType.entries.firstOrNull { it.name == value }
            ?: throw SerializationException("Unknown GraphQL data type: $value")
    }
}

object DataTypeSerializer : KSerializer<DataType> {
    override val descriptor = PrimitiveSerialDescriptor("DataType", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: DataType) = encoder.encodeInt(value.value)

    override fun deserialize(decoder: Decoder): DataType {
        val value = decoder.decodeInt()
        return DataType.entries.firstOrNull { it.value == value }
            ?: throw SerializationException("Unknown data type: $value")
    }
}
