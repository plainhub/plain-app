package com.ismartcoding.plain.features.dlna

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@JsonClassDiscriminator("action")
@Serializable
internal sealed class ReceiverCommand {
    @Serializable @SerialName("rules") data object Rules : ReceiverCommand()
    @Serializable @SerialName("removeRule") data class RemoveRule(val ip: String, val allowed: Boolean) : ReceiverCommand()
    @Serializable @SerialName("snapshot") data object Snapshot : ReceiverCommand()
    @Serializable @SerialName("start") data class Start(val port: Int) : ReceiverCommand()
    @Serializable @SerialName("stop") data object Stop : ReceiverCommand()
    @Serializable @SerialName("retry") data class Retry(val port: Int) : ReceiverCommand()
    @Serializable @SerialName("accept") data class Accept(val remember: Boolean) : ReceiverCommand()
    @Serializable @SerialName("reject") data class Reject(val remember: Boolean) : ReceiverCommand()
    @Serializable @SerialName("position") data class Position(val positionMs: Long, val durationMs: Long) : ReceiverCommand()
    @Serializable @SerialName("seekConsumed") data object SeekConsumed : ReceiverCommand()
    @Serializable @SerialName("playback") data class Playback(val state: DlnaPlaybackState) : ReceiverCommand()
    @Serializable @SerialName("stopped") data object Stopped : ReceiverCommand()
}
