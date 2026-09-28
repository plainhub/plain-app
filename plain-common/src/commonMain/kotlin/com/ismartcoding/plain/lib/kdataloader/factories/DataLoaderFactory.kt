package com.ismartcoding.plain.lib.kdataloader.factories

import kotlinx.coroutines.Job
import com.ismartcoding.plain.lib.kdataloader.*
import com.ismartcoding.plain.lib.kdataloader.BatchLoader
import com.ismartcoding.plain.lib.kdataloader.DataLoader
import com.ismartcoding.plain.lib.kdataloader.DataLoaderOptions
import com.ismartcoding.plain.lib.kdataloader.ExecutionResult
import com.ismartcoding.plain.lib.kdataloader.prime
import kotlin.coroutines.CoroutineContext

typealias DataLoaderFactoryMethod<K, R> = (options: DataLoaderOptions<K, R>, batchLoader: BatchLoader<K, R>, parent: Job?) -> DataLoader<K, R>

open class DataLoaderFactory<K, R>(
    @Suppress("MemberVisibilityCanBePrivate")
    protected val optionsFactory: () -> DataLoaderOptions<K, R>,
    @Suppress("MemberVisibilityCanBePrivate")
    protected val batchLoader: BatchLoader<K, R>,
    @Suppress("MemberVisibilityCanBePrivate")
    protected val cachePrimes: Map<K, ExecutionResult<R>>,
    protected val factoryMethod: DataLoaderFactoryMethod<K, R>
) {

    suspend fun constructNew(parent: Job?): DataLoader<K, R> {
        val dataLoader = factoryMethod(optionsFactory(), batchLoader, parent)
        cachePrimes.forEach { (key, value) -> dataLoader.prime(key, value) }
        return dataLoader
    }
}
