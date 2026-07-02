package com.ismartcoding.plain.chat.peer.transport.aware

import android.Manifest
import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.WifiAwareNetworkInfo
import android.net.wifi.aware.WifiAwareNetworkSpecifier
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.chat.peer.PeerCacher
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.lib.logcat.LogCat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.net.Inet6Address
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("MissingPermission")
class AwarePeerLink(
    val peerId: String,
    private val session: AwareSession,
    private val connectivityManager: ConnectivityManager,
    private val httpFactory: AwareHttpClientFactory,
    private val isClient: Boolean,
    private val onClose: (peerId: String, reason: String) -> Unit,
) {
    enum class LinkState { IDLE, CONNECTING, CONNECTED, CLOSED }

    private val connection = AtomicReference<PeerConnection?>(null)
    private val closed = AtomicBoolean(false)
    private val buildMutex = Mutex()
    @Volatile private var state: LinkState = LinkState.IDLE
    @Volatile private var peerHandle: PeerHandle? = null

    val lastActiveAt = AtomicLong(System.currentTimeMillis())

    fun updatePeerHandle(handle: PeerHandle) {
        peerHandle = handle
    }

    fun touch() {
        lastActiveAt.set(System.currentTimeMillis())
    }

    fun isConnected(): Boolean {
        if (state != LinkState.CONNECTED) return false
        val conn = connection.get() ?: return false
        return connectivityManager.getNetworkCapabilities(conn.network) != null
    }

    fun isBuilding(): Boolean = state == LinkState.CONNECTING

    fun stateName(): String = state.name

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.CHANGE_NETWORK_STATE])
    suspend fun build(d: DPeer): PeerConnection = buildMutex.withLock {
        LogCat.d("[AWARE] build start peer=${d.id} state=$state isClient=$isClient")
        when (state) {
            LinkState.CONNECTED -> {
                val conn = connection.get()
                if (conn != null && connectivityManager.getNetworkCapabilities(conn.network) != null) {
                    LogCat.d("[AWARE] build reuse peer=${d.id}")
                    return@withLock conn
                }
                LogCat.d("[AWARE] build connected-but-lost peer=${d.id}")
                connection.set(null)
                state = LinkState.IDLE
            }
            LinkState.CLOSED -> error("Aware build link closed peer=${d.id}")
            LinkState.CONNECTING, LinkState.IDLE -> Unit
        }

        // Wi-Fi Aware data path requires BOTH sides to call requestNetwork at roughly the same
        // time. The framework rejects a request within ~500ms (releaseRequestAsUnfulfillableByAnyFactory)
        // if no matching request exists on the other side. To synchronize:
        //
        // - subscriber (client): owns the retry loop. Each retry sends hello + requestNetwork.
        // - publisher (server): NO retry loop — only attempts once per hello. When the subscriber
        //   retries, it sends a fresh hello which triggers the publisher's buildLink via
        //   onPublishHelloReceived. This way the publisher's requestNetwork always follows the
        //   hello arrival, staying within ~50ms of the subscriber's requestNetwork.
        //
        // If the publisher also retried independently, its retry cycle would drift from the
        // subscriber's, and the two would never overlap inside the 500ms window.
        val maxAttempts = if (isClient) MAX_BUILD_ATTEMPTS else 1
        var lastError: Throwable? = null
        repeat(maxAttempts) { attempt ->
            state = LinkState.CONNECTING
            try {
                return@withLock withTimeout(ATTEMPT_TIMEOUT_MS.milliseconds) {
                    // 角色分流（对齐官方 "Create a connection" 流程）：
                    // - publisher (server): 等 subscriber 的 hello 拿到 PeerHandle，
                    //   requestNetwork 后发 ready 回执给 subscriber（非阻塞信号）。
                    // - subscriber (client): 拿 PeerHandle → 显式发 hello → 立即 requestNetwork（不等 ready）。
                    val handle = if (!isClient) {
                        session.awaitPublishPeerHandle(d.id)
                    } else {
                        val h = peerHandle ?: session.awaitPeerHandle(d.id)
                        session.sendHelloMessage(h)
                        h
                    }
                    LogCat.d("[AWARE] build handle ok peer=${d.id} attempt=${attempt + 1}/$maxAttempts hasPeerHandle=${peerHandle != null}")
                    val conn = openNetwork(handle, d.port)
                    state = LinkState.CONNECTED
                    connection.set(conn)
                    LogCat.d("[AWARE] build ok peer=${d.id} ip=${conn.peerIpv6} port=${conn.peerPort}")
                    conn
                }
            } catch (e: Throwable) {
                lastError = e
                state = LinkState.IDLE
                session.clearPeerReady(d.id)
                LogCat.w("[AWARE] build attempt ${attempt + 1}/$maxAttempts fail peer=${d.id} type=${e::class.simpleName} msg=${e.message}")
                if (attempt < maxAttempts - 1) {
                    LogCat.d("[AWARE] build retry in ${RETRY_DELAY_MS}ms peer=${d.id}")
                    delay(RETRY_DELAY_MS)
                }
            }
        }
        throw lastError ?: IllegalStateException("build failed")
    }

    private suspend fun openNetwork(handle: PeerHandle, port: Int): PeerConnection {
        val specifier = buildSpecifier(handle)
        LogCat.d("[AWARE] openNetwork start peer=$peerId specifier=$specifier pub=${session.publish != null} sub=${session.subscribe != null} awareAvail=${session.isAvailable()}")
        val ready = CompletableDeferred<PeerConnection>()
        var pendingNetwork: Network? = null
        var pendingIpv6: Inet6Address? = null
        var registered = false

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                LogCat.d("[AWARE] onAvailable peer=$peerId network=$network")
                pendingNetwork = network
                tryComplete()
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val info = caps.transportInfo as? WifiAwareNetworkInfo
                val ip = info?.peerIpv6Addr
                LogCat.d("[AWARE] onCapabilitiesChanged peer=$peerId hasInfo=${info != null} ip=${ip != null}")
                if (ip == null) return
                pendingIpv6 = ip
                tryComplete()
            }

            override fun onUnavailable() {
                LogCat.w("[AWARE] onUnavailable peer=$peerId")
                runCatching { connectivityManager.unregisterNetworkCallback(this) }
                // 不调 session.invalidatePeerHandle —— PeerHandle 本身没失效，只是 NDP 配对没成功
                // （框架在另一端 requestNetwork 到达前就拒绝了）。清掉会让重试等不到新的
                // onServiceDiscovered（它只触发一次），导致死循环。只清本地引用让重试从 session 缓存取。
                peerHandle = null
                if (!ready.isCompleted) {
                    ready.completeExceptionally(IllegalStateException("network unavailable"))
                }
            }

            override fun onLost(network: Network) {
                LogCat.w("[AWARE] onLost peer=$peerId network=$network")
                runCatching { connectivityManager.unregisterNetworkCallback(this) }
                onNetworkLost()
            }

            fun tryComplete() {
                if (ready.isCompleted) return
                val n = pendingNetwork ?: return
                val ip = pendingIpv6 ?: return
                LogCat.d("[AWARE] ready complete peer=$peerId ip=$ip port=$port")
                ready.complete(
                    PeerConnection(
                        network = n,
                        peerIpv6 = ip,
                        peerPort = port,
                        httpClient = httpFactory.build(peerId, n, ip, port),
                    ),
                )
            }
        }

        try {
            LogCat.d("[AWARE] requestNetwork peer=$peerId port=$port timeout=${REQUEST_TIMEOUT_MS}ms")
            connectivityManager.requestNetwork(
                NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE)
                    .setNetworkSpecifier(specifier)
                    .build(),
                callback,
                REQUEST_TIMEOUT_MS,
            )
            registered = true
            LogCat.d("[AWARE] requestNetwork registered peer=$peerId")
            // publisher requestNetwork 注册后发 ready 回执给 subscriber。
            // 这是非阻塞信号 —— subscriber 不等 ready 就已 requestNetwork，但收到 ready 时
            // 如果处于 IDLE（上一次 build 失败在 retry delay 中），可以立即重新 buildLink，
            // 不用等 retry delay，提高双方时序对齐的概率。
            if (!isClient) {
                session.sendReadyMessage(handle)
            }
        } catch (e: Exception) {
            LogCat.e("[AWARE] requestNetwork error peer=$peerId msg=${e.message}")
            if (!ready.isCompleted) ready.completeExceptionally(e)
        }

        return try {
            ready.await()
        } catch (e: Throwable) {
            LogCat.w("[AWARE] openNetwork await fail peer=$peerId type=${e::class.simpleName} msg=${e.message}")
            if (registered) {
                runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            }
            throw e
        }
    }

    private fun buildSpecifier(handle: PeerHandle): WifiAwareNetworkSpecifier {
        val pmk = derivePmk()
        val builder = if (isClient) {
            val sub = session.subscribe ?: error("subscribe session not ready")
            WifiAwareNetworkSpecifier.Builder(sub, handle)
        } else {
            val pub = session.publish ?: error("publish session not ready")
            WifiAwareNetworkSpecifier.Builder(pub)
        }
        pmk?.let { builder.setPmk(it) }
        if (!isClient) {
            builder.setPort(TempData.httpsPort.value)
        }
        LogCat.d("[AWARE] buildSpecifier peer=$peerId role=${if (isClient) "client" else "server"} pmk=${pmk != null}")
        return builder.build()
    }

    private fun derivePmk(): ByteArray? = try {
        val raw = PeerCacher.getKeyBytes(peerId)
        when {
            raw == null || raw.isEmpty() -> null
            raw.size == 32 -> raw
            else -> ByteArray(32).also { System.arraycopy(raw, 0, it, 0, min(raw.size, 32)) }
        }
    } catch (e: Exception) {
        LogCat.e("Wi-Fi Aware derive PMK failed: ${e.message}")
        null
    }

    private fun onNetworkLost() {
        if (!buildMutex.tryLock()) return
        try {
            if (state == LinkState.CLOSED) return
            state = LinkState.IDLE
            connection.getAndSet(null)?.httpClient?.let { runCatching { it.connectionPool.evictAll() } }
            LogCat.w("Aware onLost peer=$peerId state=IDLE")
        } finally {
            buildMutex.unlock()
        }
    }

    fun close(reason: String) {
        if (!closed.compareAndSet(false, true)) return
        state = LinkState.CLOSED
        connection.getAndSet(null)?.httpClient?.let { runCatching { it.connectionPool.evictAll() } }
        onClose(peerId, reason)
    }

    companion object {
        private const val MAX_BUILD_ATTEMPTS = 3
        private const val ATTEMPT_TIMEOUT_MS = 10_000L
        private const val RETRY_DELAY_MS = 2_000L
        private const val REQUEST_TIMEOUT_MS = 30_000

        @RequiresApi(Build.VERSION_CODES.S)
        @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.CHANGE_NETWORK_STATE])
        fun create(
            peer: DPeer,
            session: AwareSession,
            connectivityManager: ConnectivityManager,
            httpFactory: AwareHttpClientFactory,
            onClose: (peerId: String, reason: String) -> Unit,
        ): AwarePeerLink = AwarePeerLink(
            peerId = peer.id,
            session = session,
            connectivityManager = connectivityManager,
            httpFactory = httpFactory,
            isClient = TempData.clientId < peer.id,
            onClose = onClose,
        )
    }
}
