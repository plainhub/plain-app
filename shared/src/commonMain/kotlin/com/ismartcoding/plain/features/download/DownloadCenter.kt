package com.ismartcoding.plain.features.download

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.PlatformLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Process-level download queue shared by every engine kind. Owns the FIFO
 * channel, the concurrency cap, the task registry and the progress state
 * flow; transfers live in registered [DownloadEngine]s. Tasks survive page
 * navigation — only process death stops them.
 */
object DownloadCenter {
    const val MAX_CONCURRENT = 3

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val downloadChannel = Channel<DownloadTaskHandle>(Channel.BUFFERED)
    private val tasks = mutableMapOf<String, DownloadTaskHandle>()
    private val tasksLock = PlatformLock()
    private val engines = mutableMapOf<String, DownloadEngine>()

    /** Snapshot of all tasks, keyed by id, for UI collection. */
    val progress = MutableStateFlow<Map<String, DownloadTaskHandle>>(emptyMap())

    /** Same snapshots as [progress], emitted on every change (e.g. WebSocket relay). */
    val updates = MutableSharedFlow<Map<String, DownloadTaskHandle>>(extraBufferCapacity = 64)

    init {
        repeat(MAX_CONCURRENT) {
            scope.launch { processDownloads() }
        }
    }

    fun registerEngine(kind: String, engine: DownloadEngine) {
        tasksLock.withLock { engines[kind] = engine }
    }

    fun get(taskId: String): DownloadTaskHandle? = tasksLock.withLock { tasks[taskId] }

    fun all(): List<DownloadTaskHandle> = tasksLock.withLock { tasks.values.map { it.flowSnapshot() } }

    /** Enqueues a task; false when an active or finished task with the same id exists. */
    fun add(task: DownloadTaskHandle): Boolean = tasksLock.withLock {
        if (tasks.containsKey(task.id)) return@withLock false
        tasks[task.id] = task
        dispatch(task)
        scope.launch { updateProgressFlow() }
        true
    }

    /**
     * Enqueues [task], or — when a task with the same id already exists —
     * re-runs that one if it finished (retry semantics: fresh user intent),
     * keeping its engine-side bookkeeping (e.g. completed files). Returns
     * false only when an identical task is queued or running right now.
     */
    fun enqueueUnique(task: DownloadTaskHandle): Boolean = tasksLock.withLock {
        val existing = tasks[task.id]
        if (existing == null) {
            tasks[task.id] = task
            dispatch(task)
            scope.launch { updateProgressFlow() }
            return@withLock true
        }
        if (!existing.status.isTerminalDownloadStatus()) return@withLock false
        existing.refreshFrom(task)
        existing.aborted = false
        existing.status = DownloadStatus.PENDING
        dispatch(existing)
        scope.launch { updateProgressFlow() }
        true
    }

    /**
     * Sends under the caller's lock so bursts keep their order (trySend is
     * non-suspending; the async send is only a fallback past capacity).
     */
    private fun dispatch(task: DownloadTaskHandle) {
        if (!downloadChannel.trySend(task).isSuccess) {
            scope.launch { downloadChannel.send(task) }
        }
    }

    fun pause(taskId: String): Boolean = tasksLock.withLock {
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

    fun resume(taskId: String): Boolean = tasksLock.withLock {
        val task = tasks[taskId] ?: return@withLock false
        if (task.status != DownloadStatus.PAUSED) return@withLock false
        task.aborted = false
        task.status = DownloadStatus.PENDING
        dispatch(task)
        scope.launch { updateProgressFlow() }
        true
    }

    /** Retries a failed task from scratch, resetting its counters. */
    fun retry(taskId: String): Boolean = tasksLock.withLock {
        val task = tasks[taskId] ?: return@withLock false
        if (task.status != DownloadStatus.FAILED && task.status != DownloadStatus.PARTIAL) return@withLock false
        task.aborted = false
        task.status = DownloadStatus.PENDING
        dispatch(task)
        scope.launch { updateProgressFlow() }
        true
    }

    /**
     * Re-enqueues a task without resetting anything — for engines that resume
     * from their own bookkeeping (e.g. re-running only failed files).
     */
    fun requeue(taskId: String): Boolean = tasksLock.withLock {
        val task = tasks[taskId] ?: return@withLock false
        if (!task.status.isTerminalDownloadStatus()) return@withLock false
        task.aborted = false
        task.status = DownloadStatus.PENDING
        dispatch(task)
        scope.launch { updateProgressFlow() }
        true
    }

    /**
     * Aborts an active task and keeps it in the registry (terminal CANCELED),
     * so it stays visible in finished lists until removed.
     */
    fun cancel(taskId: String): Boolean = tasksLock.withLock {
        val task = tasks[taskId] ?: return@withLock false
        if (task.status.isTerminalDownloadStatus()) return@withLock false
        task.aborted = true
        task.job?.cancel()
        task.status = DownloadStatus.CANCELED
        scope.launch { updateProgressFlow() }
        true
    }

    /** Drops a task from the registry, aborting it first when still active. */
    fun remove(taskId: String): Boolean = tasksLock.withLock {
        val task = tasks[taskId] ?: return@withLock false
        if (task.status == DownloadStatus.DOWNLOADING) {
            task.aborted = true
            task.job?.cancel()
        }
        tasks.remove(taskId)
        scope.launch { updateProgressFlow() }
        true
    }

    fun notifyProgressUpdate() {
        scope.launch { updateProgressFlow() }
    }

    private suspend fun processDownloads() {
        for (task in downloadChannel) {
            try {
                // A channel entry may be stale (task paused, canceled or
                // re-enqueued since it was sent); only fresh PENDING entries run.
                if (!task.aborted && task.status == DownloadStatus.PENDING) executeTaskAsync(task)
            } catch (e: Exception) {
                LogCat.e("Download task ${task.id} failed: ${e.message}")
                if (!task.aborted && !task.status.isTerminalDownloadStatus()) {
                    task.status = DownloadStatus.FAILED
                }
                updateProgressFlow()
            }
        }
    }

    private suspend fun executeTaskAsync(task: DownloadTaskHandle) {
        val engine = tasksLock.withLock { engines[task.kind] }
        if (engine == null) {
            LogCat.e("No engine registered for download kind ${task.kind}, failing ${task.id}")
            task.error = "no engine registered for kind ${task.kind}"
            task.status = DownloadStatus.FAILED
            updateProgressFlow()
            return
        }
        task.status = DownloadStatus.DOWNLOADING
        task.aborted = false
        updateProgressFlow()
        // Engine work runs in its own child job so pause/cancel target the
        // transfer only — never the resident queue worker driving it.
        val executeJob = scope.launch {
            try {
                engine.execute(task)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                task.error = e.message ?: ""
            }
        }
        task.job = executeJob
        executeJob.join()
        if (task.aborted) return
        if (!task.status.isTerminalDownloadStatus()) {
            // Engines must report a terminal status; treat silence as failure.
            task.status = DownloadStatus.FAILED
            LogCat.e("Download engine ${task.kind} returned without a terminal status for ${task.id}")
        }
        engine.onFinished(task)
        updateProgressFlow()
    }

    private suspend fun updateProgressFlow() {
        val snapshot = tasksLock.withLock {
            tasks.mapValues { it.value.flowSnapshot() }.toMap()
        }
        progress.value = snapshot
        updates.tryEmit(snapshot)
    }
}
