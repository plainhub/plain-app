package com.ismartcoding.plain.chat.peer

import kotlinx.serialization.Serializable

@Serializable
internal data class PeerStatusRequest(val action: String)
