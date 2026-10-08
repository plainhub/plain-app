package com.ismartcoding.plain.api

import kotlinx.serialization.Serializable

@Serializable
internal data class ContentEventPacket(val type: Int, val payload: String)
