package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.db.IData
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageStatusData
import com.ismartcoding.plain.db.MessageType
import com.ismartcoding.plain.enums.ChatStatus
import kotlin.time.Instant

data class VChat(
    override var id: String,
    val fromId: String,
    val createdAt: Instant,
    val type: MessageType,
    val status: ChatStatus,
    val statusData: DMessageStatusData? = null,
    var value: Any? = null,
) : IData {
    companion object {
        fun from(data: DChat, fromName: String = ""): VChat {
            return VChat(data.id, data.fromId, data.createdAt, data.content.type, data.status, data.parseStatusData(), data.content.value)
        }
    }
}
