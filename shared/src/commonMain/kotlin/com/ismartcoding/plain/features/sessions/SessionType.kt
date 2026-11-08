package com.ismartcoding.plain.features.sessions

import kotlinx.serialization.Serializable

@Serializable
enum class SessionType {
    LOGIN,
    API_TOKEN;
}
