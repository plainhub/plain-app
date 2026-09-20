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

    /**
     * Monotonic execution counter per task id, mutated under [tasksLock].
     * Lets a finished execution recognize that a NEWER execution already
     * claimed the task (requeue/resume raced the verdict) and hands off
     * instead of failing the fresh run.
     */
    private val executions = mutableMapOf<String, Long>()

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
                // The fields are read under the registry lock: producers write
                // status/aborted inside the same lock before dispatching, so an
                // unlocked read could observe a stale terminal status on weakly
                // ordered cores (arm64) and silently drop a fresh entry.
                val fresh = tasksLock.withLock { !task.aborted && task.status == DownloadStatus.PENDING }
                if (fresh) executeTaskAsync(task)
            } catch (e: Exception) {
                LogCat.e("Download task ${task.id} failed: ${e.message}")
                if (!task.aborted && !task.status.isTerminalDownloadStatus()) {
                    task.status = DownloadStatus.FAILED
                    task.error = e.message ?: (e::class.simpleName ?: "error")
                }
                updateProgressFlow()
            }
        }
    }

    /** How the previous engine execution ended, decided atomically under [tasksLock]. */
    private enum class ExecutionVerdict {
        /** pause/cancel/remove own the task now — skip finished bookkeeping. */
        ABORTED,

        /** The engine returned without a terminal status — treat as failure. */
        SILENT,

        /** Terminal status reported (or the task was re-enqueued since). */
        DONE,
    }

    private suspend fun executeTaskAsync(task: DownloadTaskHandle) {
        val engine = tasksLock.withLock { engines[task.kind] }
        if (engine == null) {
            LogCat.e("No engine registered for download kind ${task.kind}, failing ${task.id}")
            tasksLock.withLock {
                if (task.status == DownloadStatus.PENDING) {
                    task.error = "no engine registered for kind ${task.kind}"
                    task.status = DownloadStatus.FAILED
                }
            }
            updateProgressFlow()
            return
        }
        // Claim this execution atomically: every re-dispatch path (resume,
        // retry, requeue, enqueueUnique) also writes status under tasksLock,
        // so a claim that finds anything other than PENDING is a stale
        // channel entry and must not touch the task. The returned epoch
        // identifies THIS execution for the post-join verdict.
        val myEpoch = tasksLock.withLock {
            if (task.status != DownloadStatus.PENDING) {
                return@withLock 0L
            }
            task.status = DownloadStatus.DOWNLOADING
            task.aborted = false
            val epoch = (executions[task.id] ?: 0L) + 1L
            executions[task.id] = epoch
            epoch
        }
        if (myEpoch == 0L) return
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
        // Decide under the registry lock, together with every writer: an
        // unlocked read here races a re-enqueue (status back to PENDING) or a
        // pause and can wrongly flip a freshly requeued task to FAILED — the
        // re-dispatched entry is then dropped by the stale-entry check and
        // the task silently disappears.
        val verdict = tasksLock.withLock {
            when {
                // A newer execution already claimed this task (the requeue
                // raced this verdict): hands off, the fresh run owns the state.
                executions[task.id] != myEpoch -> ExecutionVerdict.DONE
                task.aborted -> ExecutionVerdict.ABORTED
                task.status == DownloadStatus.DOWNLOADING -> {
                    // Still the status this execution set and nobody requeued:
                    // the engine went silent.
                    task.status = DownloadStatus.FAILED
                    ExecutionVerdict.SILENT
                }
                else -> ExecutionVerdict.DONE
            }
        }
        if (verdict == ExecutionVerdict.ABORTED) return
        if (verdict == ExecutionVerdict.SILENT) {
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
