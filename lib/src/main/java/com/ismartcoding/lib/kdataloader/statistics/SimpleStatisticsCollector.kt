package com.ismartcoding.lib.kdataloader.statistics

import kotlinx.coroutines.*

class SimpleStatisticsCollector : StatisticsCollector {

    private var loadAsyncMethodCalled: Long = 0
    private var loadAsyncManyMethodCalled: Long = 0
    private var dispatchMethodCalled: Long = 0
    private var clearMethodCalled: Long = 0
    private var clearAllMethodCalled: Long = 0
    private var primeMethodCalled: Long = 0
    private var objectsRequested: Long = 0
    private var batchCallsExecuted: Long = 0
    private var cacheHitCount: Long = 0

    override suspend fun incLoadAsyncMethodCalledAsync() =
        CompletableDeferred(++loadAsyncMethodCalled)

    override suspend fun incLoadManyAsyncMethodCalledAsync() =
        CompletableDeferred(++loadAsyncManyMethodCalled)

    override suspend fun incDispatchMethodCalledAsync() =
        CompletableDeferred(++dispatchMethodCalled)

    override suspend fun incClearMethodCalledAsync() =
        CompletableDeferred(++clearMethodCalled)

    override suspend fun incClearAllMethodCalledAsync() =
        CompletableDeferred(++clearAllMethodCalled)

    override suspend fun incPrimeMethodCalledAsync() =
        CompletableDeferred(++primeMethodCalled)

    override suspend fun incObjectsRequestedAsync(objectCount: Long): Deferred<Long> {
        objectsRequested += objectCount
        return CompletableDeferred(objectsRequested)
    }


    override suspend fun incBatchCallsExecutedAsync() =
        CompletableDeferred(++batchCallsExecuted)

    override suspend fun incCacheHitCountAsync() =
        CompletableDeferred(++cacheHitCount)

    override suspend fun createStatisticsSnapshot(): DataLoaderStatistics =
        DataLoaderStatistics(
            loadAsyncMethodCalled = this.loadAsyncMethodCalled,
            loadManyAsyncMethodCalled = this.loadAsyncManyMethodCalled,
            dispatchMethodCalled = this.dispatchMethodCalled,
            clearMethodCalled = this.clearMethodCalled,
            clearAllMethodCalled = this.clearAllMethodCalled,
            primeMethodCalled = this.primeMethodCalled,
            objectsRequested = this.objectsRequested,
            batchCallsExecuted = this.batchCallsExecuted,
            cacheHitCount = this.cacheHitCount
        )
}
