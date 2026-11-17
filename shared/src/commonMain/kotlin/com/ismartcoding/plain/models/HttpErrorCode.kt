package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class HttpErrorCode {
    NETWORK_ERROR,
    REQUEST_CONTEXT_CHANGED;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): HttpErrorCode? = entries.firstOrNull { it.name == value }
    }
}
