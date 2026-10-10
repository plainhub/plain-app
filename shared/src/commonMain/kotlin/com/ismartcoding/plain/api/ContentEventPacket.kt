package com.ismartcoding.plain.api

import kotlinx.serialization.Serializable

@Serializable
internal data class ContentEventPacket(
    val type: String,
    val payload: String,
    val hostCapabilities: HostEventCapabilities? = null,
)
