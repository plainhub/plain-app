package com.ismartcoding.plain.api

import kotlinx.serialization.Serializable

@Serializable
internal data class HostEventPacket(val type: String, val payload: String)
