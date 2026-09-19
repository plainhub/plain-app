package com.ismartcoding.plain.features.download

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.PlatformLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private class Run(
        val task: DownloadTaskHandle,
        val previous: CompletableDeferred<Unit>?,
        val fresh: DownloadTaskHandle?,
    ) {
        val finished = CompletableDeferred<Unit>()
        var canceled = false
        var started = false
        var job: Job? = null
    }

    // The registry already retains queued tasks. Keep dispatch non-suspending:
    // overflow send coroutines could reorder a successor ahead of its predecessor.
    private val downloadChannel = Channel<Run>(Channel.UNLIMITED)
    private val runs = mutableMapOf<String, Run>()
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
        if (hasPendingRun(existing.id) || !existing.status.isTerminalDownloadStatus()) return@withLock false
        existing.status = DownloadStatus.PENDING
        dispatch(existing, task)
        scope.launch { updateProgressFlow() }
        true
    }

    /**
     * Sends under the caller's lock so runs retain predecessor order.
     */
    private fun dispatch(task: DownloadTaskHandle, fresh: DownloadTaskHandle? = null) {
        val run = Run(task, runs[task.id]?.finished, fresh)
        runs[task.id] = run
        check(downloadChannel.trySend(run).isSuccess)
    }

    // Called under tasksLock. Keep the completion chain until the worker exits,
    // so an immediate resume/re-enqueue cannot overlap a canceled engine's cleanup.
    private fun cancelRun(taskId: String) {
        runs[taskId]?.let { run ->
            run.canceled = true
            run.job?.cancel()
        }
    }

    private fun isCurrent(run: Run) =
        !run.canceled && runs[run.task.id] === run && tasks[run.task.id] === run.task

    private fun hasPendingRun(taskId: String) =
        runs[taskId]?.let { !it.canceled && !it.started } == true

    fun pause(taskId: String): Boolean = tasksLock.withLock {
        val task = tasks[taskId] ?: return@withLock false
        when (task.status) {
            DownloadStatus.DOWNLOADING -> {
                task.aborted = true
                cancelRun(taskId)
                task.status = DownloadStatus.PAUSED
                scope.launch { updateProgressFlow() }
                true
            }
            DownloadStatus.PENDING -> {
                cancelRun(taskId)
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
        task.status = DownloadStatus.PENDING
        dispatch(task)
        scope.launch { updateProgressFlow() }
        true
    }

    /** Retries a failed task from scratch, resetting its counters. */
    fun retry(taskId: String): Boolean = tasksLock.withLock {
        val task = tasks[taskId] ?: return@withLock false
        if (hasPendingRun(taskId)) return@withLock false
        if (task.status != DownloadStatus.FAILED && task.status != DownloadStatus.PARTIAL) return@withLock false
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
        if (hasPendingRun(taskId)) return@withLock false
        if (!task.status.isTerminalDownloadStatus()) return@withLock false
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
        cancelRun(taskId)
        task.status = DownloadStatus.CANCELED
        scope.launch { updateProgressFlow() }
        true
    }

    /** Drops a task from the registry, aborting it first when still active. */
    fun remove(taskId: String): Boolean = tasksLock.withLock {
        val task = tasks[taskId] ?: return@withLock false
        task.aborted = true
        cancelRun(taskId)
        tasks.remove(taskId)
        scope.launch { updateProgressFlow() }
        true
    }

    fun notifyProgressUpdate() {
        scope.launch { updateProgressFlow() }
    }

    private suspend fun processDownloads() {
        for (run in downloadChannel) {
            val task = run.task
            try {
                run.previous?.await()
                executeTaskAsync(run)
            } catch (e: Exception) {
                LogCat.e("Download task ${task.id} failed: ${e.message}")
                tasksLock.withLock {
                    if (isCurrent(run) && !task.aborted && !task.status.isTerminalDownloadStatus()) {
                        task.status = DownloadStatus.FAILED
                    }
                }
                updateProgressFlow()
            } finally {
                tasksLock.withLock {
                    if (runs[task.id] === run) runs.remove(task.id)
                    run.finished.complete(Unit)
                }
            }
        }
    }

    private suspend fun executeTaskAsync(run: Run) {
        val task = run.task
        val engine = tasksLock.withLock {
            if (!isCurrent(run)) return@withLock null
            run.started = true
            // An old engine can still be unwinding when the task is requeued.
            // Refresh its mutable payload only after that execution has exited.
            run.fresh?.let { task.refreshFrom(it) }
            task.aborted = false
            val engine = engines[task.kind]
            if (engine == null) {
                task.error = "no engine registered for kind ${task.kind}"
                task.status = DownloadStatus.FAILED
            } else {
                task.status = DownloadStatus.DOWNLOADING
                // Publish the job before starting it, closing the cancel/start race.
                run.job = scope.launch(start = CoroutineStart.LAZY) {
                    try {
                        engine.execute(task)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        tasksLock.withLock {
                            if (isCurrent(run)) task.error = e.message ?: ""
                        }
                    }
                }
                task.job = run.job
            }
            engine
        }
        updateProgressFlow()
        if (engine == null) return
        val executeJob = run.job!!
        executeJob.start()
        executeJob.join()
        val notifyFinished = tasksLock.withLock {
            if (!isCurrent(run) || task.aborted) return@withLock false
            if (!task.status.isTerminalDownloadStatus()) {
                task.status = DownloadStatus.FAILED
                LogCat.e("Download engine ${task.kind} returned without a terminal status for ${task.id}")
            }
            true
        }
        if (notifyFinished) engine.onFinished(task)
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
