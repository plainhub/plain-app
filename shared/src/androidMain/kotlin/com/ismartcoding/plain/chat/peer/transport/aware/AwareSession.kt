package com.ismartcoding.plain.chat.peer.transport.aware

import android.Manifest
import android.net.wifi.aware.AttachCallback
import android.net.wifi.aware.DiscoverySessionCallback
import android.net.wifi.aware.PeerHandle
import android.net.wifi.aware.PublishConfig
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.SubscribeConfig
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareManager
import android.net.wifi.aware.WifiAwareSession
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.wifiAwareManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

@RequiresApi(Build.VERSION_CODES.S)
class AwareSession {

    @Volatile var session: WifiAwareSession? = null
        private set
    @Volatile var publish: PublishDiscoverySession? = null
        private set
    @Volatile var subscribe: SubscribeDiscoverySession? = null
        private set

    private var ready = CompletableDeferred<WifiAwareSession>()
    private val attaching = AtomicBoolean(false)

    private val discoveredPeers = ConcurrentHashMap<String, PeerHandle>()
    private val discoveredAt = ConcurrentHashMap<String, Long>()
    private val peerHandleWaiters = ConcurrentHashMap<String, CompletableDeferred<PeerHandle>>()
    private val serviceDiscoveredListeners = CopyOnWriteArrayList<(String, PeerHandle) -> Unit>()

    // Publish-session PeerHandles, obtained via onMessageReceived (hello from subscriber).
    // Used by publisher to send ready receipt back to subscriber.
    private val publishPeerHandles = ConcurrentHashMap<String, PeerHandle>()
    private val publishPeerHandleWaiters = ConcurrentHashMap<String, CompletableDeferred<PeerHandle>>()

    // Ready 回执（官方步骤 5）：publisher requestNetwork 后发 ready 给 subscriber，
    // subscriber 等 ready 后才发起 requestNetwork（步骤 6），确保双方时序对齐。
    // 没有这一步，subscriber 可能在 publisher 还没 requestNetwork 时就发起，导致 NDP 配对失败。
    private val readyPeers = ConcurrentHashMap<String, Boolean>()
    private val peerReadyWaiters = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    // Listener：publisher 收到 hello / subscriber 收到 ready 时通知 AwareLinkPool，
    // 让接收方（没主动 send 的一方）也触发 buildLink，保证双方都 requestNetwork。
    private val publishHelloListeners = CopyOnWriteArrayList<(String, PeerHandle) -> Unit>()
    private val subscribeReadyListeners = CopyOnWriteArrayList<(String) -> Unit>()

    fun setOnServiceDiscovered(listener: (String, PeerHandle) -> Unit) {
        serviceDiscoveredListeners.add(listener)
    }

    fun setOnPublishHelloReceived(listener: (String, PeerHandle) -> Unit) {
        publishHelloListeners.add(listener)
    }

    fun setOnSubscribeReadyReceived(listener: (String) -> Unit) {
        subscribeReadyListeners.add(listener)
    }

    fun invalidatePeerHandle(peerId: String) {
        discoveredPeers.remove(peerId)
        discoveredAt.remove(peerId)
        publishPeerHandles.remove(peerId)
        LogCat.d("[AWARE] invalidatePeerHandle peer=$peerId")
    }

    suspend fun awaitReady(): WifiAwareSession = ready.await()

    suspend fun awaitPeerHandle(peerId: String): PeerHandle {
        discoveredPeers[peerId]?.let { handle ->
            val age = System.currentTimeMillis() - (discoveredAt[peerId] ?: 0L)
            if (age < PEER_HANDLE_MAX_AGE_MS) {
                LogCat.d("[AWARE] awaitPeerHandle cached peer=$peerId ageMs=$age")
                return handle
            }
            // Stale PeerHandle — the peer's publish session may have been restarted
            // (e.g. MIUI killed it). An expired handle causes the framework to reject
            // requestNetwork immediately with onUnavailable. Discard and wait for
            // a fresh onServiceDiscovered callback.
            LogCat.d("[AWARE] awaitPeerHandle stale peer=$peerId ageMs=$age discarding")
            discoveredPeers.remove(peerId)
            discoveredAt.remove(peerId)
        }
        val waiter = peerHandleWaiters.computeIfAbsent(peerId) { CompletableDeferred() }
        discoveredPeers[peerId]?.let {
            if (!waiter.isCompleted) waiter.complete(it)
        }
        LogCat.d("[AWARE] awaitPeerHandle waiting peer=$peerId")
        return waiter.await()
    }

    suspend fun awaitPublishPeerHandle(peerId: String): PeerHandle {
        publishPeerHandles[peerId]?.let {
            LogCat.d("[AWARE] awaitPublishPeerHandle cached peer=$peerId")
            return it
        }
        val waiter = publishPeerHandleWaiters.computeIfAbsent(peerId) { CompletableDeferred() }
        publishPeerHandles[peerId]?.let {
            if (!waiter.isCompleted) waiter.complete(it)
        }
        LogCat.d("[AWARE] awaitPublishPeerHandle waiting peer=$peerId")
        return waiter.await()
    }

    // subscriber 侧调用：阻塞等待 publisher 的 ready 回执（官方步骤 5→6）
    suspend fun awaitPeerReady(peerId: String) {
        if (readyPeers[peerId] == true) {
            LogCat.d("[AWARE] awaitPeerReady already-ready peer=$peerId")
            return
        }
        val waiter = peerReadyWaiters.computeIfAbsent(peerId) { CompletableDeferred() }
        if (readyPeers[peerId] == true) {
            if (!waiter.isCompleted) waiter.complete(Unit)
            LogCat.d("[AWARE] awaitPeerReady already-ready(2) peer=$peerId")
            return
        }
        LogCat.d("[AWARE] awaitPeerReady waiting peer=$peerId")
        withTimeout(READY_TIMEOUT_MS) { waiter.await() }
    }

    // subscriber 侧收到 publisher 发来的 ready 回执时调用
    fun markPeerReady(peerId: String) {
        readyPeers[peerId] = true
        peerReadyWaiters.remove(peerId)?.complete(Unit)
        LogCat.d("[AWARE] markPeerReady peer=$peerId")
    }

    // build 失败后清掉 ready 标记，让下次重试重新等 publisher 发 ready。
    // 否则 awaitPeerReady 直接返回旧值，subscriber 在 publisher retry 之前就 requestNetwork。
    fun clearPeerReady(peerId: String) {
        readyPeers.remove(peerId)
        LogCat.d("[AWARE] clearPeerReady peer=$peerId")
    }

    // publisher 侧调用：requestNetwork 注册后立即发 ready 回执给 subscriber
    fun sendReadyMessage(peerHandle: PeerHandle) {
        runCatching {
            publish?.sendMessage(
                peerHandle,
                MSG_READY,
                TempData.clientId.toByteArray(Charsets.UTF_8),
            )
            LogCat.d("[AWARE] ready sent")
        }.onFailure { LogCat.w("[AWARE] sendReadyMessage fail: ${it.message}") }
    }

    // subscriber 侧调用：显式发 hello 给 publisher，触发 publisher 端 buildLink。
    // onServiceDiscovered 里也会发，但发现可能延迟或被跳过，所以 build() 里再发一次保证送达。
    fun sendHelloMessage(peerHandle: PeerHandle) {
        runCatching {
            subscribe?.sendMessage(
                peerHandle,
                MSG_HELLO,
                TempData.clientId.toByteArray(Charsets.UTF_8),
            )
            LogCat.d("[AWARE] hello sent (explicit)")
        }.onFailure { LogCat.w("[AWARE] sendHelloMessage fail: ${it.message}") }
    }

    @Synchronized
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE, "android.permission.NEARBY_WIFI_DEVICES"])
    fun start() {
        if (attaching.get() || session != null) return
        LogCat.d("[AWARE] session start isAvailable=${isAvailable()}")
        ready = newReady()
        attaching.set(true)
        try {
            wifiAwareManager.attach(attachCallback, null)
            LogCat.d("[AWARE] attach requested")
        } catch (e: Throwable) {
            attaching.set(false)
            if (!ready.isCompleted) ready.completeExceptionally(e)
            LogCat.e("[AWARE] start error: ${e.message}")
        }
    }

    @Synchronized
    fun stop() {
        runCatching { subscribe?.close() }
        subscribe = null
        runCatching { publish?.close() }
        publish = null
        runCatching { session?.close() }
        session = null
        attaching.set(false)
        TempData.awareRunning.value = false
        clearPeerHandles()
        if (!ready.isCompleted) {
            ready.cancel()
        }
        ready = newReady()
    }

    fun markStale() {
        stop()
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE])
    fun isAvailable(): Boolean {
        return try {
            wifiAwareManager.isAvailable
        } catch (_: Throwable) {
            false
        }
    }

    private fun clearPeerHandles() {
        discoveredPeers.clear()
        discoveredAt.clear()
        peerHandleWaiters.values.forEach { runCatching { it.cancel() } }
        peerHandleWaiters.clear()
        publishPeerHandles.clear()
        publishPeerHandleWaiters.values.forEach { runCatching { it.cancel() } }
        publishPeerHandleWaiters.clear()
        readyPeers.clear()
        peerReadyWaiters.values.forEach { runCatching { it.cancel() } }
        peerReadyWaiters.clear()
    }

    private fun newReady(): CompletableDeferred<WifiAwareSession> = CompletableDeferred()

    private val attachCallback = object : AttachCallback() {
        override fun onAttached(s: WifiAwareSession) {
            if (session != null) {
                runCatching { s.close() }
                return
            }
            LogCat.d("[AWARE] attach onAttached")
            session = s
            attaching.set(false)
            TempData.awareRunning.value = true
            if (!ready.isCompleted) ready.complete(s)
            publishOwnService(s)
            subscribeOwnService(s)
        }

        override fun onAttachFailed() {
            LogCat.e("[AWARE] attach onAttachFailed")
            attaching.set(false)
            if (!ready.isCompleted) ready.completeExceptionally(IllegalStateException("attach failed"))
        }

        override fun onAwareSessionTerminated() {
            LogCat.e("[AWARE] attach onAwareSessionTerminated")
            subscribe = null
            publish = null
            session = null
            TempData.awareRunning.value = false
            clearPeerHandles()
            if (!ready.isCompleted) {
                ready.completeExceptionally(IllegalStateException("session terminated"))
            }
        }
    }

    private fun publishOwnService(s: WifiAwareSession) {
        try {
            s.publish(
                PublishConfig.Builder()
                    .setServiceName(SERVICE_NAME)
                    .setServiceSpecificInfo(TempData.clientId.toByteArray(Charsets.UTF_8))
                    .build(),
                publishCallback,
                null,
            )
            LogCat.d("[AWARE] publish requested cid=${TempData.clientId}")
        } catch (e: Throwable) {
            LogCat.e("[AWARE] publish error: ${e.message}")
        }
    }

    private fun subscribeOwnService(s: WifiAwareSession) {
        try {
            s.subscribe(
                SubscribeConfig.Builder().setServiceName(SERVICE_NAME).build(),
                subscribeCallback,
                null,
            )
            LogCat.d("[AWARE] subscribe requested")
        } catch (e: Throwable) {
            LogCat.e("[AWARE] subscribe error: ${e.message}")
        }
    }

    private val publishCallback = object : DiscoverySessionCallback() {
        override fun onPublishStarted(s: PublishDiscoverySession) {
            publish = s
            LogCat.d("[AWARE] publish started")
        }

        override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
            val fromCid = message.toString(Charsets.UTF_8)
            LogCat.d("[AWARE] publish onMessageReceived(hello) from=$fromCid")
            publishPeerHandles[fromCid] = peerHandle
            publishPeerHandleWaiters.remove(fromCid)?.complete(peerHandle)
            // 通知 AwareLinkPool：subscriber 发来 hello，说明它要建立连接。
            // 如果本端（publisher）还没在 build，触发 buildLink 作为接收方自动建链。
            publishHelloListeners.forEach { runCatching { it(fromCid, peerHandle) } }
        }

        override fun onSessionTerminated() {
            LogCat.e("[AWARE] publish onSessionTerminated")
            publish = null
            session?.let { runCatching { publishOwnService(it) } }
        }
    }

    private val subscribeCallback = object : DiscoverySessionCallback() {
        override fun onSubscribeStarted(s: SubscribeDiscoverySession) {
            subscribe = s
            LogCat.d("[AWARE] subscribe started")
        }

        override fun onServiceDiscovered(
            peerHandle: PeerHandle,
            serviceSpecificInfo: ByteArray?,
            matchFilter: MutableList<ByteArray>,
        ) {
            val fromCid = serviceSpecificInfo?.toString(Charsets.UTF_8) ?: return
            LogCat.d("[AWARE] onServiceDiscovered from=$fromCid")
            discoveredPeers[fromCid] = peerHandle
            discoveredAt[fromCid] = System.currentTimeMillis()
            peerHandleWaiters.remove(fromCid)?.complete(peerHandle)
            // 发 hello 给 publisher，触发 publisher 端 buildLink。
            // hello 是幂等的（publisher 端只覆写 handle），且 publisher 可能刚重启 publish session，
            // 旧 handle 已失效需要新的。
            runCatching {
                subscribe?.sendMessage(
                    peerHandle,
                    MSG_HELLO,
                    TempData.clientId.toByteArray(Charsets.UTF_8),
                )
                LogCat.d("[AWARE] hello sent to=$fromCid")
            }
            serviceDiscoveredListeners.forEach { runCatching { it(fromCid, peerHandle) } }
        }

        // publisher 发来的 ready 回执（官方步骤 5）：publisher 已 requestNetwork，
        // 通知 subscriber 可以发起 requestNetwork 了（步骤 6）。
        override fun onMessageReceived(peerHandle: PeerHandle, message: ByteArray) {
            val fromCid = message.toString(Charsets.UTF_8)
            LogCat.d("[AWARE] subscribe onMessageReceived(ready) from=$fromCid")
            markPeerReady(fromCid)
            // 通知 AwareLinkPool：publisher 已准备好，如果本端（subscriber）还没在 build，
            // 触发 buildLink 作为接收方自动建链。
            subscribeReadyListeners.forEach { runCatching { it(fromCid) } }
        }

        override fun onSessionTerminated() {
            LogCat.e("[AWARE] subscribe onSessionTerminated")
            subscribe = null
            clearPeerHandles()
            session?.let { runCatching { subscribeOwnService(it) } }
        }
    }

    companion object {
        const val SERVICE_NAME = "plain-peer"
        private const val PEER_HANDLE_MAX_AGE_MS = 30_000L
        // hello 消息（subscriber→publisher）和 ready 回执（publisher→subscriber）的 messageId
        const val MSG_HELLO = 0
        const val MSG_READY = 1
        // subscriber 等 publisher ready 回执的超时，超时说明 publisher 没 requestNetwork
        private const val READY_TIMEOUT_MS = 15_000L
    }
}