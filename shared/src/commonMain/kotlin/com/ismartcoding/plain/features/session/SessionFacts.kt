package com.ismartcoding.plain.features.session

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
internal data class SessionFacts(
    @SerialName("client_id") val clientId: String,
    val name: String,
    val type: String,
    val token: String,
    @SerialName("client_ip") val clientIp: String,
    @SerialName("os_name") val osName: String,
    @SerialName("os_version") val osVersion: String,
    @SerialName("browser_name") val browserName: String,
    @SerialName("browser_version") val browserVersion: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("last_active_at") val lastActiveAt: Long?,
)
