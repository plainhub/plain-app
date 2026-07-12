package com.ismartcoding.plain.chat.download

import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.HDownloadTaskDoneEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.JsonHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

object DownloadQueue {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val downloadChannel = Channel<DownloadTask>(Channel.BUFFERED)
    private val tasksFlow = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    private const val MAX_CONCURRENT = 3

    val downloadProgress: StateFlow<Map<String, DownloadTask>> = tasksFlow.asStateFlow()

    init {
        repeat(MAX_CONCURRENT) {
            scope.launch {
                processDownloads()
            }
        }
    }

    private suspend fun processDownloads() {
        for (task in downloadChannel) {
            try {
                if (!task.aborted) {
                    executeTaskAsync(task)
                }
            } catch (e: Exception) {
                LogCat.e("Download task ${task.id} failed: ${e.message}")
                task.status = DownloadStatus.FAILED
                updateProgressFlow()
            }
        }
    }

    fun addDownloadTask(
        messageFile: DMessageFile,
        peer: DPeer,
        messageId: String
    ): String {
        if (tasksFlow.value.containsKey(messageFile.id)) {
            return messageFile.id
        }
        val downloadTask = DownloadTask(
            id = messageFile.id,
            messageFile = messageFile,
            peer = peer,
            messageId = messageId
        )
        tasksFlow.value = tasksFlow.value.toMutableMap().also { it[downloadTask.id] = downloadTask }
        scope.launch {
            downloadChannel.send(downloadTask)
            updateProgressFlow()
        }
        return downloadTask.id
    }

    fun pauseDownload(taskId: String): Boolean {
        val task = tasksFlow.value[taskId] ?: return false
        return when (task.status) {
            DownloadStatus.DOWNLOADING -> {
                task.aborted = true
                task.job?.cancel()
                task.status = DownloadStatus.PAUSED
                scope.launch { updateProgressFlow() }
                true
            }

            DownloadStatus.PENDING -> {
                task.status = DownloadStatus.PAUSED
                scope.launch { updateProgressFlow() }
                true
            }

            else -> false
        }
    }

    fun resumeDownload(taskId: String): Boolean {
        val task = tasksFlow.value[taskId] ?: return false
        if (task.status == DownloadStatus.PAUSED) {
            task.aborted = false
            task.status = DownloadStatus.PENDING
            scope.launch {
                downloadChannel.send(task)
                updateProgressFlow()
            }
            return true
        }
        return false
    }

    fun retryDownload(taskId: String): Boolean {
        val task = tasksFlow.value[taskId] ?: return false
        if (task.status == DownloadStatus.FAILED) {
            task.apply {
                error = ""
                downloadedSize = 0
                downloadSpeed = 0
                lastDownloadedSize = 0
                lastUpdateTime = null
            }
            task.aborted = false
            task.status = DownloadStatus.PENDING
            scope.launch {
                downloadChannel.send(task)
                updateProgressFlow()
            }
            return true
        }
        return false
    }

    fun removeDownload(taskId: String): Boolean {
        val task = tasksFlow.value[taskId] ?: return false
        if (task.status == DownloadStatus.DOWNLOADING) {
            task.aborted = true
            task.job?.cancel()
        }
        tasksFlow.value = tasksFlow.value.filterKeys { it != taskId }
        task.status = DownloadStatus.CANCELED
        scope.launch { updateProgressFlow() }
        return true
    }

    private suspend fun executeTaskAsync(task: DownloadTask) {
        task.status = DownloadStatus.DOWNLOADING
        task.aborted = false
        updateProgressFlow()

        val result = PeerFileDownloader.downloadAsync(task)

        if (task.aborted) {
            return
        }

        if (result != null) {
            sendEvent(HDownloadTaskDoneEvent(task))
            tasksFlow.value = tasksFlow.value.filterKeys { it != task.id }
            updateProgressFlow()
        } else {
            task.status = DownloadStatus.FAILED
        }
    }

    private suspend fun updateProgressFlow() {
        val progressMap = tasksFlow.value.mapValues { (_, task) -> task.copy() }
        tasksFlow.value = progressMap
        val items = progressMap.values.map { task ->
            DownloadProgressItem(
                id = task.id,
                messageId = task.messageId,
                downloaded = task.downloadedSize,
                total = task.messageFile.size,
                speed = task.downloadSpeed,
                status = task.status.name.lowercase(),
            )
        }
        sendEvent(WebSocketEvent(EventType.DOWNLOAD_PROGRESS, JsonHelper.jsonEncode(items)))
    }

    fun notifyProgressUpdate() {
        scope.launch { updateProgressFlow() }
    }
}
