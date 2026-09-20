package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.features.download.DOWNLOAD_KIND_SHARE
import com.ismartcoding.plain.features.download.DownloadCenter
import com.ismartcoding.plain.features.download.DownloadEngine
import com.ismartcoding.plain.features.download.DownloadFailure
import com.ismartcoding.plain.features.download.DownloadStatus
import com.ismartcoding.plain.features.download.DownloadTaskHandle
import com.ismartcoding.plain.features.download.isTerminalDownloadStatus
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.platform.getDownloadsDirPath
import com.ismartcoding.plain.ui.page.sharedfolder.SharedFolderTransfer
import kotlinx.coroutines.CancellationException

/** How a batch was created; drives enumeration and destination rules. */
enum class ShareBatchType { FILE, ZIP, SYNC, MULTI }

/** One resolved file destination inside a batch. */
data class ShareFileTarget(
    val entry: SharedFileDto,
    /** Write dir for plain writes; ignored when [storeToDownloads] is true. */
    val writeDir: String,
    /** MediaStore public Downloads (flat, top-level files only). */
    val storeToDownloads: Boolean,
)

/**
 * Aggregated download task for a shared folder: one user action (single file,
 * dir zip, dir sync or multi-selection save) is ONE task with file counters,
 * so a 1000-file sync renders as one card and occupies one queue slot.
 * [completedPaths] survives [DownloadCenter.requeue] retries, letting the
 * engine re-run only failed files.
 */
class SharedFolderBatchTask(
    override val id: String,
    val messageId: String,
    val type: ShareBatchType,
    val title: String,
    /** "" = public Downloads target; otherwise an absolute directory path. */
    val targetDir: String,
    /** Working endpoint; refreshed from a fresh enqueue before a re-run (see [refreshFrom]). */
    var link: SharedLink,
    var urlToken: String,
    val entries: List<SharedFileDto>,
    val zipName: String = "",
) : DownloadTaskHandle {
    override val kind: String = DOWNLOAD_KIND_SHARE
    override var status: DownloadStatus = DownloadStatus.PENDING
    override var error: String = ""
    var downloadedSize: Long = 0
    var totalSize: Long = 0
    var downloadSpeed: Long = 0
    var totalFiles: Int = 0
    var doneFiles: Int = 0
    var failedFiles: Int = 0
    var currentFile: String = ""
    var packing: Boolean = false
    var failures = mutableListOf<DownloadFailure>()
    override var aborted: Boolean = false
    override var job: kotlinx.coroutines.Job? = null

    val completedPaths = mutableSetOf<String>()
    /** Files resolved by the walker; null until the first run enumerates them. */
    var enumerated: List<ShareFileTarget>? = null

    override fun flowSnapshot(): DownloadTaskHandle {
        val s = SharedFolderBatchTask(
            id, messageId, type, title, targetDir, link, urlToken, entries, zipName,
        )
        s.status = status
        s.error = error
        s.downloadedSize = downloadedSize
        s.totalSize = totalSize
        s.downloadSpeed = downloadSpeed
        s.totalFiles = totalFiles
        s.doneFiles = doneFiles
        s.failedFiles = failedFiles
        s.currentFile = currentFile
        s.packing = packing
        s.failures = failures.toList().toMutableList()
        return s
    }

    /** Takes over the fresh enqueue's endpoint, so re-runs survive address changes. */
    override fun refreshFrom(fresh: DownloadTaskHandle) {
        if (fresh !is SharedFolderBatchTask) return
        link = fresh.link
        urlToken = fresh.urlToken
    }

    /** Overall fraction 0..1; 0 while the walker is still counting. */
    fun fraction(): Float = if (totalSize > 0) (downloadedSize.toFloat() / totalSize).coerceIn(0f, 1f) else 0f
}

/**
 * Terminal batch status from counters. Active states outrank error so a huge
 * batch keeps running (and keeps its progress bar) while a few files failed.
 */
internal fun deriveShareBatchStatus(
    running: Boolean,
    canceled: Boolean,
    doneFiles: Int,
    failedFiles: Int,
): DownloadStatus = when {
    running -> DownloadStatus.DOWNLOADING
    canceled -> DownloadStatus.CANCELED
    failedFiles == 0 -> DownloadStatus.COMPLETED
    doneFiles > 0 -> DownloadStatus.PARTIAL
    else -> DownloadStatus.FAILED
}

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

    override suspend fun execute(taskHandle: DownloadTaskHandle) {
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
        }
        if (task.status != DownloadStatus.CANCELED) {
            task.currentFile = ""
            task.downloadSpeed = 0
            task.status = deriveShareBatchStatus(
                running = false,
                canceled = false,
                doneFiles = task.doneFiles,
                failedFiles = task.failedFiles,
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
            task.enumerated = enumerate(task)
            task.totalFiles = task.enumerated!!.size
            task.totalSize = task.enumerated!!.sumOf { it.entry.size }
            notify.force()
        }
        val targets = task.enumerated!!
        val store = task.targetDir.isEmpty()
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
                    SharedFolderTransfer.downloadFileToDownloads(task.link, task.urlToken, entry) { f ->
                        task.downloadedSize = baseBytes + (size * f).toLong()
                        notify.tick()
                    }
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
                baseBytes += entry.size
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

    /**
     * Resolves the batch file list once. Top-level files of FILE/MULTI going
     * to the Downloads target use the MediaStore store; everything else is a
     * plain write mirroring the remote tree under the target base dir.
     */
    private suspend fun enumerate(task: SharedFolderBatchTask): List<ShareFileTarget> {
        val out = mutableListOf<ShareFileTarget>()
        val plainBase = task.targetDir.ifEmpty { downloadsBase() }
        val toDownloads = task.targetDir.isEmpty()
        when (task.type) {
            ShareBatchType.FILE -> {
                val entry = task.entries.first()
                if (toDownloads) out.add(ShareFileTarget(entry, "", true))
                else out.add(ShareFileTarget(entry, plainBase, false))
            }
            ShareBatchType.SYNC -> {
                val root = task.entries.first()
                walk(task.link, root, "${plainBase.trimEnd('/')}/${root.name}", out)
            }
            ShareBatchType.MULTI -> task.entries.forEach { entry ->
                if (entry.isDir) walk(task.link, entry, "${plainBase.trimEnd('/')}/${entry.name}", out)
                else if (toDownloads) out.add(ShareFileTarget(entry, "", true))
                else out.add(ShareFileTarget(entry, plainBase, false))
            }
            ShareBatchType.ZIP -> {}
        }
        return out
    }

    private suspend fun walk(
        link: SharedLink,
        entry: SharedFileDto,
        writeDir: String,
        out: MutableList<ShareFileTarget>,
    ) {
        if (!entry.isDir) {
            out.add(ShareFileTarget(entry, writeDir, false))
            return
        }
        val info = SharedLinkClient.fetchSharedInfo(link, entry.virtualPath.takeIf { it.isNotEmpty() })
        info.entries.sortedWith(
            compareBy<SharedFileDto> { !it.isDir }.thenBy { it.name.lowercase() },
        ).forEach { child ->
            walk(link, child, "$writeDir/${child.name}", out)
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

