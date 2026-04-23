package com.ismartcoding.lib.kdataloader.factories

import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import com.ismartcoding.lib.kdataloader.BatchLoader
import com.ismartcoding.lib.kdataloader.DataLoaderOptions
import com.ismartcoding.lib.kdataloader.ExecutionResult
import com.ismartcoding.lib.kdataloader.TimedAutoDispatcherImpl
import com.ismartcoding.lib.kdataloader.TimedAutoDispatcherDataLoaderOptions
import kotlin.coroutines.CoroutineContext

class TimedAutoDispatcherDataLoaderFactory<K, R>(
    optionsFactory: () -> TimedAutoDispatcherDataLoaderOptions<K, R>,
    cachePrimes: Map<K, ExecutionResult<R>>,
    batchLoader: BatchLoader<K, R>,
) : DataLoaderFactory<K, R>(optionsFactory, batchLoader, cachePrimes, { _: DataLoaderOptions<K, R>, bl: BatchLoader<K, R>, parent ->
    TimedAutoDispatcherImpl(optionsFactory(), bl, null)
})
