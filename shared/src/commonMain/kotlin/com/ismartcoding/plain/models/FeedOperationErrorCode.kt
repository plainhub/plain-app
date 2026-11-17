package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class FeedOperationErrorCode {
    FEED_NOT_FOUND,
    INVALID_FEED_URL;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): FeedOperationErrorCode? = entries.firstOrNull { it.name == value }
    }
}
