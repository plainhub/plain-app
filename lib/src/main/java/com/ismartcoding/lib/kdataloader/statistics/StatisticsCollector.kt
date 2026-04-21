package com.ismartcoding.lib.kdataloader.statistics

import kotlinx.coroutines.Deferred

interface StatisticsCollector {

    suspend fun incLoadAsyncMethodCalledAsync(): Deferred<Long>
    suspend fun incLoadManyAsyncMethodCalledAsync(): Deferred<Long>
    suspend fun incDispatchMethodCalledAsync(): Deferred<Long>
    suspend fun incClearMethodCalledAsync(): Deferred<Long>
    suspend fun incClearAllMethodCalledAsync(): Deferred<Long>
    suspend fun incPrimeMethodCalledAsync(): Deferred<Long>

    /**
     * Increment by the number of requested objects
     */
    suspend fun incObjectsRequestedAsync(objectCount: Long = 1): Deferred<Long>

    suspend fun incBatchCallsExecutedAsync(): Deferred<Long>

    suspend fun incCacheHitCountAsync(): Deferred<Long>

    /**
     * returns a immutable copy of the statistics at the point of calling this method
     */
    suspend fun createStatisticsSnapshot(): DataLoaderStatistics
}
