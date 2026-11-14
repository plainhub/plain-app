package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class MediaDataType {
    AUDIO,
    VIDEO,
    IMAGE,
    DOC,
}
