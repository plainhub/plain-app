package com.ismartcoding.plain.platform.network.aware

import android.Manifest
import android.net.wifi.aware.PublishDiscoverySession
import android.net.wifi.aware.SubscribeDiscoverySession
import android.net.wifi.aware.WifiAwareSession
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.ismartcoding.plain.platform.network.AwareConfig
import com.ismartcoding.plain.platform.network.AwarePeer
import com.ismartcoding.plain.connectivityManager
import com.ismartcoding.plain.platform.isTPlus
import com.ismartcoding.plain.platform.network.PeerByteSocket

@RequiresApi(Build.VERSION_CODES.S)
object WifiAwareTransport {

    private val session = AwareSession()
    private val pool = AwareLinkPool(session, connectivityManager)

    val attached get() = session.attached

    // Exposed for the Wi-Fi Aware debug page so it can read the live
    // attach/publish/subscribe session state without leaking the whole
    // AwareSession implementation.
    val awareSession: WifiAwareSession? get() = session.session
    val publishSession: PublishDiscoverySession? get() = session.publish
    val subscribeSession: SubscribeDiscoverySession? get() = session.subscribe
    val discoveredPeerCount: Int get() = session.discoveredPeerCount

    // aware only starts from android 13+
    @RequiresPermission(allOf = [Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_WIFI_STATE])
    fun isSupported(): Boolean = isTPlus() && session.isAvailable()

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.NEARBY_WIFI_DEVICES])
    fun start() {
        session.start()
        pool.start()
    }

    fun stop() {
        pool.stop()
        session.stop()
    }

    fun shutdown() {
        pool.shutdown()
        session.stop()
    }

    fun subscribe(peer: AwarePeer, config: AwareConfig) {
        pool.subscribe(peer, config)
    }

    fun unsubscribe(peerId: String) {
        pool.unsubscribe(peerId)
        if (!pool.hasSubscriptions) stop()
    }

    suspend fun openSocket(peerId: String): PeerByteSocket {
        val connection = pool.buildLink(peerId)
        return AwareByteSocket.open(connection)
    }
}
