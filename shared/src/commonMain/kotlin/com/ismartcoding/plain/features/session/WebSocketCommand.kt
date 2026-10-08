package com.ismartcoding.plain.features.session

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@kotlinx.serialization.json.JsonClassDiscriminator("action")
@Serializable
internal sealed class WebSocketCommand {
    @Serializable @SerialName("snapshot") data object Snapshot : WebSocketCommand()
    @Serializable @SerialName("closeAll") data object CloseAll : WebSocketCommand()
}
