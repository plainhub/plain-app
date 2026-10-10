package com.ismartcoding.plain.api

import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketData
import kotlinx.serialization.Serializable

@Serializable
internal data class HostEventCapabilities(
    val textTypes: Set<String>,
    val binaryTypes: Set<String>,
) {
    fun accepts(type: EventType, data: WebSocketData): Boolean = type.name in when (data) {
        is WebSocketData.Text -> textTypes
        is WebSocketData.Binary -> binaryTypes
    }
}
