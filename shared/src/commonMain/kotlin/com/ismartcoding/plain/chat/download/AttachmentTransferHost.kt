package com.ismartcoding.plain.chat.download

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.chat.peer.RustPeerStore
import com.ismartcoding.plain.chat.peer.transport.PeerTransportRouter
import com.ismartcoding.plain.features.download.DownloadIo
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.PlatformLock
import com.ismartcoding.plain.platform.DownloadTempFileHandle
import com.ismartcoding.plain.platform.createFileWriteHandle
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

object AttachmentTransferHost {
    private val cleanup = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = PlatformLock()
    private val jobs = mutableMapOf<String, Job>()

    suspend fun handle(scope: CoroutineScope, method: String, params: JsonObject): JsonElement {
        when (method) {
            "attachmentTransferStart" -> {
                val task = params.getValue("task").jsonObject
                val token = task.getValue("generation").jsonPrimitive.content
                var entered = false
                val job = scope.launch(start = CoroutineStart.LAZY) {
                    lock.withLock { entered = true }
                    transfer(task, params.getValue("path").jsonPrimitive.content)
                }
                val accepted = lock.withLock {
                    if (jobs.containsKey(token)) false else { jobs[token] = job; true }
                }
                if (accepted) {
                    job.invokeOnCompletion {
                        val neverStarted = lock.withLock {
                            if (jobs[token] === job) jobs.remove(token)
                            !entered
                        }
                        if (neverStarted) cleanup.launch {
                            runCatching { call(buildJsonObject {
                                put("action", "finish"); put("id", task.getValue("id")); put("generation", token)
                                put("error", "Attachment driver canceled before start")
                            }) }
                        }
                    }
                    job.start()
                } else job.cancel()
                return JsonPrimitive(true)
            }
            "attachmentTransferCancel" -> {
                val job = lock.withLock { jobs[params.getValue("token").jsonPrimitive.content] }
                job?.cancelAndJoin()
                return JsonPrimitive(true)
            }
            else -> error("Unknown attachment driver operation")
        }
    }

    private suspend fun transfer(task: JsonObject, path: String) {
        val id = task.getValue("id").jsonPrimitive.content
        val generation = task.getValue("generation").jsonPrimitive.content
        var handle: DownloadTempFileHandle? = null
        var closed = false
        var failure: String? = null
        try {
            DownloadIo.run {
                val peer = RustPeerStore.decode(task.getValue("peer"))
                val fileId = task.getValue("file").jsonObject.getValue("uri").jsonPrimitive.content.removePrefix("fsid:")
                val temp = createFileWriteHandle(path)
                handle = temp
                PeerTransportRouter.downloadFile(peer, fileId).use { response ->
                    check(response.status in 200..299) { "HTTP ${response.status}" }
                    val buffer = ByteArray(8192)
                    var bytes = 0L
                    var lastProgress = TimeHelper.nowMillis()
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = response.channel.readAvailable(buffer)
                        if (read == -1) break
                        temp.write(buffer, 0, read)
                        bytes += read
                        val now = TimeHelper.nowMillis()
                        if (now - lastProgress >= 1000) {
                            val active = call(buildJsonObject {
                                put("action", "progress"); put("id", id); put("generation", generation); put("downloaded", bytes)
                            }).jsonPrimitive.boolean
                            check(active) { "Attachment transfer expired" }
                            lastProgress = now
                        }
                        yield()
                    }
                }
                temp.close()
                closed = true
            }
        } catch (cancelled: CancellationException) {
            failure = "Attachment driver disconnected or canceled"
            throw cancelled
        } catch (error: Exception) {
            failure = error.message ?: "Attachment transfer failed"
        } finally {
            if (!closed) runCatching { handle?.close() }.onFailure { if (failure == null) failure = it.message ?: "File close failed" }
            withContext(NonCancellable) {
                try { call(buildJsonObject {
                    put("action", "finish"); put("id", id); put("generation", generation)
                    put("error", failure?.let(::JsonPrimitive) ?: JsonNull)
                }) } catch (error: Exception) { LogCat.e("Attachment transfer receipt", error) }
                runCatching { handle?.delete() }.onFailure { LogCat.e("Attachment temporary cleanup", it) }
            }
        }
    }
    private suspend fun call(body: JsonObject): JsonElement =
        RustContentApi.postJson("chat/download", body, longRunning = true).getValue("result")
}
