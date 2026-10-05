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
                            val exchangeScope = this
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
                                    launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                                        try {
                                            val reply = try {
                                                val result = when {
                                                    method == "httpExchange" -> { com.ismartcoding.plain.httpserver.RustHttpHost.start(exchangeScope, session, params.getValue("id").jsonPrimitive.content); JsonNull }
                                                    method.startsWith("thumbnail") -> com.ismartcoding.plain.thumbnail.ThumbnailHost.handle(session, method, params)
                                                    method.startsWith("chatPicked") -> com.ismartcoding.plain.chat.ChatPickedHost.handle(method, params)
                                                    method.startsWith("sharedTransfer") -> com.ismartcoding.plain.features.share.SharedTransferHost.handle(exchangeScope, method, params)
                                                    method.startsWith("blePair") -> com.ismartcoding.plain.discover.BlePairingHost.handle(method, params)
                                                    method.startsWith("mainGraphql") -> com.ismartcoding.plain.httpserver.MainGraphQLHost.handle(method, params)
                                                    method == "mdnsMulticast" -> com.ismartcoding.plain.discover.MdnsMulticastHost.handle(params)
                                                    method == "discoveryFacts" -> com.ismartcoding.plain.discover.DiscoveryAdvertisementHost.facts()
                                                    method == "nearbyScanFacts" -> com.ismartcoding.plain.discover.NearbyScanHost.facts()
                                                    method == "pairingNotification" -> com.ismartcoding.plain.discover.PairingNotificationHost.handle(params)
                                                    method == "peerStartAware" || method == "peerDeviceInfo" -> com.ismartcoding.plain.chat.peer.PeerGraphQLHost.handle(method, params)
                                                    method.startsWith("peerTransport") -> com.ismartcoding.plain.chat.peer.transport.PeerTransportHost.handle(method, params)
                                                    method.startsWith("system") || method == "fileMetadataFacts" -> com.ismartcoding.plain.features.system.SystemProviderHost.handle(method, params)
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
                finally { withContext(NonCancellable) { try { com.ismartcoding.plain.chat.peer.transport.PeerSocketHost.disconnect() } catch (error: Exception) { LogCat.e("Peer socket cleanup", error) }; try { com.ismartcoding.plain.chat.ChatPickedHost.disconnect() } catch (error: Exception) { LogCat.e("Selected file cleanup", error) }; try { com.ismartcoding.plain.features.share.SharedTransferHost.disconnect() } catch (error: Exception) { LogCat.e("Shared OS adapter cleanup", error) }; try { com.ismartcoding.plain.discover.MdnsMulticastHost.disconnect() } catch (error: Exception) { LogCat.e("Multicast permission cleanup", error) }; try { com.ismartcoding.plain.discover.BlePairingHost.disconnect() } catch (error: Exception) { LogCat.e("BLE handle cleanup", error) }; try { ImageIndexHost.disconnect() } catch (error: Exception) { LogCat.e("Image index cleanup",error) } } }
                delay(retryMs)
                retryMs = (retryMs * 2).coerceAtMost(5_000)
            }
        }
    }
}
