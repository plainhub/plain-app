package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class FileIndexScanState {
    IDLE,
    RUNNING,
    PAUSED,
    STOPPED,
    FAILED,
}
