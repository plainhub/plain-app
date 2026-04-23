package com.ismartcoding.lib.kdataloader

open class DataLoaderOptions<K, R>(
    /**
     * The cache implementation
     */
    val cache: Cache<K, R>? = CoroutineMapCache(),

    /**
     * Cache Exceptional States?
     */
    val cacheExceptions: Boolean = true,

    /**
     * The batch-mode
     */
    val batchMode: BatchMode = BatchMode.LoadInBatch()
)
