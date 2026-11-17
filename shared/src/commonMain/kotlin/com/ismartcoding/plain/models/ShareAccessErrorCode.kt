package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class ShareAccessErrorCode {
    PASSWORD_REQUIRED;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): ShareAccessErrorCode? = entries.firstOrNull { it.name == value }
    }
}
