package com.ismartcoding.plain.platform.network.aware

import android.Manifest
import android.net.ConnectivityManager
import android.net.wifi.aware.PeerHandle
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.ismartcoding.plain.platform.network.AwareConfig
import com.ismartcoding.plain.platform.network.AwarePeer
import com.ismartcoding.plain.lib.logcat.LogCat
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.S)
internal class AwareLinkPool(
    private val session: AwareSession,
    private val connectivityManager: ConnectivityManager,
) {
    private val links = ConcurrentHashMap<String, AwarePeerLink>()
    private data class Subscription(val peer: AwarePeer, val config: AwareConfig)
    private val configurationLock = Any()
    private val subscribedPeers = ConcurrentHashMap<String, Subscription>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var sweepJob: Job? = null

    private val onPeerDiscovered: (String, PeerHandle) -> Unit = { peerId, peerHandle ->
        LogCat.d("[AWARE] onPeerDiscovered peer=$peerId subscribed=${subscribedPeers.containsKey(peerId)}")
        scope.launch {
            val (_, link) = linkFor(peerId) ?: return@launch
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
            val (_, link) = linkFor(peerId) ?: return@launch
            link.updatePeerHandle(peerHandle)
            if (link.isConnected() || link.isBuilding()) {
                LogCat.d("[AWARE] onPublishHelloReceived skip peer=$peerId state=${link.stateName()}")
                return@launch
            }
            try {
                buildLink(peerId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogCat.w("[AWARE] onPublishHelloReceived build fail peer=$peerId type=${error::class.simpleName} msg=${error.message}")
            }
        }
    }

    // subscriber 收到 publisher 的 ready：publisher 已 requestNetwork，本端（subscriber）作为接收方自动建链。
    // 仅在未连接且未 building 时触发，避免和发送方的 buildLink 重复。
    private val onSubscribeReadyReceived: (String) -> Unit = { peerId ->
        LogCat.d("[AWARE] onSubscribeReadyReceived peer=$peerId")
        scope.launch {
            val (_, link) = linkFor(peerId) ?: return@launch
            if (link.isConnected() || link.isBuilding()) {
                LogCat.d("[AWARE] onSubscribeReadyReceived skip peer=$peerId state=${link.stateName()}")
                return@launch
            }
            try {
                buildLink(peerId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogCat.w("[AWARE] onSubscribeReadyReceived build fail peer=$peerId type=${error::class.simpleName} msg=${error.message}")
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.CHANGE_NETWORK_STATE])
    suspend fun buildLink(peerId: String): PeerConnection {
        val (peer, link) = checkNotNull(linkFor(peerId)) { "Aware subscription unavailable" }
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
        val previous = synchronized(configurationLock) {
            subscribedPeers.clear()
            links.values.toList().also { links.clear() }
        }
        previous.forEach { it.close(reason = "stop") }
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }

    fun subscribe(peer: AwarePeer, config: AwareConfig) {
        val previous = synchronized(configurationLock) {
            val old = subscribedPeers.put(peer.id, Subscription(peer, config))
            if (old != Subscription(peer, config)) links.remove(peer.id) else null
        }
        previous?.close(reason = "configuration changed")
        session.start()
    }

    fun unsubscribe(peerId: String) {
        val previous = synchronized(configurationLock) {
            subscribedPeers.remove(peerId)
            links.remove(peerId)
        }
        previous?.close(reason = "unsubscribe")
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.CHANGE_NETWORK_STATE])
    private fun linkFor(peerId: String): Pair<AwarePeer, AwarePeerLink>? {
        session.start()
        return synchronized(configurationLock) {
            val subscription = subscribedPeers[peerId] ?: return@synchronized null
            val link = links.computeIfAbsent(peerId) {
                AwarePeerLink.create(
                    peer = subscription.peer,
                    config = subscription.config,
                    session = session,
                    connectivityManager = connectivityManager,
                    onClose = { closed: AwarePeerLink, _: String ->
                        links.remove(closed.peerId, closed)
                    },
                )
            }
            subscription.peer to link
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
