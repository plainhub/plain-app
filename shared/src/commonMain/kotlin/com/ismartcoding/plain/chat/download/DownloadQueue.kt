package com.ismartcoding.plain.chat.download

import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.EventType
import com.ismartcoding.plain.events.HDownloadTaskDoneEvent
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.helpers.JsonHelper
import com.ismartcoding.plain.platform.PlatformLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val tasks = mutableMapOf<String, DownloadTask>()
    private val tasksLock = PlatformLock()

    val downloadProgress = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())

    private const val MAX_CONCURRENT = 3

    init {
        repeat(MAX_CONCURRENT) {
            scope.launch { processDownloads() }
        }
    }

    private suspend fun processDownloads() {
        for (task in downloadChannel) {
            try {
                if (!task.aborted) executeTaskAsync(task)
            } catch (e: Exception) {
                LogCat.e("Download task ${task.id} failed: ${e.message}")
                task.status = DownloadStatus.FAILED
                updateProgressFlow()
            }
        }
    }

    fun addDownloadTask(messageFile: DMessageFile, peer: DPeer, messageId: String): String {
        tasksLock.withLock {
            if (tasks.containsKey(messageFile.id)) return@withLock
            val task = DownloadTask(id = messageFile.id, messageFile = messageFile, peer = peer, messageId = messageId)
            tasks[task.id] = task
            scope.launch {
                downloadChannel.send(task)
                updateProgressFlow()
            }
        }
        return messageFile.id
    }

    fun pauseDownload(taskId: String): Boolean = tasksLock.withLock {
        val task = tasks[taskId] ?: return@withLock false
        when (task.status) {
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

    fun resumeDownload(taskId: String): Boolean = tasksLock.withLock {
        val task = tasks[taskId] ?: return@withLock false
        if (task.status != DownloadStatus.PAUSED) return@withLock false
        task.aborted = false
        task.status = DownloadStatus.PENDING
        scope.launch {
            downloadChannel.send(task)
            updateProgressFlow()
        }
        true
    }

    fun retryDownload(taskId: String): Boolean = tasksLock.withLock {
        val task = tasks[taskId] ?: return@withLock false
        if (task.status != DownloadStatus.FAILED) return@withLock false
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
        true
    }

    fun removeDownload(taskId: String): Boolean = tasksLock.withLock {
        val task = tasks[taskId] ?: return@withLock false
        if (task.status == DownloadStatus.DOWNLOADING) {
            task.aborted = true
            task.job?.cancel()
        }
        tasks.remove(taskId)
        task.status = DownloadStatus.CANCELED
        scope.launch { updateProgressFlow() }
        true
    }

    private suspend fun executeTaskAsync(task: DownloadTask) {
        task.status = DownloadStatus.DOWNLOADING
        task.aborted = false
        updateProgressFlow()

        val result = PeerFileDownloader.downloadAsync(task)

        if (task.aborted) return

        if (result != null) {
            sendEvent(HDownloadTaskDoneEvent(task))
            tasksLock.withLock { tasks.remove(task.id) }
            updateProgressFlow()
        } else {
            task.status = DownloadStatus.FAILED
            updateProgressFlow()
        }
    }

    private suspend fun updateProgressFlow() {
        val snapshot = tasksLock.withLock { tasks.mapValues { it.value.copy() }.toMap() }
        downloadProgress.value = snapshot
        sendEvent(WebSocketEvent(EventType.DOWNLOAD_PROGRESS, JsonHelper.jsonEncode(
            snapshot.values.map {
                DownloadProgressItem(it.id, it.messageId, it.downloadedSize, it.messageFile.size, it.downloadSpeed, it.status.name.lowercase())
            }
        )))
    }

    fun notifyProgressUpdate() {
        scope.launch { updateProgressFlow() }
    }
}
