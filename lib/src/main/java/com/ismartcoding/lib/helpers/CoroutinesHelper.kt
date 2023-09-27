package com.ismartcoding.lib.helpers

import kotlinx.coroutines.*

object CoroutinesHelper {
    suspend fun <T> withIO(block: suspend CoroutineScope.() -> T): T {
        return withContext(Dispatchers.IO, block)
    }

    fun coIO(runner: suspend CoroutineScope.() -> Unit) = CoroutineScope(Dispatchers.IO).launch { runner.invoke((this)) }

    fun coMain(runner: suspend CoroutineScope.() -> Unit) = CoroutineScope(Dispatchers.Main).launch { runner.invoke((this)) }

    suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> =
        coroutineScope {
            map { async { f(it) } }.awaitAll()
        }
}
