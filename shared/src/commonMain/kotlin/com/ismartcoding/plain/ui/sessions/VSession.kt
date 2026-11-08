package com.ismartcoding.plain.ui.sessions

import com.ismartcoding.plain.features.sessions.SessionType
import kotlin.time.Instant

data class VSession(
    val clientId: String,
    val name: String,
    val type: SessionType,
    val token: String,
    val clientIp: String,
    val osName: String,
    val osVersion: String,
    val appName: String,
    val appVersion: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastActiveAt: Instant?,
) {
    val isApiToken: Boolean
        get() = type == SessionType.API_TOKEN
}
