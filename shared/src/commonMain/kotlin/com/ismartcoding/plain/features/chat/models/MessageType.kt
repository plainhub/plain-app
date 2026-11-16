package com.ismartcoding.plain.features.chat.models

import kotlinx.serialization.Serializable

@Serializable
enum class MessageType {
    MESSAGE,
    SHARE,
}
