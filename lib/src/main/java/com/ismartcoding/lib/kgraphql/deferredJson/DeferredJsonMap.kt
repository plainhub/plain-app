package com.ismartcoding.lib.kgraphql.deferredJson

import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.CoroutineContext

public class DeferredJsonMap internal constructor(
    ctx: CoroutineContext,
): CoroutineScope {

    internal val job = Job()
    override val coroutineContext: CoroutineContext = ctx + job

    private val dmLock = Mutex()
    private val deferredMap = mutableMapOf<String, Deferred<JsonElement>>()
    private val uddmLock = Mutex()
    private val unDefinedDeferredMap = mutableMapOf<String, DeferredJsonMap>()
    private val mjLock = Mutex()
    private val moreJobs = mutableListOf<Deferred<Unit>>()

    private var completedMap: Map<String, JsonElement>? = null

    public suspend infix fun String.toValue(element: JsonElement) {
        this toDeferredValue CompletableDeferred(element)
    }

    public suspend infix fun String.toDeferredValue(element: Deferred<JsonElement>): Unit = dmLock.withLock {
        deferredMap[this] = element
    }

    public suspend infix fun String.toDeferredObj(block: suspend DeferredJsonMap.() -> Unit) {
        val mapToUse = uddmLock.withLock {
            val map = unDefinedDeferredMap[this]
            if (map == null) {
                val newMap = DeferredJsonMap(job)
                unDefinedDeferredMap[this] = newMap
                newMap
            } else map
        }
        block(mapToUse)
    }

    public suspend infix fun String.toDeferredArray(block: suspend DeferredJsonArray.() -> Unit) {
        val array = DeferredJsonArray(job)
        block(array)
        this@toDeferredArray toDeferredValue array.asDeferred()
    }

    private fun asDeferredAsync(startIn: Job = job): Deferred<JsonElement> = async(startIn, LAZY) {
        awaitAll()
        build()
    }

    private suspend fun awaitAll() {
        if (completedMap != null) return

        do {
            moreJobs.awaitAll()
            unDefinedDeferredMap.map { (key, map) ->
                key toDeferredValue map.asDeferredAsync(job)
            }
         } while (moreJobs.any { !it.isCompleted })

        job.children.toList().joinAll()
        job.complete()
        job.join()
        completedMap = deferredMap.mapValues { it.value.await() }
    }

    @Suppress("SuspendFunctionOnCoroutineScope")
    public suspend fun deferredLaunch(block: suspend DeferredJsonMap.() -> Unit): Unit = mjLock.withLock {
        moreJobs.add(async(job, LAZY) {
            block(this@DeferredJsonMap)
        })
    }

    private fun build(): JsonObject {
        checkNotNull(completedMap) { "The deferred tree has not been awaited!" }
        check(job.isCompleted) { "Please call 'awaitAll' before calling this" }
        return JsonObject(completedMap!!)
    }

    public suspend fun awaitAndBuild():JsonObject { awaitAll(); return build() }
}
