package com.ismartcoding.plain.api

import com.ismartcoding.plain.features.imageindex.ImageIndexHost
import com.ismartcoding.plain.features.audio.AudioLibraryHost
import com.ismartcoding.plain.features.audio.AudioEngineHost
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.createPeerStatusHttpClient
import com.ismartcoding.plain.platform.handleMediaActionHost
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.serialization.json.*

object RustHostApi {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val client by lazy { createPeerStatusHttpClient() }
    private var collector: Job? = null

    fun start(session: ContentApiSession) {
        check(collector == null) { "Rust host already started" }
        collector = scope.launch {
            var retryMs = 500L
            while (currentCoroutineContext().isActive) {
                try {
                    client.webSocket(session.baseUrl.replace("http://", "ws://") + "/host", session.headers()) { socket ->
                        retryMs = 500L
                        coroutineScope {
                            val capacity = Semaphore(4)
                            val sending = kotlinx.coroutines.sync.Mutex()
                            try {
                                for (frame in socket.incoming) {
                                    val text = frame.text ?: continue
                                    val request = Json.parseToJsonElement(text).jsonObject
                                    val id = request.getValue("id").jsonPrimitive.long
                                    val method = request.string("method")
                                    val params = request.getValue("params").jsonObject
                                    capacity.acquire()
                                    launch {
                                        try {
                                            val reply = try {
                                                val result = when {
                                                    method == "audioEngineCommand" -> AudioEngineHost.handle(method, params)
                                                    method.startsWith("fileTask") -> com.ismartcoding.plain.features.file.FileTaskHost.handle(method,params)
                                                    method == "mediaAction" -> handleMediaActionHost(params)
                                                    method.startsWith("imageIndex") -> ImageIndexHost.handle(method, params)
                                                    else -> AudioLibraryHost.handle(method, params)
                                                }
                                                buildJsonObject { put("id", id); put("result", result) }
                                            } catch (cancelled: CancellationException) { throw cancelled }
                                            catch (e: Exception) { buildJsonObject { put("id", id); put("error", e.message ?: "Host operation failed") } }
                                            sending.lock()
                                            try { socket.sendText(reply.toString()) } finally { sending.unlock() }
                                        } finally { capacity.release() }
                                    }
                                }
                            } finally { coroutineContext.cancelChildren() }
                        }
                    }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (e: Exception) { LogCat.e("Rust host: ${e.message}") }
                finally { withContext(NonCancellable) { try { ImageIndexHost.disconnect() } catch (error: Exception) { LogCat.e("Image index cleanup",error) } } }
                delay(retryMs)
                retryMs = (retryMs * 2).coerceAtMost(5_000)
            }
        }
    }
}
