package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class ClipboardErrorCode {
    INVALID_CLIPBOARD_TEXT,
    CLIPBOARD_SYNC_DISABLED;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): ClipboardErrorCode? = entries.firstOrNull { it.name == value }
    }
}
