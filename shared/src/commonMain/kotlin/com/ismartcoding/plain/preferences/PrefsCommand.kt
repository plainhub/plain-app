package com.ismartcoding.plain.preferences

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@kotlinx.serialization.json.JsonClassDiscriminator("action")
@Serializable
internal sealed class PrefsCommand {
    @Serializable @SerialName("snapshot") data class Snapshot(val user: Boolean) : PrefsCommand()
    @Serializable @SerialName("set") data class Set(val user: Boolean, val key: String, val value: JsonElement) : PrefsCommand()
    @Serializable @SerialName("remove") data class Remove(val user: Boolean, val key: String) : PrefsCommand()
}
