package com.ismartcoding.plain.chat

import kotlinx.serialization.Serializable

@Serializable
internal data class ChatDeliveryRequest(val id: String, val recipients: List<String>? = null)
