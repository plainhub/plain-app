package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.native.RustNativeApi
import com.ismartcoding.plain.ui.nearby.NearbyViewModel
import kotlin.concurrent.Volatile
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object RustMdnsRuntime {
    private class Command(
        val request: Request,
        val reply: CompletableDeferred<MdnsSnapshot>,
    )
    private val commands = Channel<Command>(64)
    private val projection = Mutex()
    @Volatile private var current: MdnsSnapshot? = null
    val running: Boolean get() = current?.running ?: false
    val scanning: Boolean get() = current?.scanning ?: false

    init {
        coIO {
            for (command in commands) {
                try {
                    val row = call(command.request)
                    apply(row)
                    command.reply.complete(row)
                } catch (cancelled: CancellationException) {
                    command.reply.cancel(cancelled)
                    throw cancelled
                } catch (error: Throwable) {
                    command.reply.completeExceptionally(error)
                    LogCat.e("Rust mDNS control", error)
                }
            }
        }
    }

    private fun enqueue(request: Request): CompletableDeferred<MdnsSnapshot> {
        val reply = CompletableDeferred<MdnsSnapshot>()
        if (!commands.trySend(Command(request, reply)).isSuccess) {
            val error = IllegalStateException("mDNS control queue is full")
            reply.completeExceptionally(error)
            LogCat.e("Rust mDNS control", error)
        }
        return reply
    }

    suspend fun debugStart(token: String) = enqueue(Request.DebugStart(token)).await()

    fun debugStop(token: String) = enqueue(Request.DebugStop(token))

    suspend fun snapshot(): MdnsSnapshot =
        GraphqlApi.execute(Query.mdnsState()).also { apply(it) }

    private suspend fun call(request: Request): MdnsSnapshot = when (request) {
        is Request.DebugStart -> RustNativeApi.acquireMdnsDebugSession(request.token)
        is Request.DebugStop -> RustNativeApi.releaseMdnsDebugSession(request.token)
    }

    private sealed interface Request {
        data class DebugStart(val token: String) : Request
        data class DebugStop(val token: String) : Request
    }

    private suspend fun apply(next: MdnsSnapshot) = projection.withLock {
        val previous = current
        if (previous != null && next.runtimeId == previous.runtimeId && next.revision < previous.revision) return@withLock
        current = next
        NearbyViewModel.isDiscovering.value = scanning
    }

    suspend fun refresh() {
        snapshot()
    }

}
