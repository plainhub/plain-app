package com.ismartcoding.plain.api

import com.ismartcoding.plain.features.imageindex.ImageIndexHost
import com.ismartcoding.plain.features.audio.AudioLibraryHost
import com.ismartcoding.plain.features.audio.AudioEngineHost
import com.ismartcoding.plain.lib.JsonHelper
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

    fun start(sessionProvider: () -> ContentApiSession) {
        check(collector == null) { "Rust host already started" }
        collector = scope.launch {
            var retryMs = 500L
            while (currentCoroutineContext().isActive) {
                try {
                    val session = sessionProvider()
                    client.webSocket(session.baseUrl.replace("http://", "ws://") + "/host", session.headers()) { socket ->
                        retryMs = 500L
                        coroutineScope {
                            val exchangeScope = this
                            val capacity = Semaphore(4)
                            val sending = kotlinx.coroutines.sync.Mutex()
                            try {
                                for (frame in socket.incoming) {
                                    val text = frame.text ?: continue
                                    val session = sessionProvider()
                                    val request = JsonHelper.jsonDecode<RustHostRequest>(text)
                                    val id = request.id
                                    val method = request.method
                                    val params = request.params
                                    capacity.acquire()
                                    launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                                        try {
                                            val reply = try {
                                                val result = when (routeForHostMethod(method)) {
                                                    HostRoute.FileResourceStream -> { com.ismartcoding.plain.platform.FileResourceStreamHost.start(exchangeScope, session, params); JsonNull }
                                                    HostRoute.Thumbnail -> com.ismartcoding.plain.thumbnail.ThumbnailHost.handle(session, method, params)
                                                    HostRoute.ChatPicked -> com.ismartcoding.plain.chat.ChatPickedHost.handle(method, params)
                                                    HostRoute.SharedTransfer -> com.ismartcoding.plain.features.share.SharedTransferHost.handle(exchangeScope, method, params)
                                                    HostRoute.BlePairing -> com.ismartcoding.plain.discover.BlePairingHost.handle(method, params)
                                                    HostRoute.MainGraphql -> com.ismartcoding.plain.platform.HttpServerHost.handle(method, params)
                                                    HostRoute.MdnsMulticast -> com.ismartcoding.plain.discover.MdnsMulticastHost.handle(params)
                                                    HostRoute.DiscoveryAdvertisement -> com.ismartcoding.plain.discover.DiscoveryAdvertisementHost.facts()
                                                    HostRoute.NearbyScan -> com.ismartcoding.plain.discover.NearbyScanHost.facts()
                                                    HostRoute.PairingNotification -> com.ismartcoding.plain.discover.PairingNotificationHost.handle(params)
                                                    HostRoute.PeerGraphql -> com.ismartcoding.plain.chat.peer.PeerGraphQLHost.handle(method, params)
                                                    HostRoute.PeerTransport -> com.ismartcoding.plain.chat.peer.transport.PeerTransportHost.handle(method, params)
                                                    HostRoute.SystemProvider -> com.ismartcoding.plain.features.system.SystemProviderHost.handle(method, params)
                                                    HostRoute.AudioEngine -> AudioEngineHost.handle(method, params)
                                                    HostRoute.FileTask -> com.ismartcoding.plain.features.file.FileTaskHost.handle(method,params)
                                                    HostRoute.MediaAction -> handleMediaActionHost(params)
                                                    HostRoute.ImageIndex -> ImageIndexHost.handle(method, params)
                                                    HostRoute.AudioLibrary -> AudioLibraryHost.handle(method, params)
                                                }
                                                RustHostReply(id, result = result)
                                            } catch (cancelled: CancellationException) { throw cancelled }
                                            catch (e: Exception) { RustHostReply(id, error = e.message ?: "Host operation failed") }
                                            sending.lock()
                                            try { socket.sendText(JsonHelper.jsonEncode(reply)) } finally { sending.unlock() }
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
