package com.ismartcoding.plain.features.settings

import kotlinx.serialization.Serializable

@Serializable
enum class AppUpdateDownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELED,
}
