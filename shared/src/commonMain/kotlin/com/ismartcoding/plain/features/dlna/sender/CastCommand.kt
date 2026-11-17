package com.ismartcoding.plain.features.dlna.sender

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("action")
@Serializable
internal sealed class CastCommand {
    @Serializable @SerialName("START_SCAN") data object StartScan : CastCommand()
    @Serializable @SerialName("STOP_SCAN") data object StopScan : CastCommand()
    @Serializable @SerialName("SELECT") data class Select(val id: String) : CastCommand()
    @Serializable @SerialName("CAST") data class Cast(val item: CastItem) : CastCommand()
    @Serializable @SerialName("PLAY") data object Play : CastCommand()
    @Serializable @SerialName("PAUSE") data object Pause : CastCommand()
    @Serializable @SerialName("SEEK") data class Seek(val positionMs: Long) : CastCommand()
    @Serializable @SerialName("STOP") data object Stop : CastCommand()
    @Serializable @SerialName("EXIT") data object Exit : CastCommand()
    @Serializable @SerialName("ADD") data class Add(val item: CastItem) : CastCommand()
    @Serializable @SerialName("REMOVE") data class Remove(val path: String) : CastCommand()
    @Serializable @SerialName("REMOVE_AT") data class RemoveAt(val index: Int) : CastCommand()
    @Serializable @SerialName("REORDER") data class Reorder(val from: Int, val to: Int) : CastCommand()
    @Serializable @SerialName("CLEAR") data object Clear : CastCommand()
}
