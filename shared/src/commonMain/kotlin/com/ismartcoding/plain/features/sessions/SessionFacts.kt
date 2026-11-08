package com.ismartcoding.plain.features.sessions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SessionFacts(
    val clientId: String,
    @SerialName("clientName") val name: String,
    val type: SessionType,
    val token: String,
    val clientIp: String,
    val osName: String,
    val osVersion: String,
    val appName: String,
    val appVersion: String,
    val createdAt: String,
    val updatedAt: String,
    val lastSeenAt: String?,
)
