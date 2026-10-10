package com.ismartcoding.plain.ai

import com.ismartcoding.plain.events.HEvent
import kotlinx.serialization.Serializable

data class HImageSearchStatusChangedEvent(
    val status: ImageSearchStatusType,
    val downloadProgress: Int = 0,
    val errorMessage: String = "",
) : HEvent()

data class HImageIndexProgressEvent(
    val total: Int,
    val indexed: Int,
    val isRunning: Boolean,
) : HEvent()

@Serializable
enum class ImageSearchStatusType {
    UNAVAILABLE,
    DOWNLOADING,
    LOADING,
    READY,
    ERROR,
}
