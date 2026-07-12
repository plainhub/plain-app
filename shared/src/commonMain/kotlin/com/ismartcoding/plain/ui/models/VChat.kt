package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.db.DChat
import com.ismartcoding.plain.db.DMessageStatusData
import kotlin.time.Instant

data class VChat(
    override var id: String,
    val fromId: String,
    val createdAt: Instant,
    val type: String,
    val status: String, // pending, sent, partial, failed
    val statusData: DMessageStatusData? = null,
    var value: Any? = null,
) : IData {
    companion object {
        fun from(data: DChat, fromName: String = ""): VChat {
            return VChat(data.id, data.fromId, data.createdAt, data.content.type, data.status, data.parseStatusData(), data.content.value)
        }
    }
}
