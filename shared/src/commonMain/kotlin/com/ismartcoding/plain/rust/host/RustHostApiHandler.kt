package com.ismartcoding.plain.rust.host

import com.ismartcoding.plain.features.chat.peer.transport.PeerTransportType
import com.ismartcoding.plain.features.chat.peer.PeerTransportProjection
import com.ismartcoding.plain.features.chat.peer.PeerTransportState
import com.ismartcoding.plain.rust.host.audio.AudioPlaybackState
import com.ismartcoding.plain.features.audio.AudioQueueClient
import com.ismartcoding.plain.features.nearby.BleScanDriver
import com.ismartcoding.plain.features.nearby.PairingProjection
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.platform.files.FileResourceHost
import com.ismartcoding.plain.platform.files.FileResourceStreamHost
import com.ismartcoding.plain.platform.files.storageVolumeFacts
import com.ismartcoding.plain.platform.media.readImageBounds
import com.ismartcoding.plain.platform.telephony.phoneLocaleFacts
import com.ismartcoding.plain.platform.media.handleImageCatalogHost
import com.ismartcoding.plain.platform.media.observeImageCatalog
import com.ismartcoding.plain.platform.network.ble.PairingTransport
import com.ismartcoding.plain.platform.network.ble.PeerBleExchange
import com.ismartcoding.plain.platform.network.isBleReady
import com.ismartcoding.plain.platform.network.isWifiAwareSupported
import kotlinx.serialization.json.jsonObject
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.HttpServerManager
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.features.dlna.sender.RustDlnaSender
import com.ismartcoding.plain.features.shares.SharedFolderDownloadEngine
import com.ismartcoding.plain.features.settings.AppUpdateDownload
import com.ismartcoding.plain.features.settings.AppUpdateDownloads
import com.ismartcoding.plain.ui.clipboard.ClipboardHistoryViewModel
import com.ismartcoding.plain.ui.audio.AudioHomeViewModel
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.feeds.FeedWorkerState
import com.ismartcoding.plain.ui.feeds.FeedSyncUpdate
import com.ismartcoding.plain.features.sessions.setOnlineClientIds
import com.ismartcoding.plain.rust.RustHttpStatus
import com.ismartcoding.plain.rust.host.audio.AudioEngineHost
import com.ismartcoding.plain.rust.host.files.ThumbnailHost
import com.ismartcoding.plain.rust.host.network.BlePairingHost
import com.ismartcoding.plain.rust.host.network.MdnsMulticastHost
import com.ismartcoding.plain.rust.host.network.NearbyScanHost
import com.ismartcoding.plain.rust.host.network.PairingNotificationHost
import com.ismartcoding.plain.rust.host.network.PeerTransportPrewarmHost
import com.ismartcoding.plain.rust.host.provider.SystemAppHost
import com.ismartcoding.plain.rust.native.RustNativeBindings
import com.ismartcoding.plain.rust.native.readRustStream
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

private val orderedHostCallbacks = setOf(
    "httpServerStatusChanged", "onlineClientsChanged", "peerTransportsChanged",
    "dlnaRendererChanged", "dlnaSenderChanged", "appUpdateDownloadChanged", "feedSyncChanged",
)

internal fun CoroutineScope.launchHostRequest(method: String, block: suspend CoroutineScope.() -> Unit): Job =
    launch(
        start = if (SessionHost.handles(method) || method in orderedHostCallbacks) CoroutineStart.UNDISPATCHED else CoroutineStart.DEFAULT,
        block = block,
    )

internal object RustHostApiHandler {
    private val requests = Channel<String>(32)

    fun start(scope: CoroutineScope) {
        scope.launch {
            val bleAdvertising = PairingTransport.connect()
            val advertisingObservation = launch {
                try { bleAdvertising.observeFacts() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { LogCat.e("BLE advertising observation", error) }
            }
            val clipboardObservation = launch {
                try { observeClipboardReports() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { LogCat.e("Clipboard observation", error) }
            }
            try {
                coroutineScope {
                    val exchangeScope = this
                    val capacity = Semaphore(4)
                    for (text in requests) {
                        val request = JsonHelper.jsonDecode<RustHostRequest>(text)
                        capacity.acquire()
                        launchHostRequest(request.method) {
                            try {
                                val reply = try {
                                    executeHostRequest(request, exchangeScope, bleAdvertising).encodeReply(request.id)
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (error: Exception) { encodeHostError(request.id, error) }
                                val error = RustNativeBindings.hostReply(reply)
                                check(error.isEmpty()) { error }
                            } finally { capacity.release() }
                        }
                    }
                }
            } finally {
                withContext(NonCancellable) {
                    clipboardObservation.cancelAndJoin()
                    advertisingObservation.cancelAndJoin()
                    cleanup("BLE advertising") { bleAdvertising.close() }
                    disconnect()
                }
            }
        }
        scope.launch {
            readRustStream(
                next = { RustNativeBindings.hostNext() },
                consume = { requests.send(it) },
                closed = { requests.cancel() },
            )
        }
    }

    private suspend fun disconnect() {
        cleanup("Multicast permission") { MdnsMulticastHost.disconnect() }
        cleanup("BLE handle") { BlePairingHost.disconnect() }
        cleanup("Image index") { observeImageCatalog(false) }
    }

    private suspend fun cleanup(name: String, block: suspend () -> Unit) {
        try { block() } catch (error: Exception) { LogCat.e("$name cleanup", error) }
    }
}

private val httpStatusMutex = Mutex()

internal suspend fun executeHostRequest(
    request: RustHostRequest,
    exchangeScope: CoroutineScope,
    bleAdvertising: PairingTransport.Connection,
): HostResult {
    val method = request.method
    val params = request.params
    return when (method) {
        "systemWebActivity", "systemWebLoginRequested", "systemWebLoginClosed",
        "systemWebLoginCompleted", "systemWebLoginSnapshot" -> SessionHost.handle(method, params)
        "httpServerStatusChanged" -> {
            httpStatusMutex.withLock {
                HttpServerManager.applyHttpServerStatus(JsonHelper.jsonDecodeFromElement<RustHttpStatus>(params))
            }
            hostResult()
        }
        "onlineClientsChanged" -> {
            setOnlineClientIds(params.getValue("clientIds").jsonArray.map { it.jsonPrimitive.content }.toSet())
            hostResult()
        }
        "pairingSucceeded" -> {
            PairingProjection.success(params.getValue("deviceId").jsonPrimitive.content, params.getValue("key").jsonPrimitive.content)
            hostResult()
        }
        "audioLibraryRefresh" -> {
            exchangeScope.launch(Dispatchers.Main.immediate) {
                try { AudioHomeViewModel.refreshActive() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { LogCat.e("Refresh audio overview", error) }
                try { AudioQueueViewModel.refreshActive() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { LogCat.e("Refresh audio queue", error) }
            }
            hostResult()
        }
        "audioPlaybackRefresh" -> {
            exchangeScope.launch {
                try {
                    val playback = AudioQueueClient.playback()
                    AudioEngineHost.restore(AudioPlaybackState(playback.revision, playback.positionMs))
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { LogCat.e("Restore audio playback", error) }
            }
            hostResult()
        }
        "feedSyncChanged" -> {
            FeedWorkerState.apply(JsonHelper.jsonDecodeFromElement<FeedSyncUpdate>(params))
            hostResult()
        }
        "dlnaSenderChanged" -> {
            RustDlnaSender.apply(params)
            hostResult()
        }
        "appUpdateDownloadChanged" -> {
            AppUpdateDownloads.apply(JsonHelper.jsonDecodeFromElement<AppUpdateDownload>(params))
            hostResult()
        }
        "peerTransportsChanged" -> {
            PeerTransportProjection.apply(JsonHelper.jsonDecodeFromElement<List<PeerTransportState>>(params.getValue("active")))
            hostResult()
        }
        "sharedFolderDownloadsRefresh" -> {
            SharedFolderDownloadEngine.refresh()
            hostResult()
        }
        "dlnaRendererChanged" -> {
            DlnaRendererState.apply(params)
            hostResult()
        }
        "dlnaRendererRefresh" -> {
            DlnaRendererState.refresh()
            hostResult()
        }
        "clipboardHistoryRefresh" -> {
            withContext(Dispatchers.Main.immediate) { ClipboardHistoryViewModel.refreshActive() }
            hostResult()
        }
        "peerNotificationsChanged", "peerClipboardChanged", "networkInterfacesChanged" -> hostResult()
        "prefsChanged" -> {
            for ((key, value) in params.getValue("values").jsonObject) {
                Prefs.update(key, value)
            }
            hostResult()
        }
        "fileResourceStream" -> { FileResourceStreamHost.start(exchangeScope, params); hostResult() }
        "thumbnailDecode" -> ThumbnailHost.decode(params)
        "blePairConnect", "blePairSend", "blePairWait",
        "blePairClose" -> BlePairingHost.handle(method, params)
        "bleAdvertising" -> hostResult(bleAdvertising.handle(params))
        "bleScanRead" -> hostResult(BleScanDriver.read(params))
        "mdnsMulticast" -> MdnsMulticastHost.handle(params)
        "nearbyScanFacts" -> NearbyScanHost.facts()
        "pairingNotification" -> PairingNotificationHost.handle(params)
        "peerTransportPrewarmScan" -> PeerTransportPrewarmHost.scan(params)
        "peerTransportCapabilities" -> hostResult(buildList {
            if (isWifiAwareSupported) add(PeerTransportType.AWARE)
            if (isBleReady()) add(PeerTransportType.BLE)
        })
        "peerTransportBleExchange" -> hostResult(PeerBleExchange.exchange(params))
        "systemPhoneLocaleFacts" -> hostResult(phoneLocaleFacts())
        "systemImageModelsObserve" -> {
            observeImageCatalog(JsonHelper.jsonDecodeFromElement<ObserveImageCatalogRequest>(params).enabled)
            hostResult(true)
        }
        "systemFileResource" -> hostResult(FileResourceHost.handle(params))
        "systemMountFacts" -> hostResult(storageVolumeFacts())
        "imageDecodeBounds" -> hostResult(readImageBounds(JsonHelper.jsonDecodeFromElement<PathRequest>(params).path))
        "systemAudioPlaybackState" -> hostResult(AudioEngineHost.localPlaybackFacts())
        "systemSetClipboard", "systemPermissionFacts", "systemOpenWebSettings",
        "systemNetworkType", "systemScheduleBackgroundWake", "systemCompleteBackgroundExecution",
        "systemProperties", "systemBatterySample",
        "systemCapabilities", "systemClearMediaCache",
        "systemRelaunchApp" -> SystemAppHost.handle(method, params)
        "audioEngineCommand" -> AudioEngineHost.command(params)
        "imageCatalogSnapshot", "imageCatalogPage", "imageCatalogResolve",
        "imageCatalogVerify" -> hostResult(handleImageCatalogHost(method, params))
        else -> executePlatformHostRequest(method, params, exchangeScope)

    }
}

internal expect suspend fun executePlatformHostRequest(method: String, params: JsonObject, scope: CoroutineScope): HostResult

@Serializable
private data class ObserveImageCatalogRequest(val enabled: Boolean)
