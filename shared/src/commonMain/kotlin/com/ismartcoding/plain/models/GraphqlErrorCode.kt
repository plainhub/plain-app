package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class GraphqlErrorCode {
    INTERNAL_ERROR,
    INVALID_REQUEST;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): GraphqlErrorCode? = entries.firstOrNull { it.name == value }
    }
}
