package com.ismartcoding.plain.features.download

/**
 * Lifecycle of a download task. Shared by every engine kind (chat peer
 * transfers, shared-folder batches); engines set one terminal status before
 * [DownloadEngine.execute] returns.
 */
enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    PARTIAL,
    FAILED,
    CANCELED,
}

val TERMINAL_DOWNLOAD_STATUSES = setOf(
    DownloadStatus.COMPLETED,
    DownloadStatus.PARTIAL,
    DownloadStatus.FAILED,
    DownloadStatus.CANCELED,
)

fun DownloadStatus.isTerminalDownloadStatus(): Boolean = this in TERMINAL_DOWNLOAD_STATUSES
