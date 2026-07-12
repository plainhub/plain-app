package com.ismartcoding.plain.lib.kgraphql.schema.model

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class DataLoader<K, V>(private val batchLoader: suspend (List<K>) -> Map<K, V?>) {

    inner class DataScope(totalTimes: Int, scope: CoroutineScope) : CoroutineScope by scope {
        private val actor = dataActor(totalTimes, batchLoader, this)

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

fun <K, V> dataActor(totalTimes: Int, batchLoader: suspend (List<K>) -> Map<K, V?>, scope: CoroutineScope): Channel<DataActor> {
    val channel = Channel<DataActor>()
    scope.launch {
        var counter = totalTimes

        log("Starting dataActor with totalCount: $counter")

        val cache = mutableMapOf<K, V?>()
        val promiseMap = mutableMapOf<K, ArrayDeque<CompletableDeferred<V?>>>()

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
                var promise: CompletableDeferred<V?>? = if (promises.isNotEmpty()) promises.removeLast() else null
                while (promise != null) {
                    promise.complete(cache[key])
                    promise = if (promises.isNotEmpty()) promises.removeLast() else null
                }
            }
            promiseMap.clear()
        }

        for (msg in channel) {
            if (msg is Add<*, *>) {
                msg as Add<K, V>
                if (!promiseMap.containsKey(msg.key)) promiseMap[msg.key] = ArrayDeque()
                promiseMap[msg.key]?.add(msg.result) ?: throw TODO("Couldn't find any '${msg.key}' in map")
                log("$counter")
                if (--counter == 0) doJoin()
            }
        }
    }
    return channel
}


fun log(str: String) = println("DATALOADER: $str")
