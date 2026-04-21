package com.ismartcoding.lib.kdataloader

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultLoaderQueueImpl<K, V> : LoaderQueue<K, V> {

    private val mutex = Mutex()
    private var queue: MutableList<LoaderQueueEntry<K, CompletableDeferred<V>>> = mutableListOf()


    override suspend fun enqueue(key: K, deferred: CompletableDeferred<V>) {
        mutex.withLock {
            queue.add(LoaderQueueEntry(key, deferred))
        }
    }

    override suspend fun getAllItemsAsList(): List<LoaderQueueEntry<K, CompletableDeferred<V>>> =
        mutex.withLock {
            val currentQueue = queue
            queue = mutableListOf()
            return@withLock currentQueue
        }


}
