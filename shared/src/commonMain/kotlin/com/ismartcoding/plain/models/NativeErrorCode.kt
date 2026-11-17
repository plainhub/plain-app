package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class NativeErrorCode {
    NATIVE_ERROR,
    NATIVE_PEER_ID_MISSING;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): NativeErrorCode? = entries.firstOrNull { it.name == value }
    }
}
