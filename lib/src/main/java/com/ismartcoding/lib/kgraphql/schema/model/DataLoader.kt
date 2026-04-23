package com.ismartcoding.lib.kgraphql.schema.model

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class DataLoader<K, V>(private val batchLoader: suspend (List<K>) -> Map<K, V?>) {

    inner class DataScope(totalTimes: Int, scope: CoroutineScope) : CoroutineScope by scope {
        private val actor = dataActor(totalTimes, batchLoader)

        suspend fun load(key: K, valueResult: CompletableDeferred<Any?>) {
            actor.send(Add(key, valueResult))
        }

    }

    fun begin(totalTimes: Int, scope: CoroutineScope): DataScope {
        return DataScope(totalTimes, scope)
    }
}

sealed class DataActor
class Add<K, V : Any?>(val key: K, val result: CompletableDeferred<V?>) : DataActor()
class Increment(val count: Int) : DataActor()

@ObsoleteCoroutinesApi
fun <K, V> CoroutineScope.dataActor(totalTimes: Int, batchLoader: suspend (List<K>) -> Map<K, V?>) = actor<DataActor> {
    var counter = totalTimes

    log("Starting dataActor with totalCount: $counter")

    val cache = mutableMapOf<K, V?>()
    val promiseMap = mutableMapOf<K, Stack<CompletableDeferred<V?>>>()

    suspend fun doJoin() {
        val toLoad = promiseMap
            .map { it.key }
            .filterNot { cache.containsKey(it) }
            .toList()
        if (toLoad.isNotEmpty()) {
            batchLoader(toLoad).forEach { (key, value) ->
                cache[key] = value
            }
        }
        promiseMap.forEach { (key, promises) ->
            var promise: CompletableDeferred<V?>? = promises.pop()
            while (promise != null) {
                promise.complete(cache[key])
                promise = if (promises.isNotEmpty()) promises.pop() else null
            }
        }
        promiseMap.clear()
    }

    for (msg in channel) {
        if (msg is Add<*, *>) {
            msg as Add<K, V>
            if (!promiseMap.containsKey(msg.key)) promiseMap[msg.key] = Stack()
            promiseMap[msg.key]?.add(msg.result) ?: throw TODO("Couldn't find any '${msg.key}' in map")
            log("$counter")
            if (--counter == 0) doJoin()
        }
    }
}


fun log(str: String) = println("DATALOADER: $str")
