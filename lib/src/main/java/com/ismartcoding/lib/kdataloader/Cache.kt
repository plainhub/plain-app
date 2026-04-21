package com.ismartcoding.lib.kdataloader

import kotlinx.coroutines.CompletableDeferred

/**
 * A "Threadsafe" Cache
 * (Coroutine-Save)
 */
interface Cache<K, V> {

    suspend fun store(key: K, value: CompletableDeferred<V>): CompletableDeferred<V>

    suspend fun get(key: K): CompletableDeferred<V>?

    suspend fun getOrCreate(
        key: K,
        generator: suspend (key: K) -> CompletableDeferred<V>,
        callOnCacheHit: suspend () -> Unit
    ): CompletableDeferred<V>

    suspend fun clear(key: K): CompletableDeferred<V>?

    suspend fun clear()
}

suspend fun <K, V> Cache<K, V>.getOrCreate(
    key: K,
    generator: suspend (key: K) -> CompletableDeferred<V>
): CompletableDeferred<V> =
    getOrCreate(key, generator, {})
