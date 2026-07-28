package com.ismartcoding.plain.platform

import com.ismartcoding.plain.events.MediaDurationZeroEvent
import com.ismartcoding.plain.events.MediaDurationZeroItem
import com.ismartcoding.plain.helpers.coIO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock

/**
 * Async queue for fixing zero-duration media items.
 *
 * Uses a [Channel] as the task queue: when empty, the worker coroutine suspends
 * on [receive][Channel.receive] without consuming CPU — no polling, no timer,
 * no idle overhead. A [Semaphore] caps concurrency at [MAX_CONCURRENCY]. A
 * [Mutex]-guarded [Set] deduplicates by file path so rapid list refreshes
 * don't re-enqueue the same item.
 *
 * Call [start] once at app startup (from [com.ismartcoding.plain.events.AppEvents.register]).
 * Producers call [enqueue] when they encounter zero-duration items.
 */
object MediaDurationFixQueue {
    private const val MAX_CONCURRENCY = 10

    private data class Task(
        val mediaType: String,
        val item: MediaDurationZeroItem,
    )

    private val channel = Channel<Task>(capacity = Channel.UNLIMITED)
    private val mutex = Mutex()
    private val processing = mutableSetOf<String>()

    // MutableStateFlow.compareAndSet provides the same atomic check-and-set
    // semantics as @Volatile + synchronized, but works on both JVM and Native.
    private val started = MutableStateFlow(false)

    /**
     * Start the background worker. Safe to call multiple times — only the first
     * call has effect. The worker runs for the app lifetime; when the channel is
     * empty it simply suspends.
     */
    fun start() {
        if (!started.compareAndSet(false, true)) return
        coIO {
            val semaphore = Semaphore(MAX_CONCURRENCY)
            while (true) {
                val task = channel.receive()
                // Block here when 10 tasks are already running; the suspended
                // coroutine consumes no CPU while waiting.
                semaphore.acquire()
                launch {
                    try {
                        processSingleDurationZero(task.mediaType, task.item)
                    } finally {
                        semaphore.release()
                        mutex.withLock { processing.remove(task.item.path) }
                    }
                }
            }
        }
    }

    /**
     * Enqueue zero-duration items for async processing. Non-blocking — items
     * are sent to the channel in a background coroutine. Deduplicates by path:
     * items already queued or being processed are skipped.
     */
    fun enqueue(event: MediaDurationZeroEvent) {
        coIO {
            event.items.forEach { item ->
                val added = mutex.withLock { processing.add(item.path) }
                if (added) {
                    channel.send(Task(event.mediaType, item))
                }
            }
        }
    }
}
