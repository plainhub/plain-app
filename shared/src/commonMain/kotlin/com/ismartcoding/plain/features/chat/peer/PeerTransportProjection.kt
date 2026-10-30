package com.ismartcoding.plain.features.chat.peer

import kotlinx.serialization.Serializable

import com.ismartcoding.plain.ui.chat.ChatViewModel
import com.ismartcoding.plain.features.chat.peer.transport.PeerTransportType

object PeerTransportProjection {
    internal fun apply(active: List<PeerTransportState>) {
        ChatViewModel.local.currentTransportMap.value = active.associate { row ->
            row.peerId to PeerTransportType.valueOf(row.transport)
        }
    }
}

@Serializable
internal data class PeerTransportState(val peerId: String, val transport: String)
