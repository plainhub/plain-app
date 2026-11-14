package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class FeedErrorCode {
    NONE,
    TIMEOUT,
    SERVER,
    DNS,
    PARSE,
    UNKNOWN,
}
