package com.ismartcoding.lib.kdataloader

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import com.ismartcoding.lib.kdataloader.statistics.SimpleStatisticsCollector

class TimedAutoDispatcherImpl<K, R>(
    options: TimedAutoDispatcherDataLoaderOptions<K, R>,
    batchLoader: BatchLoader<K, R>,
    parent: Job? = null,
) : SimpleDataLoaderImpl<K, R>(options, SimpleStatisticsCollector(), batchLoader), CoroutineScope {

    private val autoChannel = Channel<Unit>()
    override val coroutineContext = Job(parent)

    val dataLoaderDispatcher = newSingleThreadContext("CounterContext")

    init {
        launch(CoroutineName("TimedAutoDispatcherImpl:init")) {
            var job: Job? = null
            while (true) {
                autoChannel.receive()
//                println("TimedAutoDispatcherImpl:message")
                if (job?.isActive == true) job.cancelAndJoin()
                job = launch(CoroutineName("TimedAutoDispatcherImpl:autoChannel") + coroutineContext) {
                    delay(options.waitInterval)
                    if (isActive) launch {
                        dispatch()
                    }
                }
            }
        }
    }

    suspend fun cancel() {
        coroutineContext.cancel()
        autoChannel.close()
        dispatch()
    }

    override suspend fun loadAsync(key: K): Deferred<R> {
        return super.loadAsync(key).also { autoChannel.send(Unit) }
    }

    override suspend fun loadManyAsync(vararg keys: K): Deferred<List<R>> {
        return super.loadManyAsync(*keys).also { autoChannel.send(Unit) }
    }

    override suspend fun clear(key: K) {
        super.clear(key).also { autoChannel.send(Unit) }
    }

    override suspend fun clearAll() {
        super.clearAll().also { autoChannel.send(Unit) }
    }

    override suspend fun prime(key: K, value: R) {
        super.prime(key, value).also { autoChannel.send(Unit) }
    }

    override suspend fun prime(key: K, value: Throwable){
        super.prime(key, value).also { autoChannel.send(Unit) }
    }

}
