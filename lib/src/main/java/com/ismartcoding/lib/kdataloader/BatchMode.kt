package com.ismartcoding.lib.kdataloader

sealed class BatchMode {
    /**
     * Load data in batches of [batchSize]
     */
    data class LoadInBatch(val batchSize: Int? = null) : BatchMode()

    /**
     * Load everything immediately
     */
    object LoadImmediately : BatchMode()
}
