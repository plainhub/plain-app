package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class DiskFormatErrorCode {
    FORMAT_DISK_FAILED;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): DiskFormatErrorCode? = entries.firstOrNull { it.name == value }
    }
}
