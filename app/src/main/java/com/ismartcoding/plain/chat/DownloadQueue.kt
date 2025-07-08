package com.ismartcoding.plain.chat

import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.events.HttpApiEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object DownloadQueue {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloadChannel = Channel<DownloadTask>(Channel.BUFFERED)
    private val tasks = ConcurrentHashMap<String, DownloadTask>()
    private const val MAX_CONCURRENT = 3

    private val _downloadProgress = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, DownloadTask>> = _downloadProgress.asStateFlow()

    init {
        // Start download workers
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
        if (tasks.containsKey(messageFile.id)) {
            return messageFile.id
        }

        val downloadTask = DownloadTask(
            id = messageFile.id,
            messageFile = messageFile,
            peer = peer,
            messageId = messageId
        )

        tasks[downloadTask.id] = downloadTask
        scope.launch {
            downloadChannel.send(downloadTask)
            updateProgressFlow()
        }
        return downloadTask.id
    }

    fun pauseDownload(taskId: String): Boolean = tasks[taskId]?.let { task ->
        when (task.status) {
            DownloadStatus.DOWNLOADING -> {
                task.aborted = true
                task.httpClient?.dispatcher?.cancelAll()
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
    } ?: false

    fun resumeDownload(taskId: String): Boolean = tasks[taskId]?.let { task ->
        if (task.status == DownloadStatus.PAUSED) {
            task.aborted = false
            task.status = DownloadStatus.PENDING
            scope.launch {
                downloadChannel.send(task)
                updateProgressFlow()
            }
            true
        } else false
    } ?: false

    fun retryDownload(taskId: String): Boolean = tasks[taskId]?.let { task ->
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
            true
        } else false
    } ?: false

    fun removeDownload(taskId: String): Boolean = tasks[taskId]?.let { task ->
        if (task.status == DownloadStatus.DOWNLOADING) {
            task.aborted = true
            task.httpClient?.dispatcher?.cancelAll()
        }
        tasks.remove(taskId)
        task.status = DownloadStatus.CANCELED
        scope.launch { updateProgressFlow() }
        true
    } ?: false

    private suspend fun executeTaskAsync(task: DownloadTask) {
        task.status = DownloadStatus.DOWNLOADING
        task.aborted = false
        updateProgressFlow()

        val result = PeerFileDownloader.downloadAsync(
            context = MainApp.instance,
            task = task
        )

        if (task.aborted) {
            return
        }

        if (result != null) {
            sendEvent(HttpApiEvents.DownloadTaskDoneEvent(task))
            tasks.remove(task.id)
            updateProgressFlow()
        } else {
            task.status = DownloadStatus.FAILED
        }
    }

    private fun updateProgressFlow() {
        val progressMap = tasks.mapValues { (_, task) ->
            task.copy() // Create a new copy to trigger Flow update
        }
        _downloadProgress.value = progressMap
    }

    fun notifyProgressUpdate() {
        updateProgressFlow()
    }
} 