package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class ArchiveErrorCode {
    ARCHIVE_ENTRY_UNAVAILABLE;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): ArchiveErrorCode? = entries.firstOrNull { it.name == value }
    }
}
