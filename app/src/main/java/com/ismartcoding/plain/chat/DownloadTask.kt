package com.ismartcoding.plain.chat

import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DPeer
import okhttp3.OkHttpClient

data class DownloadTask(
    val id: String,
    val messageFile: DMessageFile,
    val peer: DPeer,
    val messageId: String,
    var status: DownloadStatus = DownloadStatus.PENDING,
    var error: String = "",
    var downloadedSize: Long = 0,
    var downloadSpeed: Long = 0,
    var lastDownloadedSize: Long = 0,
    var lastUpdateTime: Long? = null,
    var httpClient: OkHttpClient? = null,
    var aborted: Boolean = false
)  {
    fun isDownloading(): Boolean {
        return setOf(DownloadStatus.PENDING, DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED, DownloadStatus.FAILED).contains(status)
    }
}