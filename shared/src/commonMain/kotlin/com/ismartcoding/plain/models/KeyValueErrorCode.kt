package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class KeyValueErrorCode {
    INVALID_KEY_VALUE;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): KeyValueErrorCode? = entries.firstOrNull { it.name == value }
    }
}
