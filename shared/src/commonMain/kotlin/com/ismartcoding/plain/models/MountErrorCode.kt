package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class MountErrorCode {
    MOUNT_FAILED;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): MountErrorCode? = entries.firstOrNull { it.name == value }
    }
}
