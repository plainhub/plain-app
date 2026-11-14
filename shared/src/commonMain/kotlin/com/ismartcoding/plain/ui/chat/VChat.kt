package com.ismartcoding.plain.ui.chat

import com.ismartcoding.plain.features.chat.models.MessageStatusData
import com.ismartcoding.plain.features.chat.models.MessageType
import com.ismartcoding.plain.IData
import com.ismartcoding.plain.features.chat.ChatStatus
import com.ismartcoding.plain.models.ChatItem
import kotlin.time.Instant

data class VChat(
    override var id: String,
    val fromId: String,
    val createdAt: Instant,
    val type: MessageType,
    val status: ChatStatus,
    val statusData: MessageStatusData? = null,
    val value: Any? = null,
) : IData {
    companion object {
        fun from(data: ChatItem): VChat {
            val content = data.messageContent
            return VChat(data.id, data.fromId, data.createdAt, content.type, data.status, data.deliveryStatus(), content.value)
        }
    }
}
