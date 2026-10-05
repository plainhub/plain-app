package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.features.download.DOWNLOAD_KIND_SHARE
import com.ismartcoding.plain.features.download.DownloadCenter
import com.ismartcoding.plain.features.download.DownloadEngine
import com.ismartcoding.plain.features.download.DownloadStatus
import com.ismartcoding.plain.features.download.DownloadFailure
import com.ismartcoding.plain.features.download.DownloadTaskHandle
import com.ismartcoding.plain.features.download.isTerminalDownloadStatus
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.platform.getDownloadsDirPath
import com.ismartcoding.plain.ui.page.sharedfolder.SharedFolderTransfer
import kotlinx.coroutines.CancellationException

/**
 * Engine for shared-folder batch downloads. Enqueue APIs build a
 * [SharedFolderBatchTask] per user action; execution walks the remote tree
 * once (SYNC/MULTI/ZIP), then streams files serially — the queue and UI only
 * ever see batch-level counters, never per-file rows.
 */
object SharedFolderDownloadEngine : DownloadEngine {
    private const val PROGRESS_INTERVAL_MS = 600L

    init {
        // Every enqueue API touches this object first, so the engine is
        // always registered before any of its tasks can reach the queue.
        DownloadCenter.registerEngine(DOWNLOAD_KIND_SHARE, this)
    }

    /** Downloads root subfolder used when saving into public Downloads. */
    private fun downloadsBase(): String = "${getDownloadsDirPath().trimEnd('/')}/PlainApp"

    fun enqueueFile(messageId: String, link: SharedLink, urlToken: String, entry: SharedFileDto, targetDir: String) {
        val task = SharedFolderBatchTask(
            id = "$messageId|${entry.virtualPath}|file|$targetDir",
            messageId = messageId, type = ShareBatchType.FILE, title = entry.name,
            targetDir = targetDir, link = link, urlToken = urlToken, entries = listOf(entry),
        )
        DownloadCenter.enqueueUnique(task)
    }

    fun enqueueDirSync(messageId: String, link: SharedLink, urlToken: String, entry: SharedFileDto, targetDir: String) {
        val task = SharedFolderBatchTask(
            id = "$messageId|${entry.virtualPath}|sync|$targetDir",
            messageId = messageId, type = ShareBatchType.SYNC, title = entry.name,
            targetDir = targetDir, link = link, urlToken = urlToken, entries = listOf(entry),
        )
        DownloadCenter.enqueueUnique(task)
    }

    fun enqueueZip(messageId: String, link: SharedLink, urlToken: String, entries: List<SharedFileDto>, zipName: String) {
        val task = SharedFolderBatchTask(
            id = "$messageId|zip|$zipName",
            messageId = messageId, type = ShareBatchType.ZIP, title = zipName,
            targetDir = "", link = link, urlToken = urlToken, entries = entries, zipName = zipName,
        )
        DownloadCenter.enqueueUnique(task)
    }

    fun enqueueMulti(messageId: String, link: SharedLink, urlToken: String, entries: List<SharedFileDto>, targetDir: String) {
        val selectionKey = entries.joinToString(",") { it.virtualPath }.hashCode()
        val task = SharedFolderBatchTask(
            id = "$messageId|multi|$selectionKey|$targetDir",
            messageId = messageId, type = ShareBatchType.MULTI,
            title = "${entries.firstOrNull()?.name ?: ""}${if (entries.size > 1) " (+${entries.size - 1})" else ""}",
            targetDir = targetDir, link = link, urlToken = urlToken, entries = entries,
        )
        DownloadCenter.enqueueUnique(task)
    }

    /** Re-runs only the failed files of a finished batch. */
    fun retryFailed(taskId: String) {
        val task = DownloadCenter.get(taskId) as? SharedFolderBatchTask ?: return
        if (!task.status.isTerminalDownloadStatus()) return
        DownloadCenter.requeue(taskId)
    }

    override suspend fun execute(taskHandle: DownloadTaskHandle) = com.ismartcoding.plain.features.download.DownloadIo.run {
        val task = taskHandle as SharedFolderBatchTask
        task.status = DownloadStatus.DOWNLOADING
        task.error = ""
        task.failures.clear()
        task.failedFiles = 0
        task.packing = false
        val notify = ThrottledNotifier(task)
        DownloadCenter.notifyProgressUpdate()
        try {
            when (task.type) {
                ShareBatchType.ZIP -> executeZip(task, notify)
                else -> executeFiles(task, notify)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            task.error = e.message ?: "error"
            if (task.type == ShareBatchType.ZIP) task.doneFiles = 0
        }
        if (task.status != DownloadStatus.CANCELED) {
            task.currentFile = ""
            task.packing = false
            task.downloadSpeed = 0
            task.status = deriveShareBatchStatus(
                running = false,
                canceled = false,
                doneFiles = task.doneFiles,
                failedFiles = task.failedFiles,
                error = task.error,
            )
            if (task.status == DownloadStatus.FAILED && task.error.isEmpty()) {
                task.error = "download failed"
            }
        }
        DownloadCenter.notifyProgressUpdate()
    }

    override fun onFinished(taskHandle: DownloadTaskHandle) {
        // Deliberately silent: completion feedback lives in the download UI
        // (mini bar flips to a check state, the downloads list shows the
        // terminal status), so finishing a batch does not interrupt the user.
    }

    /** File-based batches (FILE / SYNC / MULTI): enumerate once, stream serially. */
    private suspend fun executeFiles(task: SharedFolderBatchTask, notify: ThrottledNotifier) {
        if (task.enumerated == null) {
            task.enumerated = SharedLinkClient.plan(task.type, task.link, task.entries, task.targetDir, downloadsBase())
            task.totalFiles = task.enumerated!!.totalFiles
            task.totalSize = task.enumerated!!.totalSize
            notify.force()
        }
        val targets = task.enumerated!!.targets
        task.doneFiles = targets.count { it.entry.virtualPath in task.completedPaths }
        var baseBytes = targets.filter { it.entry.virtualPath in task.completedPaths }.sumOf { it.entry.size }
        task.downloadedSize = baseBytes
        notify.force()

        for (target in targets) {
            val entry = target.entry
            if (entry.virtualPath in task.completedPaths) continue
            if (task.aborted) return
            task.currentFile = entry.name
            notify.force()
            val size = entry.size.coerceAtLeast(1)
            try {
                if (target.storeToDownloads) {
                    val saved = SharedFolderTransfer.downloadFileToDownloads(task.link, task.urlToken, entry) { f ->
                        task.downloadedSize = baseBytes + (size * f).toLong()
                        notify.tick()
                    }
                    check(saved.isNotEmpty()) { "Platform declined saving shared file" }
                } else {
                    SharedFolderTransfer.downloadFileToDir(task.link, task.urlToken, entry, target.writeDir) { f ->
                        task.downloadedSize = baseBytes + (size * f).toLong()
                        notify.tick()
                    }
                }
                task.completedPaths.add(entry.virtualPath)
                task.doneFiles = targets.count { it.entry.virtualPath in task.completedPaths }
                baseBytes += entry.size
                task.downloadedSize = baseBytes
                notify.force()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                task.failures.add(DownloadFailure(entry.virtualPath, e.message ?: ""))
                task.failedFiles = task.failures.size
                task.downloadedSize = baseBytes
                notify.force()
            }
        }
    }

    /** ZIP batches: temp-download every file, then pack into one archive. */
    private suspend fun executeZip(task: SharedFolderBatchTask, notify: ThrottledNotifier) {
        var doneBytes = 0L
        var lastSize = 0L
        var startedCount = 0
        val ok = SharedFolderTransfer.zipEntriesTo(
            task.link, task.urlToken, task.entries,
            "${downloadsBase()}/${task.zipName}",
            onEnumerated = { count, bytes ->
                task.totalFiles = count
                task.totalSize = bytes
                notify.force()
            },
            onFileStart = { entry ->
                if (startedCount > 0) doneBytes += lastSize
                lastSize = entry.size
                startedCount++
                task.currentFile = entry.name
                task.doneFiles = startedCount - 1
                task.downloadedSize = doneBytes
                notify.force()
            },
            onFileProgress = { _, f ->
                task.downloadedSize = doneBytes + (lastSize * f).toLong()
                notify.tick()
            },
            onPacking = {
                task.packing = true
                task.currentFile = ""
                task.downloadedSize = task.totalSize
                notify.force()
            },
        )
        if (ok) {
            task.doneFiles = task.totalFiles
            task.downloadedSize = task.totalSize
        } else {
            task.error = "zip failed"
        }
    }

    /** Notifies the center at most once per [PROGRESS_INTERVAL_MS], computing batch speed. */
    private class ThrottledNotifier(private val task: SharedFolderBatchTask) {
        private var lastNotifyAt = 0L
        private var lastBytes = 0L

        fun tick() {
            val now = TimeHelper.nowMillis()
            if (now - lastNotifyAt < PROGRESS_INTERVAL_MS) return
            fire(now)
        }

        fun force() = fire(TimeHelper.nowMillis())

        private fun fire(now: Long) {
            val dt = (now - lastNotifyAt) / 1000.0
            if (lastNotifyAt > 0 && dt > 0) {
                task.downloadSpeed = ((task.downloadedSize - lastBytes) / dt).toLong().coerceAtLeast(0)
            }
            lastNotifyAt = now
            lastBytes = task.downloadedSize
            DownloadCenter.notifyProgressUpdate()
        }
    }
}

