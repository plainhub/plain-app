package com.ismartcoding.plain.features.download

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object DownloadIo {
    private val capacity = Semaphore(3)
    suspend fun <T> run(block: suspend () -> T): T = capacity.withPermit { block() }
}
