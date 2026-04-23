package com.ismartcoding.lib.kdataloader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlin.jvm.JvmName
import com.ismartcoding.lib.kdataloader.statistics.DataLoaderStatistics

typealias BatchLoader<K, R> = suspend (ids: List<K>) -> List<ExecutionResult<R>>

interface DataLoader<K, R> {

    val options: DataLoaderOptions<K, R>

    /**
     * Loads the value for the given Key.
     * The returned [Deferred] completes with the finish of [dispatch] in case of [DataLoaderOptions.batchLoadEnabled] = true.
     * If [DataLoaderOptions.batchLoadEnabled] = false it calls the BatchLoader immediately and returns the retrieved value.
     */
    suspend fun loadAsync(key: K): Deferred<R>

    /**
     * The same as [loadAsync] but for multiple Keys at once.
     */
    suspend fun loadManyAsync(vararg keys: K): Deferred<List<R>>

    /**
     * Executes all stored requests via the given batchLoader.
     * After this function finishes all [Deferred] created before are completed.
     */
    suspend fun dispatch()

    /**
     * Removes the value of the given Key from the cache
     */
    suspend fun clear(key: K)

    /**
     * Removes all values from the cache
     */
    suspend fun clearAll()

    /**
     * Primes the cache with the given values.
     * After priming the [BatchLoader] will not be called with this key.
     */
    suspend fun prime(key: K, value: R)

    /**
     * Primes the cache with the given [Throwable].
     * After priming the [BatchLoader] will not be called with this key, if [DataLoaderOptions.cacheExceptions] = true.
     */
    suspend fun prime(key: K, value: Throwable)


    /**
     * Returns a snapshot of the statistics at the point of calling
     */
    suspend fun createStatisticsSnapshot(): DataLoaderStatistics

}

/**
 * @see DataLoader.prime(K, R)
 */
suspend fun <K, R> SimpleDataLoaderImpl<K, R>.prime(cacheEntry: Pair<K, R>) {
    prime(cacheEntry.first, cacheEntry.second)
}

/**
 * @see DataLoader.prime(K, Throwable)
 */
@JvmName("primeFailure")
suspend fun <K, R> SimpleDataLoaderImpl<K, R>.prime(cacheEntry: Pair<K, Throwable>) {
    prime(cacheEntry.first, cacheEntry.second)
}

internal suspend fun <K, R> DataLoader<K, R>.prime(key: K, value: ExecutionResult<R>) =
    when (value) {
        is ExecutionResult.Success -> prime(key, value.value)
        is ExecutionResult.Failure -> prime(key, value.throwable)
    }
