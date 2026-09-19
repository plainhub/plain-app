package com.ismartcoding.plain.chat.download

import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.features.download.DOWNLOAD_KIND_CHAT
import com.ismartcoding.plain.features.download.DownloadCenter
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.sendEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class DownloadProgressItem(
    val id: String,
    val messageId: String,
    val downloaded: Long,
    val total: Long,
    val speed: Long,
    val status: String,
)

/**
 * Chat-facing facade over [DownloadCenter]. Public API and WebSocket
 * DOWNLOAD_PROGRESS payload are unchanged; the queue mechanics and the
 * process-level lifetime now live in the shared center.
 */
object DownloadQueue {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val downloadProgress: StateFlow<Map<String, DownloadTask>> = DownloadCenter.progress
        .map { snapshot ->
            snapshot.values.filterIsInstance<DownloadTask>().associateBy { it.id }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    init {
        DownloadCenter.registerEngine(DOWNLOAD_KIND_CHAT, ChatDownloadEngine)
        scope.launch {
            DownloadCenter.updates.collect { snapshot ->
                val items = snapshot.values.filterIsInstance<DownloadTask>().map {
                    DownloadProgressItem(it.id, it.messageId, it.downloadedSize, it.messageFile.size, it.downloadSpeed, it.status.name.lowercase())
                }
                sendEvent(WebSocketEvent(EventType.DOWNLOAD_PROGRESS, JsonHelper.jsonEncode(items)))
            }
        }
    }

    fun addDownloadTask(messageFile: DMessageFile, peer: DPeer, messageId: String): String {
        DownloadCenter.add(DownloadTask(messageFile, peer, messageId))
        return messageFile.id
    }

    fun pauseDownload(taskId: String): Boolean = DownloadCenter.pause(taskId)

    fun resumeDownload(taskId: String): Boolean = DownloadCenter.resume(taskId)

    fun retryDownload(taskId: String): Boolean = DownloadCenter.retry(taskId)

    fun removeDownload(taskId: String): Boolean = DownloadCenter.remove(taskId)

    fun notifyProgressUpdate() = DownloadCenter.notifyProgressUpdate()
}
