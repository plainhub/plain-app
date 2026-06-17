package com.ismartcoding.plain.helpers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

suspend fun <T> withIO(block: suspend CoroutineScope.() -> T): T {
    return withContext(IODispatcher, block)
}

fun coIO(runner: suspend CoroutineScope.() -> Unit) = CoroutineScope(IODispatcher).launch { runner.invoke(this) }

fun coMain(runner: suspend CoroutineScope.() -> Unit) = CoroutineScope(Dispatchers.Main).launch { runner.invoke(this) }

suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> =
    coroutineScope {
        map { async { f(it) } }.awaitAll()
    }