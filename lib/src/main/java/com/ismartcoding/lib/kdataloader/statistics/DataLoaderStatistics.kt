package com.ismartcoding.lib.kdataloader.statistics

data class DataLoaderStatistics(
    val loadAsyncMethodCalled: Long = 0,
    val loadManyAsyncMethodCalled: Long = 0,
    val dispatchMethodCalled: Long = 0,
    val clearMethodCalled: Long = 0,
    val clearAllMethodCalled: Long = 0,
    val primeMethodCalled: Long = 0,

    /**
     * Contains the count of all Objects requested via loadAsync or loadAsyncMany
     */
    val objectsRequested: Long = 0,
    val batchCallsExecuted: Long = 0,

    val cacheHitCount: Long = 0
)
