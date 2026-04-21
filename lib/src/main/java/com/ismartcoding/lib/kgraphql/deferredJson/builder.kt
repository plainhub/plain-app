package com.ismartcoding.lib.kgraphql.deferredJson

import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject

public fun CoroutineScope.deferredJsonBuilder(
    timeout: Long? = null,
    init: suspend DeferredJsonMap.() -> Unit
): Deferred<JsonObject> = async {
    val block: suspend CoroutineScope.() -> JsonObject = {
        try {
            val builder = DeferredJsonMap(coroutineContext)
            builder.init()
            builder.awaitAndBuild()
        } catch (e: CancellationException) {
            throw e.cause ?: e
        }
    }

    timeout?.let {
        withTimeout(it) {
            block()
        }
    } ?: block()
}
