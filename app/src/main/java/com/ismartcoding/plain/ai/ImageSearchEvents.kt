package com.ismartcoding.plain.ai

import com.ismartcoding.lib.channel.ChannelEvent

data class ImageSearchStatusChangedEvent(
    val status: ImageSearchStatus,
    val downloadProgress: Int = 0,
    val errorMessage: String = "",
) : ChannelEvent()

data class ImageIndexProgressEvent(
    val total: Int,
    val indexed: Int,
    val isRunning: Boolean,
) : ChannelEvent()

enum class ImageSearchStatus {
    UNAVAILABLE,
    DOWNLOADING,
    LOADING,
    READY,
    ERROR,
}
