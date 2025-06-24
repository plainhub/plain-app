package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.db.DChat
import kotlinx.datetime.Instant

data class VChat(override var id: String, val fromId: String, val createdAt: Instant, val type: String, var value: Any? = null) : IData {
    companion object {
        fun from(data: DChat): VChat {
            return VChat(data.id, data.fromId, data.createdAt, data.content.type, data.content.value)
        }
    }
}