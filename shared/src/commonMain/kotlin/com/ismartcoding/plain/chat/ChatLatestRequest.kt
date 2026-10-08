package com.ismartcoding.plain.chat

import kotlinx.serialization.Serializable

@Serializable
internal data class ChatLatestRequest(val action: String = "latestChats")
