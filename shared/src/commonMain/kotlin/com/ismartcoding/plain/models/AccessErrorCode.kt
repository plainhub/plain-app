package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class AccessErrorCode {
    REMOTE_ACCESS_DISABLED,
    NO_PERMISSION,
    UNAUTHORIZED,
    FORBIDDEN,
    UNSUPPORTED_CAPABILITY;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): AccessErrorCode? = entries.firstOrNull { it.name == value }
    }
}
