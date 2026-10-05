package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.features.download.DownloadStatus

/**
 * Terminal batch status from counters. Active states outrank error so a huge
 * batch keeps running (and keeps its progress bar) while a few files failed.
 */
internal fun deriveShareBatchStatus(
    running: Boolean,
    canceled: Boolean,
    doneFiles: Int,
    failedFiles: Int,
    error: String = "",
): DownloadStatus = when {
    running -> DownloadStatus.DOWNLOADING
    canceled -> DownloadStatus.CANCELED
    failedFiles == 0 && error.isEmpty() -> DownloadStatus.COMPLETED
    doneFiles > 0 -> DownloadStatus.PARTIAL
    else -> DownloadStatus.FAILED
}

