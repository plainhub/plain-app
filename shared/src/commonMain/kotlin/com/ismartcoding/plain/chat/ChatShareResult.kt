package com.ismartcoding.plain.chat

import kotlinx.serialization.Serializable

@Serializable
internal data class ChatShareResult(val ok: Boolean, val warnings: List<String> = emptyList())
