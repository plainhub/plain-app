package com.ismartcoding.lib.kdataloader

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CoroutineMapCache<K, V>(
    private val cacheMap: MutableMap<K, CompletableDeferred<V>> = mutableMapOf()
) : Cache<K, V> {
    private val mutex = Mutex()


    override suspend fun store(key: K, value: CompletableDeferred<V>): CompletableDeferred<V> {
        mutex.withLock {
            cacheMap[key] = value
        }
        return value
    }

    override suspend fun get(key: K): CompletableDeferred<V>? =
        mutex.withLock {
            cacheMap[key]
        }

    override suspend fun getOrCreate(
        key: K,
        generator: suspend (key: K) -> CompletableDeferred<V>,
        callOnCacheHit: suspend () -> Unit
    ): CompletableDeferred<V> =
        mutex.withLock {
            val currentVal = cacheMap[key]
            if (currentVal == null) {
                val generated = generator(key)
                cacheMap[key] = generated
                return@withLock generated
            } else {
                callOnCacheHit()
                return@withLock currentVal
            }
        }


    override suspend fun clear(key: K): CompletableDeferred<V>? =
        mutex.withLock {
            cacheMap.remove(key)
        }

    override suspend fun clear() =
        mutex.withLock {
            cacheMap.clear()
        }


}
