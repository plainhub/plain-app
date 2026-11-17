package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class AudioQueueErrorCode {
    BULK_QUERY_REQUIRED;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): AudioQueueErrorCode? = entries.firstOrNull { it.name == value }
    }
}
