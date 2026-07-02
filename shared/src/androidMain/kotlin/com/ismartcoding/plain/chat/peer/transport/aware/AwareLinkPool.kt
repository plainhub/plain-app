package com.ismartcoding.plain.chat.peer.transport.aware

import android.Manifest
import android.net.ConnectivityManager
import android.net.wifi.aware.PeerHandle
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

@RequiresApi(Build.VERSION_CODES.S)
internal class AwareLinkPool(
    private val session: AwareSession,
    private val connectivityManager: ConnectivityManager,
    private val httpFactory: AwareHttpClientFactory,
) {
    private val links = ConcurrentHashMap<String, AwarePeerLink>()
    private val subscribedPeers = ConcurrentHashMap<String, DPeer>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var sweepJob: Job? = null

    private val onPeerDiscovered: (String, PeerHandle) -> Unit = { peerId, peerHandle ->
        LogCat.d("[AWARE] onPeerDiscovered peer=$peerId subscribed=${subscribedPeers.containsKey(peerId)}")
        scope.launch {
            val peer = subscribedPeers[peerId] ?: return@launch
            val link = linkFor(peer)
            link.updatePeerHandle(peerHandle)
            // 只记录 PeerHandle，不自动 buildLink。
            // 扫到 100 台设备也不建链，只在真正 send/download 时才建 NDP。
            // 接收方的 buildLink 由 hello/ready 消息触发（见下方两个回调）。
        }
    }

    // publisher 收到 subscriber 的 hello：subscriber 要通信，本端（publisher）作为接收方自动建链。
    // 仅在未连接且未 building 时触发，避免和发送方的 buildLink 重复。
    private val onPublishHelloReceived: (String, PeerHandle) -> Unit = { peerId, peerHandle ->
        LogCat.d("[AWARE] onPublishHelloReceived peer=$peerId")
        scope.launch {
            val peer = subscribedPeers[peerId] ?: return@launch
            val link = linkFor(peer)
            link.updatePeerHandle(peerHandle)
            if (link.isConnected() || link.isBuilding()) {
                LogCat.d("[AWARE] onPublishHelloReceived skip peer=$peerId state=${link.stateName()}")
                return@launch
            }
            runCatching { buildLink(peer) }
                .onFailure { LogCat.w("[AWARE] onPublishHelloReceived build fail peer=$peerId msg=${it.message}") }
        }
    }

    // subscriber 收到 publisher 的 ready：publisher 已 requestNetwork，本端（subscriber）作为接收方自动建链。
    // 仅在未连接且未 building 时触发，避免和发送方的 buildLink 重复。
    private val onSubscribeReadyReceived: (String) -> Unit = { peerId ->
        LogCat.d("[AWARE] onSubscribeReadyReceived peer=$peerId")
        scope.launch {
            val peer = subscribedPeers[peerId] ?: return@launch
            val link = linkFor(peer)
            if (link.isConnected() || link.isBuilding()) {
                LogCat.d("[AWARE] onSubscribeReadyReceived skip peer=$peerId state=${link.stateName()}")
                return@launch
            }
            runCatching { buildLink(peer) }
                .onFailure { LogCat.w("[AWARE] onSubscribeReadyReceived build fail peer=$peerId msg=${it.message}") }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.CHANGE_NETWORK_STATE])
    suspend fun buildLink(peer: DPeer): PeerConnection {
        val link = linkFor(peer)
        val conn = link.build(peer)
        link.touch()
        return conn
    }

    fun start() {
        if (sweepJob?.isActive == true) return
        session.setOnServiceDiscovered(onPeerDiscovered)
        session.setOnPublishHelloReceived(onPublishHelloReceived)
        session.setOnSubscribeReadyReceived(onSubscribeReadyReceived)
        sweepJob = scope.launch {
            while (isActive) {
                delay(IDLE_SWEEP_INTERVAL_MS.milliseconds)
                sweepIdleLinks()
            }
        }
    }

    fun stop() {
        sweepJob?.cancel()
        sweepJob = null
        links.values.toList().forEach { it.close(reason = "stop") }
        links.clear()
        subscribedPeers.clear()
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }

    fun subscribe(peer: DPeer) {
        subscribedPeers[peer.id] = peer
        session.start()
    }

    fun unsubscribe(peerId: String) {
        subscribedPeers.remove(peerId)
        links.remove(peerId)?.close(reason = "unsubscribe")
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.CHANGE_NETWORK_STATE])
    suspend fun linkFor(peer: DPeer): AwarePeerLink {
        session.start()
        return links.computeIfAbsent(peer.id) {
            AwarePeerLink.create(
                peer = peer,
                session = session,
                connectivityManager = connectivityManager,
                httpFactory = httpFactory,
                onClose = { peerId: String, reason: String ->
                    links.remove(peerId)
                },
            )
        }
    }

    private fun sweepIdleLinks() {
        val now = System.currentTimeMillis()
        links.forEach { (peerId, link) ->
            if (now - link.lastActiveAt.get() > IDLE_TIMEOUT_MS) {
                link.close(reason = "idle")
            }
        }
    }

    companion object {
        private const val IDLE_TIMEOUT_MS = 60_000L
        private const val IDLE_SWEEP_INTERVAL_MS = 10_000L
    }
}