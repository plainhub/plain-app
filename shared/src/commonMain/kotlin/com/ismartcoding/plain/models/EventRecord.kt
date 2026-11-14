package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
enum class EventType {
    LOGIN, LOGIN_FAILED, LOGOUT, REVOKE_SESSION, UPDATE_DEVICE_NAME,
    MOUNT, MOUNT_FAILED, UNMOUNT, FORMAT_DISK, FORMAT_DISK_FAILED,
}

@Serializable
data class EventRecord(
    val id: String,
    val type: EventType,
    val message: String,
    val clientId: String?,
    val createdAt: Instant,
)
