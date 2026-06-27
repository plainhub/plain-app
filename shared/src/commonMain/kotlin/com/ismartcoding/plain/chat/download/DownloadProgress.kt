package com.ismartcoding.plain.chat.download

data class DownloadProgress(
    val fileId: String,
    val fileName: String,
    val downloaded: Long = 0,
    val total: Long = 0,
    val status: DownloadStatus = DownloadStatus.PENDING
)