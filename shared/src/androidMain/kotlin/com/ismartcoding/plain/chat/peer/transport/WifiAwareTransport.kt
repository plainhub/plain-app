package com.ismartcoding.plain.chat.peer.transport

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.ismartcoding.plain.chat.peer.GraphQLResponse
import com.ismartcoding.plain.chat.peer.transport.aware.AwareHttpClientFactory
import com.ismartcoding.plain.chat.peer.transport.aware.AwareLinkPool
import com.ismartcoding.plain.chat.peer.transport.aware.AwareSession
import com.ismartcoding.plain.connectivityManager
import com.ismartcoding.plain.db.DPeer
import com.ismartcoding.plain.db.getAwareFileUrl
import com.ismartcoding.plain.lib.isSPlus
import com.ismartcoding.plain.lib.isTPlus
import com.ismartcoding.plain.lib.logcat.LogCat

@RequiresApi(Build.VERSION_CODES.S)
object WifiAwareTransport : PeerTransport {
    override val id: String = "aware"

    private val session = AwareSession()
    private val httpFactory = AwareHttpClientFactory()
    private val pool = AwareLinkPool(session, connectivityManager, httpFactory)

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

    fun subscribe(peer: DPeer) {
        pool.subscribe(peer)
    }

    fun unsubscribe(peerId: String) {
        pool.unsubscribe(peerId)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.CHANGE_NETWORK_STATE])
    override suspend fun downloadFile(peer: DPeer, fileId: String): DownloadedResponse {
        val connection = pool.buildLink(peer)
        val client = httpFactory.buildFileDownload(connection.network, connection.peerIpv6)
        val url = peer.getAwareFileUrl(fileId, connection.peerPort)
        val response = executeDownloadRequest(id, peer.id, client, url)
        if (!response.isSuccessful) {
            response.close()
            LogCat.e("Aware transport file download failed: ${response.code}")
            throw TransportUnavailable(id, peer.id, null)
        }
        return DownloadedResponse(client, response)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.CHANGE_NETWORK_STATE])
    override suspend fun send(peer: DPeer, request: SignedRequest, keyBytes: ByteArray): GraphQLResponse {
        LogCat.d("[AWARE] send start peer=${peer.id} cid=${request.channelId}")
        val connection = pool.buildLink(peer)
        val url = "https://${AwareHttpClientFactory.AWARE_HOST}:${connection.peerPort}/peer_graphql"
        LogCat.d("[AWARE] send http peer=${peer.id} url=$url")
        val resp = executeGraphQLRequest(
            transportId = id,
            peerId = peer.id,
            client = connection.httpClient,
            url = url,
            body = request.body,
            channelId = request.channelId,
        )
        LogCat.d("[AWARE] send done peer=${peer.id} hasException=${resp.exception != null} hasErrors=${resp.errors != null}")
        return resp
    }
}