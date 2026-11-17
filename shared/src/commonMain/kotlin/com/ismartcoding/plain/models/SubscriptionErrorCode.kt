package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class SubscriptionErrorCode {
    INVALID_SUBSCRIPTION,
    SUBSCRIPTION_RESULT_TOO_LARGE,
    RESYNC_REQUIRED,
    SLOW_CLIENT;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): SubscriptionErrorCode? = entries.firstOrNull { it.name == value }
    }
}
