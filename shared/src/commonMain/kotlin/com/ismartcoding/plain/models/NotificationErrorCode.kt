package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationErrorCode {
    ACTION_NOT_FOUND;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): NotificationErrorCode? = entries.firstOrNull { it.name == value }
    }
}
