package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.features.download.DownloadIo
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

object SharedTransferHost {
    private val lock = PlatformLock()
    private val jobs = mutableMapOf<String, Pair<String, Job>>()
    private val temporary = mutableMapOf<String, MutableList<DownloadTempFileHandle>>()
    private val cleanup = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun handle(scope: CoroutineScope, method: String, params: JsonObject): JsonElement {
        val generation = params.getValue("generation").jsonPrimitive.content
        if (method == "sharedTransferCancel") {
            val current = lock.withLock { jobs.values.filter { it.first == generation }.map { it.second } }
            current.forEach { it.cancelAndJoin() }
            val handles = lock.withLock { temporary[generation]?.toList().orEmpty() }
            DownloadIo.run { handles.forEach { it.delete() } }
            lock.withLock { temporary.remove(generation) }
            return JsonPrimitive(true)
        }
        check(method == "sharedTransferStart") { "Unknown shared OS operation" }
        val ticket = params.getValue("ticket").jsonPrimitive.content
        var entered = false
        val job = scope.launch(start = CoroutineStart.LAZY) { lock.withLock { entered = true }; transfer(params) }
        val accepted = lock.withLock { if (jobs.containsKey(ticket)) false else { jobs[ticket] = generation to job; true } }
        if (accepted) {
            job.invokeOnCompletion {
                val neverStarted = lock.withLock { if (jobs[ticket]?.second === job) jobs.remove(ticket); !entered }
                if (neverStarted) cleanup.launch {
                    runCatching { call(params, "receipt") { put("receipt", buildJsonObject {
                        put("path", ""); put("bytes", 0); put("error", "Shared OS adapter canceled before start")
                    }) } }.onFailure { LogCat.e("Shared OS early cancellation receipt", it) }
                }
            }
            job.start()
        } else job.cancel()
        return JsonPrimitive(accepted)
    }

    suspend fun disconnect() {
        val current = lock.withLock { jobs.values.map { it.second } }
        current.forEach { it.cancelAndJoin() }
        val generations = lock.withLock { temporary.keys.toList() }
        generations.forEach { generation ->
            val handles = lock.withLock { temporary[generation]?.toList().orEmpty() }
            DownloadIo.run { handles.forEach { it.delete() } }
            lock.withLock { temporary.remove(generation) }
        }
    }

    private suspend fun transfer(params: JsonObject) {
        val operation = params.getValue("operation").jsonObject
        val generation = params.getValue("generation").jsonPrimitive.content
        var handle: DownloadTempFileHandle? = null
        var closed = false
        var bytes = 0L
        var path = ""
        var failure: String? = null
        var retained = false
        var savedPublic = false
        try {
            DownloadIo.run {
                val isZip = operation.getValue("kind").jsonPrimitive.content == "zip"
                val target = operation["target"]?.jsonObject
                val isTemporary = operation["temporary"]?.jsonPrimitive?.boolean == true
                val isPublic = target?.getValue("storeToDownloads")?.jsonPrimitive?.boolean == true
                val name = target?.getValue("entry")?.jsonObject?.getValue("name")?.jsonPrimitive?.content.orEmpty()
                val destination = when {
                    isZip -> operation.getValue("path").jsonPrimitive.content
                    isTemporary || isPublic -> null
                    else -> "${target!!.getValue("writeDir").jsonPrimitive.content.trimEnd('/')}/$name"
                }
                val output = if (destination == null) createDownloadTempFile("shared_${params.getValue("ticket").jsonPrimitive.content}") else createFileWriteHandle(destination)
                handle = output
                if (isZip) {
                    val entries = operation.getValue("items").jsonArray.map { item ->
                        ZipStreamEntry(item.jsonObject.getValue("sourcePath").jsonPrimitive.content, item.jsonObject.getValue("entryName").jsonPrimitive.content)
                    }
                    SharedLinkClient.packZip(entries) { buffer, length -> output.write(buffer, 0, length); bytes += length }
                } else {
                    SharedLinkClient.downloadTo(operation.getValue("url").jsonPrimitive.content, { buffer, length -> output.write(buffer, 0, length); bytes += length }) { done, _ ->
                        val active = call(params, "progress") { put("bytes", done) }.jsonPrimitive.boolean
                        check(active) { "Shared OS operation expired" }
                    }
                }
                output.close(); closed = true
                currentCoroutineContext().ensureActive()
                path = if (isPublic) saveTempFileToDownloads(output, name).also { check(it.isNotEmpty()) { "OS declined shared file save" }; savedPublic = true } else output.filePath
                if (isTemporary) {
                    lock.withLock { temporary.getOrPut(generation) { mutableListOf() }.add(output) }
                    retained = true
                }
            }
        } catch (cancelled: CancellationException) {
            failure = "Shared OS operation canceled or disconnected"
            throw cancelled
        } catch (error: Exception) { failure = error.message ?: "Shared OS save failed" }
        finally {
            withContext(NonCancellable) {
                if (!closed) runCatching { handle?.close() }.onFailure { if (failure == null) failure = it.message ?: "OS close failed" }
                var accepted = false
                try {
                    accepted = call(params, "receipt") {
                        put("receipt", buildJsonObject { put("path", path); put("bytes", bytes); put("error", failure?.let(::JsonPrimitive) ?: JsonNull) })
                    }.jsonPrimitive.boolean
                } catch (error: Exception) { LogCat.e("Shared OS save receipt", error) }
                if (!retained && (failure != null || !accepted || savedPublic)) runCatching { handle?.delete() }.onFailure { error ->
                    handle?.let { output -> lock.withLock { temporary.getOrPut(generation) { mutableListOf() }.add(output) } }
                    LogCat.e("Shared OS target cleanup", error)
                }
            }
        }
    }
    private suspend fun call(params: JsonObject, action: String, body: JsonObjectBuilder.() -> Unit): JsonElement =
        RustContentApi.postJsonOrThrow("shares/batch", buildJsonObject {
            put("action", action); put("id", params.getValue("id")); put("generation", params.getValue("generation")); put("ticket", params.getValue("ticket")); body()
        }, longRunning = true).getValue("result")
}
