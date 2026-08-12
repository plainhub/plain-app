package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.db.DSession
import com.ismartcoding.plain.enums.SessionType
import kotlin.time.Instant

data class VSession(
    val clientId: String,
    val name: String,
    val type: SessionType,
    val token: String,
    val clientIP: String,
    val osName: String,
    val osVersion: String,
    val browserName: String,
    val browserVersion: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastActiveAt: Instant?,
) {
    val isCustom: Boolean
        get() = type == SessionType.CUSTOM

    companion object {
        fun from(data: DSession): VSession {
            return VSession(
                data.clientId,
                data.name,
                data.type,
                data.token,
                data.clientIP,
                data.osName,
                data.osVersion,
                data.browserName,
                data.browserVersion,
                data.createdAt,
                data.updatedAt,
                data.lastActiveAt,
            )
        }
    }
}
