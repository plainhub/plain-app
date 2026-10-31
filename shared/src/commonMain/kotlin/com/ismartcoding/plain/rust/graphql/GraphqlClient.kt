package com.ismartcoding.plain.rust.graphql

import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CancellationException
import kotlin.time.TimeSource

open class GraphqlClient internal constructor(
    private val transport: String = "native",
    private val send: suspend (GraphqlOperation) -> GraphqlResponse,
) {
    suspend fun <T> execute(request: GraphqlRequest<T>): T {
        val started = TimeSource.Monotonic.markNow()
        val label = "GraphQL transport=$transport operation=${request.operation.operationName ?: request.field ?: "anonymous"}"
        try {
            val response = send(request.operation)
            if (response.errors.isNotEmpty()) {
                error(response.errors.joinToString { it.message })
            }
            val data = checkNotNull(response.data) { "GraphQL response has no data" }
            val result = request.decode(data)
            LogCat.d("$label result=success elapsedMs=${started.elapsedNow().inWholeMilliseconds} query=${request.operation.query}")
            return result
        } catch (error: CancellationException) {
            LogCat.d("$label result=cancelled elapsedMs=${started.elapsedNow().inWholeMilliseconds} query=${request.operation.query}")
            throw error
        } catch (error: Exception) {
            LogCat.e("$label result=error error=${error::class.simpleName} elapsedMs=${started.elapsedNow().inWholeMilliseconds} query=${request.operation.query}")
            throw error
        }
    }
}
