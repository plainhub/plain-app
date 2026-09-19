package com.ismartcoding.plain.chat.download

import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.features.download.DOWNLOAD_KIND_CHAT
import com.ismartcoding.plain.features.download.DownloadStatus
import com.ismartcoding.plain.features.download.DownloadTaskHandle
import kotlinx.coroutines.Job

data class DownloadTask(
    val messageFile: DMessageFile,
    val peer: DPeer,
    val messageId: String,
    override var status: DownloadStatus = DownloadStatus.PENDING,
    override var error: String = "",
    var downloadedSize: Long = 0,
    var downloadSpeed: Long = 0,
    var lastDownloadedSize: Long = 0,
    var lastUpdateTime: Long? = null,
    override var job: Job? = null,
    override var aborted: Boolean = false,
) : DownloadTaskHandle {
    override val id: String get() = messageFile.id
    override val kind: String get() = DOWNLOAD_KIND_CHAT

    fun isDownloading(): Boolean {
        return status == DownloadStatus.PENDING || status == DownloadStatus.DOWNLOADING
    }

    /** True when a progress overlay / pause-resume controls should be shown. */
    fun isActive(): Boolean {
        return setOf(DownloadStatus.PENDING, DownloadStatus.DOWNLOADING, DownloadStatus.PAUSED, DownloadStatus.FAILED).contains(status)
    }

    override fun flowSnapshot(): DownloadTaskHandle = copy()
}
