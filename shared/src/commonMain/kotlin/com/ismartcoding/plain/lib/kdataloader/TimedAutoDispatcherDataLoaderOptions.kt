package com.ismartcoding.plain.lib.kdataloader

class TimedAutoDispatcherDataLoaderOptions<K, R>(
    val waitInterval: Long = 100,
    cache: Cache<K, R>? = CoroutineMapCache(),
    cacheExceptions: Boolean = true,
    batchMode: BatchMode = BatchMode.LoadInBatch()
): DataLoaderOptions<K, R>(cache, cacheExceptions, batchMode)
