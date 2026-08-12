package com.ismartcoding.plain.ai

import com.ismartcoding.plain.lib.ChannelEvent
import kotlinx.serialization.Serializable

data class ImageSearchStatusChangedEvent(
    val status: ImageSearchStatusType,
    val downloadProgress: Int = 0,
    val errorMessage: String = "",
) : ChannelEvent()

data class ImageIndexProgressEvent(
    val total: Int,
    val indexed: Int,
    val isRunning: Boolean,
) : ChannelEvent()

@Serializable
enum class ImageSearchStatusType {
    UNAVAILABLE,
    DOWNLOADING,
    LOADING,
    READY,
    ERROR,
}
