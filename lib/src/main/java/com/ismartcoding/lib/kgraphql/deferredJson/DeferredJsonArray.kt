package com.ismartcoding.lib.kgraphql.deferredJson

import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlin.coroutines.CoroutineContext


@Suppress("SuspendFunctionOnCoroutineScope")
public class DeferredJsonArray internal constructor(
    ctx: CoroutineContext
): CoroutineScope, Mutex by Mutex() {

    private val job = Job()
    override val coroutineContext: CoroutineContext = ctx + job

    private val deferredArray = mutableListOf<Deferred<JsonElement>>()
    private var completedArray: List<JsonElement>? = null

    public suspend fun addValue(element: JsonElement) {
        addDeferredValue(CompletableDeferred(element))
    }

    public suspend fun addDeferredValue(element: Deferred<JsonElement>): Unit = withLock {
        deferredArray.add(element)
    }

    public suspend fun addDeferredObj(block: suspend DeferredJsonMap.() -> Unit) {
        val map = DeferredJsonMap(job)
        block(map)
        addDeferredValue(async(job, LAZY) { map.awaitAndBuild() })
    }

    public suspend fun addDeferredArray(block: suspend DeferredJsonArray.() -> Unit) {
        val array = DeferredJsonArray(job)
        block(array)
        addDeferredValue(array.asDeferred())
    }

    internal fun asDeferred() : Deferred<JsonElement> {
        return async(job, LAZY) {
            awaitAll()
            build()
        }
    }

    public suspend fun awaitAll() {
        check(completedArray == null) { "The deferred tree has already been awaited!" }
        completedArray = deferredArray.awaitAll()
        job.complete()
    }

    public fun build(): JsonArray {
        checkNotNull(completedArray) { "The deferred tree has not been awaited!" }
        return JsonArray(completedArray!!)
    }
}
