package com.ismartcoding.plain.chat.download

import com.ismartcoding.plain.events.HDownloadTaskDoneEvent
import com.ismartcoding.plain.features.download.DownloadCenter
import com.ismartcoding.plain.features.download.DownloadEngine
import com.ismartcoding.plain.features.download.DownloadStatus
import com.ismartcoding.plain.features.download.DownloadTaskHandle
import com.ismartcoding.plain.lib.sendEvent

/** Chat engine: delegates to the peer downloader and reports chat-side completion. */
object ChatDownloadEngine : DownloadEngine {
    override suspend fun execute(task: DownloadTaskHandle) {
        PeerFileDownloader.downloadAsync(task as DownloadTask)
    }

    override fun onFinished(task: DownloadTaskHandle) {
        val chatTask = task as DownloadTask
        if (chatTask.status == DownloadStatus.COMPLETED) {
            sendEvent(HDownloadTaskDoneEvent(chatTask))
            DownloadCenter.remove(chatTask.id)
        }
    }
}
