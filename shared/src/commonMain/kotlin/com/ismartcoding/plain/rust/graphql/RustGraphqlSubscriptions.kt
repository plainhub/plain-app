package com.ismartcoding.plain.rust.graphql

import com.ismartcoding.plain.models.PeerSubscriptionTarget
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.rust.native.RustNativeBindings
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object RustGraphqlSubscriptions {
    private val registry = Mutex()
    private val consumers = mutableMapOf<String, SendChannel<GraphqlResponse>>()
    private val reads = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private var handle: Long? = null
    private var sequence = 0L

    internal fun start(scope: CoroutineScope) {
        val opened = RustNativeBindings.graphqlSubscriptionOpen()
        handle = opened
        scope.launch {
            try {
                while (isActive) {
                    val encoded = read(opened)
                    val message = JsonHelper.jsonDecode<GraphqlSubscriptionMessage>(encoded)
                    val id = message.id ?: error("Subscription operation ID missing")
                    registry.withLock {
                        val consumer = consumers[id] ?: return@withLock
                        when (message.type) {
                            "next" -> {
                                val response = JsonHelper.jsonDecodeFromElement<GraphqlResponse>(requireNotNull(message.payload))
                                if (!consumer.trySend(response).isSuccess) {
                                    consumer.close(GraphqlSubscriptionException(listOf(GraphqlError("resync_required"))))
                                    consumers.remove(id)
                                    send(opened, GraphqlSubscriptionCommand.Complete(id))
                                }
                            }
                            "error" -> {
                                val errors = JsonHelper.jsonDecodeFromElement<List<GraphqlError>>(requireNotNull(message.payload))
                                consumer.close(GraphqlSubscriptionException(errors))
                                consumers.remove(id)
                            }
                            "complete" -> { consumer.close(); consumers.remove(id) }
                            else -> error("Unknown subscription message")
                        }
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                LogCat.e("GraphQL subscription reader", error)
                registry.withLock { consumers.values.forEach { it.close(error) }; consumers.clear() }
            } finally {
                withContext(NonCancellable) {
                    registry.withLock {
                        if (handle == opened) {
                            handle = null
                            consumers.values.forEach { it.close(IllegalStateException("Subscription session closed")) }
                            consumers.clear()
                        }
                    }
                    RustNativeBindings.graphqlSubscriptionClose(opened)
                }
            }
        }
    }

    internal fun subscribe(request: GraphqlOperation): Flow<GraphqlResponse> = callbackFlow {
        var operation: Pair<Long, String>? = null
        try {
            registry.withLock {
                val opened = checkNotNull(handle) { "GraphQL subscriptions not initialized" }
                val id = (++sequence).toString()
                consumers[id] = channel
                operation = opened to id
                send(opened, GraphqlSubscriptionCommand.Subscribe(id, request))
            }
            awaitClose()
        } finally {
            withContext(NonCancellable) {
                registry.withLock {
                    operation?.let { (opened, id) ->
                        if (consumers.remove(id) != null && handle == opened) send(opened, GraphqlSubscriptionCommand.Complete(id))
                    }
                }
            }
        }
    }
    internal fun subscribePeerUpdates(peer: com.ismartcoding.plain.models.Peer, requests: List<GraphqlOperation>): Flow<SubscriptionUpdate<GraphqlResponse>> = flow {
        val opened = RustNativeBindings.graphqlSubscriptionOpen(JsonHelper.jsonEncode(PeerSubscriptionTarget(peer.id, peer.publicKey)))
        val peerReads = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val operations = requests.mapIndexed { index, request -> (index + 1).toString() to request }.toMap()
            operations.forEach { (id, request) -> send(opened, GraphqlSubscriptionCommand.Subscribe(id, request)) }
            val active = operations.keys.toMutableSet()
            val attaching = operations.keys.toMutableSet()
            while (currentCoroutineContext().isActive && active.isNotEmpty()) {
                val message = JsonHelper.jsonDecode<GraphqlSubscriptionMessage>(read(opened, peerReads))
                check(message.id in active) { "Unexpected peer subscription operation" }
                when (message.type) {
                    "next" -> {
                        val response = JsonHelper.jsonDecodeFromElement<GraphqlResponse>(requireNotNull(message.payload))
                        if (response.errors.isNotEmpty()) throw GraphqlSubscriptionException(response.errors)
                        if (response.extensions?.get("subscriptionReady") == kotlinx.serialization.json.JsonPrimitive(true)) {
                            if (attaching.remove(message.id) && attaching.isEmpty()) emit(SubscriptionUpdate.Attached)
                        } else if (response.data != null) emit(SubscriptionUpdate.Data(response))
                    }
                    "error" -> throw GraphqlSubscriptionException(JsonHelper.jsonDecodeFromElement(requireNotNull(message.payload)))
                    "complete" -> active.remove(message.id)
                    else -> error("Unknown peer subscription message")
                }
            }
        } finally {
            RustNativeBindings.graphqlSubscriptionClose(opened)
            peerReads.cancel()
        }
    }
    private fun send(opened: Long, command: GraphqlSubscriptionCommand) {
        RustNativeBindings.graphqlSubscriptionSend(opened, JsonHelper.jsonEncode<GraphqlSubscriptionCommand>(command))
    }
    private suspend fun read(opened: Long, readerScope: CoroutineScope = reads): String = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { RustNativeBindings.graphqlSubscriptionClose(opened) }
        readerScope.launch {
            try { continuation.resume(RustNativeBindings.graphqlSubscriptionRead(opened)) }
            catch (error: Exception) { continuation.resumeWithException(error) }
        }
    }
}
